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
public class FolderEndpoint {

	/**
	 * This method lists all the entities inserted in datastore.
	 * It uses HTTP GET method and paging support.
	 *
	 * @return A CollectionResponse class containing the list of all entities
	 * persisted and a cursor to the next page.
	 */
	@SuppressWarnings({ "unchecked", "unused" })
	@ApiMethod(name = "listFolder")
	public CollectionResponse<Folder> listFolder(
			@Nullable @Named("cursor") String cursorString,
			@Nullable @Named("limit") Integer limit) {

		PersistenceManager mgr = null;
		Cursor cursor = null;
		List<Folder> execute = null;

		try {
			mgr = getPersistenceManager();
			Query query = mgr.newQuery(Folder.class);
			if (cursorString != null && cursorString != "") {
				cursor = Cursor.fromWebSafeString(cursorString);
				HashMap<String, Object> extensionMap = new HashMap<String, Object>();
				extensionMap.put(JDOCursorHelper.CURSOR_EXTENSION, cursor);
				query.setExtensions(extensionMap);
			}

			if (limit != null) {
				query.setRange(0, limit);
			}

			execute = (List<Folder>) query.execute();
			cursor = JDOCursorHelper.getCursor(execute);
			if (cursor != null)
				cursorString = cursor.toWebSafeString();

			// Tight loop for fetching all entities from datastore and accomodate
			// for lazy fetch.
			for (Folder obj : execute)
				;
		} finally {
			mgr.close();
		}

		return CollectionResponse.<Folder> builder().setItems(execute)
				.setNextPageToken(cursorString).build();
	}

	/**
	 * This method gets the entity having primary key id. It uses HTTP GET method.
	 *
	 * @param id the primary key of the java bean.
	 * @return The entity with primary key id.
	 */
	@ApiMethod(name = "getFolder")
	public Folder getFolder(@Named("id") Long id) {
		PersistenceManager mgr = getPersistenceManager();
		Folder folder = null;
		try {
			folder = mgr.getObjectById(Folder.class, id);
		} finally {
			mgr.close();
		}
		return folder;
	}

	/**
	 * This gets the folders the current user owns
	 * It uses HTTP GET method.
	 *
	 * @param step the entity to be inserted.
	 * @return The inserted entity.
	 */
    @SuppressWarnings({"unchecked", "unused"})
    @ApiMethod(name = "listconsoleFolder", path = "listconsoleFolder/{id}")
    public CollectionResponse <Folder> listconsoleFolder(@Nullable @Named("cursor") String cursorString, @Nullable @Named("limit") Integer limit, @Named("id") Long id) {

        PersistenceManager mgr = null;
        Cursor cursor = null;
        List <Folder> execute = null;

        try {
            mgr = getPersistenceManager();

            Query query = mgr.newQuery(Folder.class);
            
            query.setFilter("ownerid == owneridParam");
            query.declareParameters("Long owneridParam");
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

            execute = (List <Folder> ) query.execute(uid);
            cursor = JDOCursorHelper.getCursor(execute);
            if (cursor != null)
                cursorString = cursor.toWebSafeString();

            // Tight loop for fetching all entities from datastore and accomodate
            // for lazy fetch.
            for (Folder obj: execute)
            ;
        } finally {
            mgr.close();
        }

        return CollectionResponse.<Folder> builder().setItems(execute)
            .setNextPageToken(cursorString).build();
    }

	/**
	 * This inserts a new entity into App Engine datastore. If the entity already
	 * exists in the datastore, an exception is thrown.
	 * It uses HTTP POST method.
	 *
	 * @param folder the entity to be inserted.
	 * @return The inserted entity.
	 */
	@ApiMethod(name = "insertFolder")
	public Folder insertFolder(HttpServletRequest req, Folder folder) throws BadRequestException, IOException {
		HttpSession session = req.getSession(true);
		
		// get user id from session
		Long uid = (Long) session.getAttribute("id");
		if (uid != null ){
            //set current date
            folder.modified = new Date();
			folder.created = new Date();
			folder.entitytype = "folder";
			PersistenceManager mgr = getPersistenceManager();
			try {
				if (folder.getId() != null) {
					if (containsFolder(folder)) {
						throw new EntityExistsException("Object already exists");
					}
				}
				//save folder
				mgr.makePersistent(folder);
				
                //make feed
                Feed feed = new Feed();
                feed.ownerid = folder.ownerid;
                feed.ownername = folder.ownername;
                feed.ownerthumb = folder.ownerthumb;
                feed.entitytype = "feed";
                feed.target = folder.entitytype;
                feed.targetid = folder.id;
                feed.targetowner = folder.ownerid;
                feed.targetaction = "added";
                feed.created = new Date();
                //save feed
                mgr.makePersistent(feed);      				
				
			} finally {
				mgr.close();
			}
			
			return folder;
		
		} else {
			
			throw new BadRequestException("Your Not Logged In...");

		}
	}

	/**
	 * This method is used for updating an existing entity. If the entity does not
	 * exist in the datastore, an exception is thrown.
	 * It uses HTTP PUT method.
	 *
	 * @param folder the entity to be updated.
	 * @return The updated entity.
	 */
	@ApiMethod(name = "updateFolder")
    public Folder updateFolder(HttpServletRequest req, Folder folder) throws BadRequestException, IOException {
		
    	HttpSession session = req.getSession(true);
        // get user id from session
       Long uid = (Long) session.getAttribute("id");
        // get folderid from request
       Long eid = folder.id;
       
       PersistenceManager mgr = getPersistenceManager();
       // see if logged in
      if (uid != null) {
    	  
          Query q = mgr.newQuery(Folder.class);
          q.setFilter("ownerid == " + uid + " && id == " + eid);
          Map < String, Long > paramValues = new HashMap < String, Long > ();
          paramValues.put("uid", uid);
          paramValues.put("eid", eid);

          @SuppressWarnings("unchecked")
          List <Folder> results = (List<Folder>)q.executeWithMap(paramValues);
          
          // see if owner
          if (!results.isEmpty()) {
              try {
                  if (!containsFolder(folder)) {
                      throw new EntityNotFoundException("Object does not exist");
                  }
                
                  if (folder.systemfolder == true || folder.defaultfolder == true) {
                      throw new EntityNotFoundException("Object is system folder");               	                 	  
                  }
                	  
                  //set current date
                  folder.modified = new Date();
                  //save folder
                  mgr.makePersistent(folder);
                  
                  //make feed
                  Feed feed = new Feed();
                  feed.ownerid = folder.ownerid;
                  feed.ownername = folder.ownername;
                  feed.ownerthumb = folder.ownerthumb;
                  feed.entitytype = "feed";
                  feed.target = folder.entitytype;
                  feed.targetid = folder.id;
                  feed.targetowner = folder.ownerid;
                  feed.targetaction = "edited";
                  feed.created = new Date();
                  //save feed
                  mgr.makePersistent(feed);                   

              } finally {
                  mgr.close();
              }
              return folder;
              

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
	@SuppressWarnings({ "unchecked" })
	@ApiMethod(name = "removeFolder")
	public void removeFolder(HttpServletRequest req, @Named("id") Long id) throws BadRequestException, IOException {
		PersistenceManager mgr = getPersistenceManager();
		Folder folder = mgr.getObjectById(Folder.class, id);		
        HttpSession session = req.getSession(true);
    // get user id from session
       Long uid = (Long) session.getAttribute("id");
        // get folderid from request
       Long eid = folder.id;
        // see if logged in
       if (uid != null) {

           Query q = mgr.newQuery(Folder.class);
           q.setFilter("ownerid == " + uid + " && id == " + eid);
           Map <String, Long> paramValues = new HashMap <String, Long> ();
           paramValues.put("uid", uid);
           paramValues.put("eid", eid);

           List <Folder> results = (List<Folder>) q.executeWithMap(paramValues);
           // see if owner
           if (!results.isEmpty()) {

               try {
                   if (!containsFolder(folder)) {
                       throw new EntityNotFoundException("Object does not exist");
                   }
                   
                   if (folder.systemfolder == true || folder.defaultfolder == true) {
                       throw new EntityNotFoundException("Object is system folder");               	                 	  
                   }
                   
                   //make feed
                   Feed feed = new Feed();
                   feed.ownerid = folder.ownerid;
                   feed.ownername = folder.ownername;
                   feed.ownerthumb = folder.ownerthumb;
                   feed.entitytype = "feed";
                   feed.target = folder.entitytype;
                   feed.targetid = folder.id;
                   feed.targetowner = folder.ownerid;
                   feed.targetaction = "removed";
                   feed.created = new Date();
                   //save feed
                   mgr.makePersistent(feed);  
                           //delete folder        	                       
	                       mgr.deletePersistent(folder);
	          
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
	
	
	private boolean containsFolder(Folder folder) {
		PersistenceManager mgr = getPersistenceManager();
		boolean contains = true;
		try {
			mgr.getObjectById(Folder.class, folder.getId());
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
