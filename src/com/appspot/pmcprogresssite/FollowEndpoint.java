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
public class FollowEndpoint {

	/**
	 * This method lists all the entities inserted in datastore.
	 * It uses HTTP GET method and paging support.
	 *
	 * @return A CollectionResponse class containing the list of all entities
	 * persisted and a cursor to the next page.
	 */
	@SuppressWarnings({ "unchecked", "unused" })
	@ApiMethod(name = "listFollow")
	public CollectionResponse<Follow> listFollow(
			@Nullable @Named("cursor") String cursorString,
			@Nullable @Named("limit") Integer limit) {

		PersistenceManager mgr = null;
		Cursor cursor = null;
		List<Follow> execute = null;

		try {
			mgr = getPersistenceManager();
			Query query = mgr.newQuery(Follow.class);
			if (cursorString != null && cursorString != "") {
				cursor = Cursor.fromWebSafeString(cursorString);
				HashMap<String, Object> extensionMap = new HashMap<String, Object>();
				extensionMap.put(JDOCursorHelper.CURSOR_EXTENSION, cursor);
				query.setExtensions(extensionMap);
			}

			if (limit != null) {
				query.setRange(0, limit);
			}

			execute = (List<Follow>) query.execute();
			cursor = JDOCursorHelper.getCursor(execute);
			if (cursor != null)
				cursorString = cursor.toWebSafeString();

			// Tight loop for fetching all entities from datastore and accomodate
			// for lazy fetch.
			for (Follow obj : execute)
				;
		} finally {
			mgr.close();
		}

		return CollectionResponse.<Follow> builder().setItems(execute)
				.setNextPageToken(cursorString).build();
	}

	/**
	 * This method gets the entity having primary key id. It uses HTTP GET method.
	 *
	 * @param id the primary key of the java bean.
	 * @return The entity with primary key id.
	 */
	@ApiMethod(name = "getFollow")
	public Follow getFollow(@Named("id") Long id) {
		PersistenceManager mgr = getPersistenceManager();
		Follow follow = null;
		try {
			follow = mgr.getObjectById(Follow.class, id);
		} finally {
			mgr.close();
		}
		return follow;
	}

	/**
	 * This inserts a new entity into App Engine datastore. If the entity already
	 * exists in the datastore, an exception is thrown.
	 * It uses HTTP POST method.
	 *
	 * @param follow the entity to be inserted.
	 * @return The inserted entity.
	 */
	@SuppressWarnings("unchecked")
	@ApiMethod(name = "insertFollow")
	public Follow insertFollow(HttpServletRequest req, Follow follow) throws BadRequestException, IOException {
		HttpSession session = req.getSession(false);
		// get user id from session
		Long uid = (Long) session.getAttribute("id");
		
		if (uid != null ){
		PersistenceManager mgr = getPersistenceManager();
		try {
            if (follow.getId() != null) {
                if (containsFollow(follow)) {
                    throw new EntityExistsException("Object already exists");
                }
            }
			
			//see if like already exists

            Long eid = follow.targetid;
            Query q = mgr.newQuery(Follow.class);
            q.setFilter("ownerid == " + uid + " && targetid == " + eid);
            Map <String, Long> paramValues = new HashMap <String, Long> ();
            paramValues.put("uid", uid);
            paramValues.put("eid", eid);	
            
            List<Follow> results = (List<Follow>) q.executeWithMap(paramValues);
            //see if like exists
            if (!results.isEmpty()) {
            	Long objectid = results.get(0).getId();
                    //delete like
            	follow = mgr.getObjectById(Follow.class, objectid);
            	
                //make feed
                Feed feed = new Feed();
                feed.ownerid = follow.ownerid;
                feed.ownername = follow.ownername;
                feed.ownerthumb = follow.ownerthumb;
                feed.entitytype = "feed";
                feed.target = follow.entitytype;
                feed.targetid = follow.id;
                feed.targetowner = follow.targetowner;
                feed.targetaction = "removed";
                feed.created = new Date();
                //save feed
                mgr.makePersistent(feed);  
                
                //delete follow
                    mgr.deletePersistent(follow);  
                    
        			//decrement count
            		switch (follow.target) {
            		case "project":
            			//subtract from projects count
            			Project project = mgr.getObjectById(Project.class, follow.targetid);
            			int projCount = project.followcount;
            			project.setfollowcount(projCount - 1);
                        //save project
                        mgr.makePersistent(project);
            			break;
            			
            		case "step":
            			//subtract from steps count    
            			Step step = mgr.getObjectById(Step.class, follow.targetid);
            			int stepCount = step.followcount;
            			step.setfollowcount(stepCount - 1);
                        //save step
                        mgr.makePersistent(step);
            			break;
            			
            		case "user":
            			//subtract from users count   
            			User user = mgr.getObjectById(User.class, follow.targetid);
            			int userCount = user.followcount;
            			user.setfollowcount(userCount - 1);
                        //save user
                        mgr.makePersistent(user);
            			break;

            		case "comment":
            			//subtract from comments count    
            			Comment comment = mgr.getObjectById(Comment.class, follow.targetid);
            			int commentCount = comment.followcount;
            			comment.setfollowcount(commentCount - 1);
                        //save comment
                        mgr.makePersistent(comment);
            			break;
            			
            		default:
            			//no case found    			
            			break;
            		}
       
        } else {
        	follow.created = new Date();
            //create like            
            mgr.makePersistent(follow);  
            
            //make feed
            Feed feed = new Feed();
            feed.ownerid = follow.ownerid;
            feed.ownername = follow.ownername;
            feed.ownerthumb = follow.ownerthumb;
            feed.entitytype = "feed";
            feed.target = follow.entitytype;
            feed.targetid = follow.id;
            feed.targetowner = follow.targetowner;
            feed.targetaction = "added";
            feed.created = new Date();         
            //save feed
            mgr.makePersistent(feed);   
            
			//TODO increment count
        }
                    
		} finally {
			mgr.close();
		}
		return follow;
		
		} else {
			
			throw new BadRequestException("Your Not Logged In...");
		}
	}

	/**
	 * This method is used for updating an existing entity. If the entity does not
	 * exist in the datastore, an exception is thrown.
	 * It uses HTTP PUT method.
	 *
	 * @param follow the entity to be updated.
	 * @return The updated entity.
	 */
	@ApiMethod(name = "updateFollow")
	public Follow updateFollow(Follow follow) {
		PersistenceManager mgr = getPersistenceManager();
		try {
			if (!containsFollow(follow)) {
				throw new EntityNotFoundException("Object does not exist");
			}
			mgr.makePersistent(follow);
		} finally {
			mgr.close();
		}
		return follow;
	}

	/**
	 * This method removes the entity with primary key id.
	 * It uses HTTP DELETE method.
	 *
	 * @param id the primary key of the entity to be deleted.
	 */
	@ApiMethod(name = "removeFollow")
	public void removeFollow(@Named("id") Long id) {
		PersistenceManager mgr = getPersistenceManager();
		try {
			Follow follow = mgr.getObjectById(Follow.class, id);
			mgr.deletePersistent(follow);
		} finally {
			mgr.close();
		}
	}
	
	/**
	 * This gets the follows the current user is target of 
	 * It uses HTTP GET method.
	 *
	 * @param step the entity to be inserted.
	 * @return The inserted entity.
	 */
    @SuppressWarnings({"unchecked", "unused"})
    @ApiMethod(name = "listuserFollower", path = "listuserFollower/{id}")
    public CollectionResponse <Follow> listuserFollower(@Nullable @Named("cursor") String cursorString, @Nullable @Named("limit") Integer limit, @Named("id") Long id) {

        PersistenceManager mgr = null;
        Cursor cursor = null;
        List <Follow> execute = null;

        try {
            mgr = getPersistenceManager();

            Query query = mgr.newQuery(Follow.class);
            query.setFilter("targetid == owneridParam");
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

            execute = (List <Follow> ) query.execute(uid);
            cursor = JDOCursorHelper.getCursor(execute);
            if (cursor != null)
                cursorString = cursor.toWebSafeString();

            // Tight loop for fetching all entities from datastore and accomodate
            // for lazy fetch.
            for (Follow obj: execute)
            ;
        } finally {
            mgr.close();
        }

        return CollectionResponse. <Follow> builder().setItems(execute)
            .setNextPageToken(cursorString).build();
    }

	/**
	 * This gets the follows the current user owns
	 * It uses HTTP GET method.
	 *
	 * @param step the entity to be inserted.
	 * @return The inserted entity.
	 */
    @SuppressWarnings({"unchecked", "unused"})
    @ApiMethod(name = "listuserFollowing", path = "listuserFollowing/{id}")
    public CollectionResponse <Follow> listuserFollowing(@Nullable @Named("cursor") String cursorString, @Nullable @Named("limit") Integer limit, @Named("id") Long id) {

        PersistenceManager mgr = null;
        Cursor cursor = null;
        List <Follow> execute = null;

        try {
            mgr = getPersistenceManager();

            Query query = mgr.newQuery(Follow.class);
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

            execute = (List <Follow> ) query.execute(uid);
            cursor = JDOCursorHelper.getCursor(execute);
            if (cursor != null)
                cursorString = cursor.toWebSafeString();

            // Tight loop for fetching all entities from datastore and accomodate
            // for lazy fetch.
            for (Follow obj: execute)
            ;
        } finally {
            mgr.close();
        }

        return CollectionResponse. <Follow> builder().setItems(execute)
            .setNextPageToken(cursorString).build();
    }
    
    
	private boolean containsFollow(Follow follow) {
		PersistenceManager mgr = getPersistenceManager();
		boolean contains = true;
		try {
			mgr.getObjectById(Follow.class, follow.getId());
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
