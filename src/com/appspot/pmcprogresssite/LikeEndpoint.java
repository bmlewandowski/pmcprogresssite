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
public class LikeEndpoint {

	/**
	 * This method lists all the entities inserted in datastore.
	 * It uses HTTP GET method and paging support.
	 *
	 * @return A CollectionResponse class containing the list of all entities
	 * persisted and a cursor to the next page.
	 */
	@SuppressWarnings({ "unchecked", "unused" })
	@ApiMethod(name = "listLike")
	public CollectionResponse<Like> listLike(
			@Nullable @Named("cursor") String cursorString,
			@Nullable @Named("limit") Integer limit) {

		PersistenceManager mgr = null;
		Cursor cursor = null;
		List<Like> execute = null;

		try {
			mgr = getPersistenceManager();
			Query query = mgr.newQuery(Like.class);
			if (cursorString != null && cursorString != "") {
				cursor = Cursor.fromWebSafeString(cursorString);
				HashMap<String, Object> extensionMap = new HashMap<String, Object>();
				extensionMap.put(JDOCursorHelper.CURSOR_EXTENSION, cursor);
				query.setExtensions(extensionMap);
			}

			if (limit != null) {
				query.setRange(0, limit);
			}

			execute = (List<Like>) query.execute();
			cursor = JDOCursorHelper.getCursor(execute);
			if (cursor != null)
				cursorString = cursor.toWebSafeString();

			// Tight loop for fetching all entities from datastore and accomodate
			// for lazy fetch.
			for (Like obj : execute)
				;
		} finally {
			mgr.close();
		}

		return CollectionResponse.<Like> builder().setItems(execute)
				.setNextPageToken(cursorString).build();
	}

	/**
	 * This method gets the entity having primary key id. It uses HTTP GET method.
	 *
	 * @param id the primary key of the java bean.
	 * @return The entity with primary key id.
	 */
	@ApiMethod(name = "getLike")
	public Like getLike(@Named("id") Long id) {
		PersistenceManager mgr = getPersistenceManager();
		Like like = null;
		try {
			like = mgr.getObjectById(Like.class, id);
		} finally {
			mgr.close();
		}
		return like;
	}

	/**
	 * This inserts a new entity into App Engine datastore. If the entity already
	 * exists in the datastore, an exception is thrown.
	 * It uses HTTP POST method.
	 *
	 * @param like the entity to be inserted.
	 * @return The inserted entity.
	 */
	@SuppressWarnings("unchecked")
	@ApiMethod(name = "insertLike")
	public Like insertLike(HttpServletRequest req, Like like) throws BadRequestException, IOException {
		HttpSession session = req.getSession(false);
		// get user id from session
		Long uid = (Long) session.getAttribute("id");
		
		if (uid != null ){
		PersistenceManager mgr = getPersistenceManager();
		try {
            if (like.getId() != null) {
                if (containsLike(like)) {
                    throw new EntityExistsException("Object already exists");
                }
            }
			
			//see if like already exists

            Long eid = like.targetid;
            Query q = mgr.newQuery(Like.class);
            q.setFilter("ownerid == " + uid + " && targetid == " + eid);
            Map <String, Long> paramValues = new HashMap <String, Long> ();
            paramValues.put("uid", uid);
            paramValues.put("eid", eid);	
            
            List<Like> results = (List<Like>) q.executeWithMap(paramValues);
            //see if like exists
            if (!results.isEmpty()) {
            	Long objectid = results.get(0).getId();
                    //delete like
            		like = mgr.getObjectById(Like.class, objectid);

                    //make feed
                    Feed feed = new Feed();
                    feed.ownerid = like.ownerid;
                    feed.ownername = like.ownername;
                    feed.ownerthumb = like.ownerthumb;
                    feed.entitytype = "feed";
                    feed.target = like.entitytype;
                    feed.targetid = like.id;
                    feed.targetowner = like.targetowner;
                    feed.targetaction = "removed";
                    feed.created = new Date();
                    //save feed
                    mgr.makePersistent(feed);  
                    
            		//delete like
                    mgr.deletePersistent(like); 
                    
        			//decrement count
            		switch (like.target) {
            		case "project":
            			//subtract from projects count
            			Project project = mgr.getObjectById(Project.class, like.targetid);
            			int projCount = project.likecount;
            			project.setlikecount(projCount - 1);
                        //save project
                        mgr.makePersistent(project);
            			break;
            			
            		case "step":
            			//subtract from steps count    
            			Step step = mgr.getObjectById(Step.class, like.targetid);
            			int stepCount = step.likecount;
            			step.setlikecount(stepCount - 1);
                        //save step
                        mgr.makePersistent(step);
            			break;
            			
            		case "user":
            			//subtract from users count   
            			User user = mgr.getObjectById(User.class, like.targetid);
            			int userCount = user.likecount;
            			user.setlikecount(userCount - 1);
                        //save user
                        mgr.makePersistent(user);
            			break;

            		case "comment":
            			//subtract from comments count    
            			Comment comment = mgr.getObjectById(Comment.class, like.targetid);
            			int commentCount = comment.likecount;
            			comment.setlikecount(commentCount - 1);
                        //save comment
                        mgr.makePersistent(comment);
            			break;
            			
            		default:
            			//no case found    			
            			break;
            		}
       
        } else {
        	like.created = new Date();
            //save like            
            mgr.makePersistent(like);   
            
            //make feed
            Feed feed = new Feed();
            feed.ownerid = like.ownerid;
            feed.ownername = like.ownername;
            feed.ownerthumb = like.ownerthumb;
            feed.entitytype = "feed";
            feed.target = like.entitytype;
            feed.targetid = like.id;
            feed.targetowner = like.targetowner;
            feed.targetaction = "added";
            feed.created = new Date();
            //save feed
            mgr.makePersistent(feed);   
            
			//increment count             
    		switch (like.target) {
    		case "project":
    			//add to projects count
    			Project project = mgr.getObjectById(Project.class, like.targetid);
    			int projCount = project.likecount;
    			project.setlikecount(projCount + 1);
                //save project
                mgr.makePersistent(project);
    			break;
    			
    		case "step":
    			//add to steps count    
    			Step step = mgr.getObjectById(Step.class, like.targetid);
    			int stepCount = step.likecount;
    			step.setlikecount(stepCount + 1);
                //save project
                mgr.makePersistent(step);
    			break;
    			
    		case "user":
    			//add to users count   
    			User user = mgr.getObjectById(User.class, like.targetid);
    			int userCount = user.likecount;
    			user.setlikecount(userCount + 1);
                //save project
                mgr.makePersistent(user);
    			break;

    		case "comment":
    			//add to comments count    
    			Comment comment = mgr.getObjectById(Comment.class, like.targetid);
    			int commentCount = comment.likecount;
    			comment.setlikecount(commentCount + 1);
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
		return like;
		
		} else {
			
			throw new BadRequestException("Your Not Logged In...");
		}
	}

	/**
	 * This method is used for updating an existing entity. If the entity does not
	 * exist in the datastore, an exception is thrown.
	 * It uses HTTP PUT method.
	 *
	 * @param like the entity to be updated.
	 * @return The updated entity.
	 */
	@ApiMethod(name = "updateLike")
	public Like updateLike(Like like) {
		PersistenceManager mgr = getPersistenceManager();
		try {
			if (!containsLike(like)) {
				throw new EntityNotFoundException("Object does not exist");
			}
			mgr.makePersistent(like);
		} finally {
			mgr.close();
		}
		return like;
	}

	/**
	 * This method removes the entity with primary key id.
	 * It uses HTTP DELETE method.
	 *
	 * @param id the primary key of the entity to be deleted.
	 */
	@ApiMethod(name = "removeLike")
	public void removeLike(@Named("id") Long id) {
		PersistenceManager mgr = getPersistenceManager();
		try {
			Like like = mgr.getObjectById(Like.class, id);
			mgr.deletePersistent(like);
		} finally {
			mgr.close();
		}
	}

	/**
	 * This gets the likes the current user owns
	 * It uses HTTP GET method.
	 *
	 * @param step the entity to be inserted.
	 * @return The inserted entity.
	 */
    @SuppressWarnings({"unchecked", "unused"})
    @ApiMethod(name = "listuserLike", path = "listuserLike/{id}")
    public CollectionResponse <Like> listuserLike(@Nullable @Named("cursor") String cursorString, @Nullable @Named("limit") Integer limit, @Named("id") Long id) {

        PersistenceManager mgr = null;
        Cursor cursor = null;
        List <Like> execute = null;

        try {
            mgr = getPersistenceManager();

            Query query = mgr.newQuery(Like.class);
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

            execute = (List <Like> ) query.execute(uid);
            cursor = JDOCursorHelper.getCursor(execute);
            if (cursor != null)
                cursorString = cursor.toWebSafeString();

            // Tight loop for fetching all entities from datastore and accomodate
            // for lazy fetch.
            for (Like obj: execute)
            ;
        } finally {
            mgr.close();
        }

        return CollectionResponse. <Like> builder().setItems(execute)
            .setNextPageToken(cursorString).build();
    }
    
    
	private boolean containsLike(Like like) {
		PersistenceManager mgr = getPersistenceManager();
		boolean contains = true;
		try {
			mgr.getObjectById(Like.class, like.getId());
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
