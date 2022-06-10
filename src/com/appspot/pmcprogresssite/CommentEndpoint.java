package com.appspot.pmcprogresssite;

import com.appspot.pmcprogresssite.PMF;
import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiAuth;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.CollectionResponse;
//import com.google.appengine.api.blobstore.BlobKey;
//import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
//import com.google.appengine.api.blobstore.BlobKey;
//import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.Cursor;
//import com.google.appengine.api.images.ImagesServiceFactory;
//import com.google.appengine.api.images.ServingUrlOptions;
//import com.google.appengine.api.images.ImagesServiceFactory;
//import com.google.appengine.api.images.ServingUrlOptions;
import com.google.appengine.datanucleus.query.JDOCursorHelper;
//import com.google.appengine.tools.cloudstorage.GcsFileOptions;
//import com.google.appengine.tools.cloudstorage.GcsFilename;
//import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
//import com.google.appengine.tools.cloudstorage.GcsService;
//import com.google.appengine.tools.cloudstorage.GcsOutputChannel;
//import com.google.appengine.tools.cloudstorage.RetryParams;



//import com.google.appengine.tools.cloudstorage.GcsFileOptions;
//import com.google.appengine.tools.cloudstorage.GcsFilename;
//import com.google.appengine.tools.cloudstorage.GcsServiceFactory;









import java.io.IOException;
//import java.nio.ByteBuffer;
//import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
//import java.util.UUID;
import java.util.Date;
import java.util.Map;
//import java.util.UUID;









import javax.annotation.Nullable;
import javax.inject.Named;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;

//import org.apache.commons.codec.binary.Base64;

@Api(auth = @ApiAuth (allowCookieAuth = AnnotationBoolean.TRUE), name = "progressendpoint", namespace = @ApiNamespace(ownerDomain = "appspot.com", ownerName = "appspot.com", packagePath = "pmcprogresssite"))
public class CommentEndpoint {

	/**
	 * This method lists all the entities inserted in datastore.
	 * It uses HTTP GET method and paging support.
	 *
	 * @return A CollectionResponse class containing the list of all entities
	 * persisted and a cursor to the next page.
	 */
	@SuppressWarnings({ "unchecked", "unused" })
	@ApiMethod(name = "listComment")
	public CollectionResponse<Comment> listComment(
			@Nullable @Named("cursor") String cursorString,
			@Nullable @Named("limit") Integer limit) {

		PersistenceManager mgr = null;
		Cursor cursor = null;
		List<Comment> execute = null;

		try {
			mgr = getPersistenceManager();
			Query query = mgr.newQuery(Comment.class);
			if (cursorString != null && cursorString != "") {
				cursor = Cursor.fromWebSafeString(cursorString);
				HashMap<String, Object> extensionMap = new HashMap<String, Object>();
				extensionMap.put(JDOCursorHelper.CURSOR_EXTENSION, cursor);
				query.setExtensions(extensionMap);
			}

			if (limit != null) {
				query.setRange(0, limit);
			}

			execute = (List<Comment>) query.execute();
			cursor = JDOCursorHelper.getCursor(execute);
			if (cursor != null)
				cursorString = cursor.toWebSafeString();

			// Tight loop for fetching all entities from datastore and accomodate
			// for lazy fetch.
			for (Comment obj : execute)
				;
		} finally {
			mgr.close();
		}

		return CollectionResponse.<Comment> builder().setItems(execute)
				.setNextPageToken(cursorString).build();
	}

	/**
	 * This method gets the entity having primary key id. It uses HTTP GET method.
	 *
	 * @param id the primary key of the java bean.
	 * @return The entity with primary key id.
	 */
	@ApiMethod(name = "getComment")
	public Comment getComment(@Named("id") Long id) {
		PersistenceManager mgr = getPersistenceManager();
		Comment comment = null;
		try {
			comment = mgr.getObjectById(Comment.class, id);
		} finally {
			mgr.close();
		}
		return comment;
	}

	/**
	 * This inserts a new entity into App Engine datastore. If the entity already
	 * exists in the datastore, an exception is thrown.
	 * It uses HTTP POST method.
	 *
	 * @param comment the entity to be inserted.
	 * @return The inserted entity.
	 */
	@ApiMethod(name = "insertComment")
    public Comment insertComment(HttpServletRequest req, Comment comment) throws BadRequestException, IOException {
        HttpSession session = req.getSession(false);
        // get user id from session
        Long uid = (Long) session.getAttribute("id");

        if (uid != null) {

            //set current date
            comment.created = new Date();

            PersistenceManager mgr = getPersistenceManager();
            try {
                if (comment.getId() != null) {
                    if (containsComment(comment)) {
                        throw new EntityExistsException("Object already exists");
                    }
                }
                //save comment
                mgr.makePersistent(comment);
                
                //make feed
                Feed feed = new Feed();
                feed.ownerid = comment.ownerid;
                feed.ownername = comment.ownername;
                feed.ownerthumb = comment.ownerthumb;
                feed.entitytype = "feed";
                feed.target = comment.entitytype;
                feed.targetid = comment.id;
                feed.targetowner = comment.targetowner;
                feed.targetaction = "added";
                feed.created = new Date();
                //save feed
                mgr.makePersistent(feed);  
                               
    			//increment count             
        		switch (comment.target) {
        		case "project":
        			//add to projects count
        			Project project = mgr.getObjectById(Project.class, comment.targetid);
        			int projCount = project.commentcount;
        			project.setcommentcount(projCount + 1);
                    //save project
                    mgr.makePersistent(project);
        			break;
        			
        		case "step":
        			//add to steps count    
        			Step step = mgr.getObjectById(Step.class, comment.targetid);
        			int stepCount = step.commentcount;
        			step.setcommentcount(stepCount + 1);
                    //save project
                    mgr.makePersistent(step);
        			break;
        			
        		case "user":
        			//add to users count   
        			User user = mgr.getObjectById(User.class, comment.targetid);
        			int userCount = user.commentcount;
        			user.setcommentcount(userCount + 1);
                    //save project
                    mgr.makePersistent(user);
        			break;
        			
        		default:
        			//no case found    			
        			break;
        		}
                
            } finally {
                mgr.close();
            }

            return comment;

        } else {

            throw new BadRequestException("Your Not Logged In...");

        }
    }

	/**
	 * This method is used for updating an existing entity. If the entity does not
	 * exist in the datastore, an exception is thrown.
	 * It uses HTTP PUT method.
	 *
	 * @param comment the entity to be updated.
	 * @return The updated entity.
	 */
	@ApiMethod(name = "updateComment")
    public Comment updateComment(HttpServletRequest req, Comment comment) throws BadRequestException, IOException {
    	
        HttpSession session = req.getSession(false);
        // get user id from session
       Long uid = (Long) session.getAttribute("id");
        // get commentid from request
       Long eid = comment.id;

       PersistenceManager mgr = getPersistenceManager();
        // see if logged in
       if (uid != null) {

           Query q = mgr.newQuery(Comment.class);
           q.setFilter("ownerid == " + uid + " && id == " + eid);
           Map <String, Long> paramValues = new HashMap <String, Long> ();
           paramValues.put("uid", uid);
           paramValues.put("eid", eid);

           @SuppressWarnings("unchecked")
           List <Comment> results = (List <Comment> ) q.executeWithMap(paramValues);
           // see if owner
           if (!results.isEmpty()) {

               try {
                   if (!containsComment(comment)) {
                       throw new EntityNotFoundException("Object does not exist");
                   }
                   		//save comment
                       mgr.makePersistent(comment);
                       
                       //make feed
                       Feed feed = new Feed();
                       feed.ownerid = comment.ownerid;
                       feed.ownername = comment.ownername;
                       feed.ownerthumb = comment.ownerthumb;
                       feed.entitytype = "feed";
                       feed.target = comment.entitytype;
                       feed.targetid = comment.id;
                       feed.targetowner = comment.targetowner;
                       feed.targetaction = "edited";
                       feed.created = new Date();
                       //save feed
                       mgr.makePersistent(feed);  
                       

               } finally {
                   mgr.close();
               }
               return comment;


           } else {

               throw new BadRequestException("Your Not The Owner");
           }

       } else {

           throw new BadRequestException("Your Not Logged In...");
       }
       }

	/**
	 * This method removes the entity with primary key id.
	 * It uses HTTP DELETE method.
	 *
	 * @param id the primary key of the entity to be deleted.
	 */
	@ApiMethod(name = "removeComment")
	public void removeComment(HttpServletRequest req, Comment comment) throws BadRequestException, IOException {

		PersistenceManager mgr = getPersistenceManager();	
		
        HttpSession session = req.getSession(false);
    // get user id from session
       Long uid = (Long) session.getAttribute("id");
        // get commentid from request
       Long eid = comment.id;
        // see if logged in
       if (uid != null) {

           Query q = mgr.newQuery(Comment.class);
           q.setFilter("ownerid == " + uid + " && id == " + eid);
           Map <String, Long> paramValues = new HashMap <String, Long> ();
           paramValues.put("uid", uid);
           paramValues.put("eid", eid);

           @SuppressWarnings("unchecked")
           List <Comment> results = (List <Comment> ) q.executeWithMap(paramValues);
           // see if owner
           if (!results.isEmpty()) {

               try {
                   if (!containsComment(comment)) {
                       throw new EntityNotFoundException("Object does not exist");
                   }			
                
                   //make feed
                   Feed feed = new Feed();
                   feed.ownerid = comment.ownerid;
                   feed.ownername = comment.ownername;
                   feed.ownerthumb = comment.ownerthumb;
                   feed.entitytype = "feed";
                   feed.target = comment.entitytype;
                   feed.targetid = comment.id;
                   feed.targetowner = comment.targetowner;
                   feed.targetaction = "removed";
                   feed.created = new Date();
                   //delete comment
                       mgr.deletePersistent(comment);
                       
           			//decrement count
               		switch (comment.target) {
               		case "project":
               			//subtract from projects count
               			Project project = mgr.getObjectById(Project.class, comment.targetid);
               			int projCount = project.commentcount;
               			project.setcommentcount(projCount - 1);
                           //save project
                           mgr.makePersistent(project);
               			break;
               			
               		case "step":
               			//subtract from steps count    
               			Step step = mgr.getObjectById(Step.class, comment.targetid);
               			int stepCount = step.commentcount;
               			step.setcommentcount(stepCount - 1);
                           //save step
                           mgr.makePersistent(step);
               			break;
               			
               		case "user":
               			//subtract from users count   
               			User user = mgr.getObjectById(User.class, comment.targetid);
               			int userCount = user.commentcount;
               			user.setcommentcount(userCount - 1);
                           //save user
                           mgr.makePersistent(user);
               			break;
               			
               		default:
               			//no case found    			
               			break;
               		}                       
          
               } finally {
                   mgr.close();
               }
          
           } else {

               throw new BadRequestException("Your Not The Owner");
           }

       } else {

           throw new BadRequestException("Your Not Logged In...");
       }
       }

	/**
	 * This gets the comments of the given object
	 * It uses HTTP GET method.
	 *
	 * @param step the entity to be inserted.
	 * @return The inserted entity.
	 */
	@SuppressWarnings({ "unchecked", "unused" })
	@ApiMethod(name = "listobjectComments", path="listobjectComments/{id}")
	public CollectionResponse<Comment> listentityComments(
			@Nullable @Named("cursor") String cursorString,
            @Nullable @Named("limit") Integer limit,
            @Named("id") Long id) {
		
		PersistenceManager mgr = null;
		Cursor cursor = null;
		List<Comment> execute = null;

		try {
			mgr = getPersistenceManager();
						
			Query query = mgr.newQuery(Comment.class);
			
			query.setFilter("targetid == targetidParam");
			query.declareParameters("Long targetidParam");
			//query.setOrdering("created asc");
						
			if (cursorString != null && cursorString != "") {
				cursor = Cursor.fromWebSafeString(cursorString);
				HashMap<String, Object> extensionMap = new HashMap<String, Object>();
				extensionMap.put(JDOCursorHelper.CURSOR_EXTENSION, cursor);
				query.setExtensions(extensionMap);
			}

			if (limit != null) {
				query.setRange(0, limit);
			}
			
			Long uid = id;
			
			execute = (List<Comment>) query.execute(uid);
			cursor = JDOCursorHelper.getCursor(execute);
			if (cursor != null)
				cursorString = cursor.toWebSafeString();

			// Tight loop for fetching all entities from datastore and accomodate
			// for lazy fetch.
			for (Comment obj : execute)
				;
		} finally {
			mgr.close();
		}

		return CollectionResponse.<Comment> builder().setItems(execute)
				.setNextPageToken(cursorString).build();
	}
	

	/**
	 * This gets the comments the current user owns
	 * It uses HTTP GET method.
	 *
	 * @param step the entity to be inserted.
	 * @return The inserted entity.
	 */
    @SuppressWarnings({"unchecked", "unused"})
    @ApiMethod(name = "listuserComment", path = "listuserComment/{id}")
    public CollectionResponse <Comment> listuserComment(@Nullable @Named("cursor") String cursorString, @Nullable @Named("limit") Integer limit, @Named("id") Long id) {

        PersistenceManager mgr = null;
        Cursor cursor = null;
        List <Comment> execute = null;

        try {
            mgr = getPersistenceManager();

            Query query = mgr.newQuery(Comment.class);
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

            execute = (List <Comment> ) query.execute(uid);
            cursor = JDOCursorHelper.getCursor(execute);
            if (cursor != null)
                cursorString = cursor.toWebSafeString();

            // Tight loop for fetching all entities from datastore and accomodate
            // for lazy fetch.
            for (Comment obj: execute)
            ;
        } finally {
            mgr.close();
        }

        return CollectionResponse. <Comment> builder().setItems(execute)
            .setNextPageToken(cursorString).build();
    }
    
	
	private boolean containsComment(Comment comment) {
		PersistenceManager mgr = getPersistenceManager();
		boolean contains = true;
		try {
			mgr.getObjectById(Comment.class, comment.getId());
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
