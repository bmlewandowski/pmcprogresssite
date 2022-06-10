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
import com.google.appengine.api.datastore.Cursor;
//import com.google.appengine.api.images.ImagesServiceFactory;
//import com.google.appengine.api.images.ServingUrlOptions;
import com.google.appengine.datanucleus.query.JDOCursorHelper;
//import com.google.appengine.tools.cloudstorage.GcsFileOptions;
//import com.google.appengine.tools.cloudstorage.GcsFilename;
//import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
//import com.google.appengine.tools.cloudstorage.GcsService;
//import com.google.appengine.tools.cloudstorage.GcsOutputChannel;
//import com.google.appengine.tools.cloudstorage.RetryParams;






import java.io.IOException;
//import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
//import java.util.UUID;
import java.util.Date;

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
public class FeedEndpoint {

	/**
	 * This method lists all the entities inserted in datastore.
	 * It uses HTTP GET method and paging support.
	 *
	 * @return A CollectionResponse class containing the list of all entities
	 * persisted and a cursor to the next page.
	 */
	@SuppressWarnings({ "unchecked", "unused" })
	@ApiMethod(name = "listFeed")
	public CollectionResponse<Feed> listFeed(
			@Nullable @Named("cursor") String cursorString,
			@Nullable @Named("limit") Integer limit) {

		PersistenceManager mgr = null;
		Cursor cursor = null;
		List<Feed> execute = null;

		try {
			mgr = getPersistenceManager();
			Query query = mgr.newQuery(Feed.class);
			if (cursorString != null && cursorString != "") {
				cursor = Cursor.fromWebSafeString(cursorString);
				HashMap<String, Object> extensionMap = new HashMap<String, Object>();
				extensionMap.put(JDOCursorHelper.CURSOR_EXTENSION, cursor);
				query.setExtensions(extensionMap);
			}

			if (limit != null) {
				query.setRange(0, limit);
			}

			execute = (List<Feed>) query.execute();
			cursor = JDOCursorHelper.getCursor(execute);
			if (cursor != null)
				cursorString = cursor.toWebSafeString();

			// Tight loop for fetching all entities from datastore and accomodate
			// for lazy fetch.
			for (Feed obj : execute)
				;
		} finally {
			mgr.close();
		}

		return CollectionResponse.<Feed> builder().setItems(execute)
				.setNextPageToken(cursorString).build();
	}

	/**
	 * This method gets the entity having primary key id. It uses HTTP GET method.
	 *
	 * @param id the primary key of the java bean.
	 * @return The entity with primary key id.
	 */
	@ApiMethod(name = "getFeed")
	public Feed getFeed(@Named("id") Long id) {
		PersistenceManager mgr = getPersistenceManager();
		Feed feed = null;
		try {
			feed = mgr.getObjectById(Feed.class, id);
		} finally {
			mgr.close();
		}
		return feed;
	}

	/**
	 * This inserts a new entity into App Engine datastore. If the entity already
	 * exists in the datastore, an exception is thrown.
	 * It uses HTTP POST method.
	 *
	 * @param feed the entity to be inserted.
	 * @return The inserted entity.
	 */
	@ApiMethod(name = "insertFeed")
	public Feed insertFeed(HttpServletRequest req, Feed feed) throws BadRequestException, IOException {
		HttpSession session = req.getSession(false);
		// get user id from session
		Long uid = (Long) session.getAttribute("id");
		
		if (uid != null ){

		PersistenceManager mgr = getPersistenceManager();
		try {
			if (containsFeed(feed)) {
				throw new EntityExistsException("Object already exists");
			}
			
		    //set current date
			feed.created = new Date();
			
			mgr.makePersistent(feed);
		} finally {
			mgr.close();
		}
		return feed;
		
		} else {
			
			throw new BadRequestException("Your Not Logged In...");

		}
	}

	/**
	 * This method is used for updating an existing entity. If the entity does not
	 * exist in the datastore, an exception is thrown.
	 * It uses HTTP PUT method.
	 *
	 * @param feed the entity to be updated.
	 * @return The updated entity.
	 */
	@ApiMethod(name = "updateFeed")
	public Feed updateFeed(Feed feed) {
		PersistenceManager mgr = getPersistenceManager();
		try {
			if (!containsFeed(feed)) {
				throw new EntityNotFoundException("Object does not exist");
			}
			mgr.makePersistent(feed);
		} finally {
			mgr.close();
		}
		return feed;
	}

	/**
	 * This method removes the entity with primary key id.
	 * It uses HTTP DELETE method.
	 *
	 * @param id the primary key of the entity to be deleted.
	 */
	@ApiMethod(name = "removeFeed")
	public void removeFeed(@Named("id") Long id) {
		PersistenceManager mgr = getPersistenceManager();
		try {
			Feed feed = mgr.getObjectById(Feed.class, id);
			mgr.deletePersistent(feed);
		} finally {
			mgr.close();
		}
	}
	
	/**
	 * This gets the feeds the current user owns
	 * It uses HTTP GET method.
	 *
	 * @param step the entity to be inserted.
	 * @return The inserted entity.
	 */
    @SuppressWarnings({"unchecked", "unused"})
    @ApiMethod(name = "listuserFeed", path = "listuserFeed/{id}")
    public CollectionResponse <Feed> listuserFeed(@Nullable @Named("cursor") String cursorString, @Nullable @Named("limit") Integer limit, @Named("id") Long id) {

        PersistenceManager mgr = null;
        Cursor cursor = null;
        List <Feed> execute = null;

        try {
            mgr = getPersistenceManager();

            Query query = mgr.newQuery(Feed.class);
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

            execute = (List <Feed> ) query.execute(uid);
            cursor = JDOCursorHelper.getCursor(execute);
            if (cursor != null)
                cursorString = cursor.toWebSafeString();

            // Tight loop for fetching all entities from datastore and accomodate
            // for lazy fetch.
            for (Feed obj: execute)
            ;
        } finally {
            mgr.close();
        }

        return CollectionResponse. <Feed> builder().setItems(execute)
            .setNextPageToken(cursorString).build();
    }
    
    

	private boolean containsFeed(Feed feed) {
		PersistenceManager mgr = getPersistenceManager();
		boolean contains = true;
		try {
			mgr.getObjectById(Feed.class, feed.getId());
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
