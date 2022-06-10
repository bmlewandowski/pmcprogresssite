package com.appspot.pmcprogresssite;

import com.appspot.pmcprogresssite.PMF;
import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiAuth;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.datanucleus.query.JDOCursorHelper;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Date;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;


@Api(auth = @ApiAuth (allowCookieAuth = AnnotationBoolean.TRUE), name = "progressendpoint", namespace = @ApiNamespace(ownerDomain = "appspot.com", ownerName = "appspot.com", packagePath = "pmcprogresssite"))
public class FlagEndpoint {
	
	/**
	 * This method lists all the entities inserted in datastore.
	 * It uses HTTP GET method and paging support.
	 *
	 * @return A CollectionResponse class containing the list of all entities
	 * persisted and a cursor to the next page.
	 */
	@SuppressWarnings({ "unchecked", "unused" })
	@ApiMethod(name = "listFlag")
	public CollectionResponse<Flag> listFlag(
			@Nullable @Named("cursor") String cursorString,
			@Nullable @Named("limit") Integer limit) {

		PersistenceManager mgr = null;
		Cursor cursor = null;
		List<Flag> execute = null;

		try {
			mgr = getPersistenceManager();
			Query query = mgr.newQuery(Flag.class);
			if (cursorString != null && cursorString != "") {
				cursor = Cursor.fromWebSafeString(cursorString);
				HashMap<String, Object> extensionMap = new HashMap<String, Object>();
				extensionMap.put(JDOCursorHelper.CURSOR_EXTENSION, cursor);
				query.setExtensions(extensionMap);
			}

			if (limit != null) {
				query.setRange(0, limit);
			}

			execute = (List<Flag>) query.execute();
			cursor = JDOCursorHelper.getCursor(execute);
			if (cursor != null)
				cursorString = cursor.toWebSafeString();

			// Tight loop for fetching all entities from datastore and accomodate
			// for lazy fetch.
			for (Flag obj : execute)
				;
		} finally {
			mgr.close();
		}

		return CollectionResponse.<Flag> builder().setItems(execute)
				.setNextPageToken(cursorString).build();
	}

	/**
	 * This method gets the entity having primary key id. It uses HTTP GET method.
	 *
	 * @param id the primary key of the java bean.
	 * @return The entity with primary key id.
	 */
	@ApiMethod(name = "getFlag")
	public Flag getFlag(@Named("id") Long id) {
		PersistenceManager mgr = getPersistenceManager();
		Flag flag = null;
		try {
			flag = mgr.getObjectById(Flag.class, id);
		} finally {
			mgr.close();
		}
		return flag;
	}

	/**
	 * This inserts a new entity into App Engine datastore. If the entity already
	 * exists in the datastore, an exception is thrown.
	 * It uses HTTP POST method.
	 *
	 * @param flag the entity to be inserted.
	 * @return The inserted entity.
	 */
	@SuppressWarnings("unchecked")
	@ApiMethod(name = "insertFlag")
	public Flag insertFlag(HttpServletRequest req, Flag flag) throws BadRequestException, IOException {
		HttpSession session = req.getSession(false);
		// get user id from session
		Long uid = (Long) session.getAttribute("id");
		
		if (uid != null ){
		PersistenceManager mgr = getPersistenceManager();
		try {
            if (flag.getId() != null) {
                if (containsFlag(flag)) {
                    throw new EntityExistsException("Object already exists");
                }
            }
			
			//see if like already exists

            Long eid = flag.targetid;
            Query q = mgr.newQuery(Flag.class);
            q.setFilter("ownerid == " + uid + " && targetid == " + eid);
            Map <String, Long> paramValues = new HashMap <String, Long> ();
            paramValues.put("uid", uid);
            paramValues.put("eid", eid);	
            
            List<Flag> results = (List<Flag>) q.executeWithMap(paramValues);
            //see if like exists
            if (!results.isEmpty()) {
            	Long objectid = results.get(0).getId();
                    //delete like
            	flag = mgr.getObjectById(Flag.class, objectid);
            	
                //make feed
                Feed feed = new Feed();
                feed.ownerid = flag.ownerid;
                feed.ownername = flag.ownername;
                feed.ownerthumb = flag.ownerthumb;
                feed.entitytype = "feed";
                feed.target = flag.entitytype;
                feed.targetid = flag.id;
                feed.targetowner = flag.targetowner;
                feed.targetaction = "removed";
                feed.created = new Date();
                //save feed
                mgr.makePersistent(feed);  
                
        		//delete flag
                    mgr.deletePersistent(flag);  
                    
        			//decrement count
            		switch (flag.target) {
            		case "project":
            			//subtract from projects count
            			Project project = mgr.getObjectById(Project.class, flag.targetid);
            			int projCount = project.flagcount;
            			project.setflagcount(projCount - 1);
                        //save project
                        mgr.makePersistent(project);
            			break;
            			
            		case "step":
            			//subtract from steps count    
            			Step step = mgr.getObjectById(Step.class, flag.targetid);
            			int stepCount = step.flagcount;
            			step.setflagcount(stepCount - 1);
                        //save step
                        mgr.makePersistent(step);
            			break;
            			
            		case "user":
            			//subtract from users count   
            			User user = mgr.getObjectById(User.class, flag.targetid);
            			int userCount = user.flagcount;
            			user.setflagcount(userCount - 1);
                        //save user
                        mgr.makePersistent(user);
            			break;

            		case "comment":
            			//subtract from comments count    
            			Comment comment = mgr.getObjectById(Comment.class, flag.targetid);
            			int commentCount = comment.flagcount;
            			comment.setflagcount(commentCount - 1);
                        //save comment
                        mgr.makePersistent(comment);
            			break;
            			
            		default:
            			//no case found    			
            			break;
            		}
       
        } else {
        	flag.created = new Date();
            //save like            
            mgr.makePersistent(flag);   
            
            //make feed
            Feed feed = new Feed();
            feed.ownerid = flag.ownerid;
            feed.ownername = flag.ownername;
            feed.ownerthumb = flag.ownerthumb;
            feed.entitytype = "feed";
            feed.target = flag.entitytype;
            feed.targetid = flag.id;
            feed.targetowner = flag.targetowner;
            feed.targetaction = "added";
            feed.created = new Date();
            //save feed
            mgr.makePersistent(feed);  
            
			//increment count             
    		switch (flag.target) {
    		case "project":
    			//add to projects count
    			Project project = mgr.getObjectById(Project.class, flag.targetid);
    			int projCount = project.flagcount;
    			project.setflagcount(projCount + 1);
                //save project
                mgr.makePersistent(project);
    			break;
    			
    		case "step":
    			//add to steps count    
    			Step step = mgr.getObjectById(Step.class, flag.targetid);
    			int stepCount = step.flagcount;
    			step.setflagcount(stepCount + 1);
                //save project
                mgr.makePersistent(step);
    			break;
    			
    		case "user":
    			//add to users count   
    			User user = mgr.getObjectById(User.class, flag.targetid);
    			int userCount = user.flagcount;
    			user.setflagcount(userCount + 1);
                //save project
                mgr.makePersistent(user);
    			break;

    		case "comment":
    			//add to comments count    
    			Comment comment = mgr.getObjectById(Comment.class, flag.targetid);
    			int commentCount = comment.flagcount;
    			comment.setflagcount(commentCount + 1);
                //save project
                mgr.makePersistent(comment);
    			break;
    			
    		default:
    			//no case found    			
    			break;
    		}
        }
                    
		} finally {
			mgr.close();
		}
		return flag;
		
		} else {
			
			throw new BadRequestException("Your Not Logged In...");
		}
	}

	/**
	 * This method is used for updating an existing entity. If the entity does not
	 * exist in the datastore, an exception is thrown.
	 * It uses HTTP PUT method.
	 *
	 * @param flag the entity to be updated.
	 * @return The updated entity.
	 */
	@ApiMethod(name = "updateFlag")
	public Flag updateFlag(Flag flag) {
		PersistenceManager mgr = getPersistenceManager();
		try {
			if (!containsFlag(flag)) {
				throw new EntityNotFoundException("Object does not exist");
			}
			mgr.makePersistent(flag);
		} finally {
			mgr.close();
		}
		return flag;
	}

	/**
	 * This method removes the entity with primary key id.
	 * It uses HTTP DELETE method.
	 *
	 * @param id the primary key of the entity to be deleted.
	 */
	@ApiMethod(name = "removeFlag")
	public void removeFlag(@Named("id") Long id) {
		PersistenceManager mgr = getPersistenceManager();
		try {
			Flag flag = mgr.getObjectById(Flag.class, id);
			mgr.deletePersistent(flag);
		} finally {
			mgr.close();
		}
	}

	/**
	 * This gets the flags the current user owns
	 * It uses HTTP GET method.
	 *
	 * @param step the entity to be inserted.
	 * @return The inserted entity.
	 */
    @SuppressWarnings({"unchecked", "unused"})
    @ApiMethod(name = "listuserFlag", path = "listuserFlag/{id}")
    public CollectionResponse <Flag> listuserFlag(@Nullable @Named("cursor") String cursorString, @Nullable @Named("limit") Integer limit, @Named("id") Long id) {

        PersistenceManager mgr = null;
        Cursor cursor = null;
        List <Flag> execute = null;

        try {
            mgr = getPersistenceManager();

            Query query = mgr.newQuery(Flag.class);
            query.setFilter("ownerid == owneridParam");
            query.declareParameters("Long owneridParam");

            if (cursorString != null && cursorString != "") {
                cursor = Cursor.fromWebSafeString(cursorString);
                HashMap <String, Object> extensionMap = new HashMap <String, Object> ();
                extensionMap.put(JDOCursorHelper.CURSOR_EXTENSION, cursor);
                query.setExtensions(extensionMap);
            }

            if (limit != null) {
                query.setRange(0, limit);
            }

            Long uid = id;

            execute = (List <Flag> ) query.execute(uid);
            cursor = JDOCursorHelper.getCursor(execute);
            if (cursor != null)
                cursorString = cursor.toWebSafeString();

            // Tight loop for fetching all entities from datastore and accomodate
            // for lazy fetch.
            for (Flag obj: execute)
            ;
        } finally {
            mgr.close();
        }

        return CollectionResponse. <Flag> builder().setItems(execute)
            .setNextPageToken(cursorString).build();
    }
    
    
	private boolean containsFlag(Flag flag) {
		PersistenceManager mgr = getPersistenceManager();
		boolean contains = true;
		try {
			mgr.getObjectById(Flag.class, flag.getId());
		} catch (javax.jdo.JDOObjectNotFoundException ex) {
			contains = false;
		} finally {
			mgr.close();
		}
		return contains;
	}

	private static PersistenceManager getPersistenceManager() {
		return PMF.get().getPersistenceManager();
	}

}
