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
public class BlockEndpoint {
	/**
	 * This method lists all the entities inserted in datastore.
	 * It uses HTTP GET method and paging support.
	 *
	 * @return A CollectionResponse class containing the list of all entities
	 * persisted and a cursor to the next page.
	 */
	@SuppressWarnings({ "unchecked", "unused" })
	@ApiMethod(name = "listBlock")
	public CollectionResponse<Block> listBlock(
			@Nullable @Named("cursor") String cursorString,
			@Nullable @Named("limit") Integer limit) {

		PersistenceManager mgr = null;
		Cursor cursor = null;
		List<Block> execute = null;

		try {
			mgr = getPersistenceManager();
			Query query = mgr.newQuery(Block.class);
			if (cursorString != null && cursorString != "") {
				cursor = Cursor.fromWebSafeString(cursorString);
				HashMap<String, Object> extensionMap = new HashMap<String, Object>();
				extensionMap.put(JDOCursorHelper.CURSOR_EXTENSION, cursor);
				query.setExtensions(extensionMap);
			}

			if (limit != null) {
				query.setRange(0, limit);
			}

			execute = (List<Block>) query.execute();
			cursor = JDOCursorHelper.getCursor(execute);
			if (cursor != null)
				cursorString = cursor.toWebSafeString();

			// Tight loop for fetching all entities from datastore and accomodate
			// for lazy fetch.
			for (Block obj : execute)
				;
		} finally {
			mgr.close();
		}

		return CollectionResponse.<Block> builder().setItems(execute)
				.setNextPageToken(cursorString).build();
	}

	/**
	 * This method gets the entity having primary key id. It uses HTTP GET method.
	 *
	 * @param id the primary key of the java bean.
	 * @return The entity with primary key id.
	 */
	@ApiMethod(name = "getBlock")
	public Block getBlock(@Named("id") Long id) {
		PersistenceManager mgr = getPersistenceManager();
		Block block = null;
		try {
			block = mgr.getObjectById(Block.class, id);
		} finally {
			mgr.close();
		}
		return block;
	}

	/**
	 * This inserts a new entity into App Engine datastore. If the entity already
	 * exists in the datastore, an exception is thrown.
	 * It uses HTTP POST method.
	 *
	 * @param block the entity to be inserted.
	 * @return The inserted entity.
	 */
	@SuppressWarnings("unchecked")
	@ApiMethod(name = "insertBlock")
	public Block insertBlock(HttpServletRequest req, Block block) throws BadRequestException, IOException {
		HttpSession session = req.getSession(false);
		// get user id from session
		Long uid = (Long) session.getAttribute("id");
		
		if (uid != null ){
		PersistenceManager mgr = getPersistenceManager();
		try {
            if (block.getId() != null) {
                if (containsBlock(block)) {
                    throw new EntityExistsException("Object already exists");
                }
            }
			
			//see if like already exists

            Long eid = block.targetid;
            Query q = mgr.newQuery(Block.class);
            q.setFilter("ownerid == " + uid + " && targetid == " + eid);
            Map <String, Long> paramValues = new HashMap <String, Long> ();
            paramValues.put("uid", uid);
            paramValues.put("eid", eid);	
            
            List<Block> results = (List<Block>) q.executeWithMap(paramValues);
            //see if like exists
            if (!results.isEmpty()) {
            	Long objectid = results.get(0).getId();
                    //delete like
            	block = mgr.getObjectById(Block.class, objectid);
            	
                //make feed
                Feed feed = new Feed();
                feed.ownerid = block.ownerid;
                feed.ownername = block.ownername;
                feed.ownerthumb = block.ownerthumb;
                feed.entitytype = "feed";
                feed.target = block.entitytype;
                feed.targetid = block.id;
                feed.targetowner = block.targetowner;
                feed.targetaction = "removed";
                feed.created = new Date();
                //save feed
                mgr.makePersistent(feed);  
                
                //delete block
                    mgr.deletePersistent(block);  
                    
        			//TODO decrement count
       
        } else {
        	block.created = new Date();
            //save like            
            mgr.makePersistent(block); 
      
            //make feed
            Feed feed = new Feed();
            feed.ownerid = block.ownerid;
            feed.ownername = block.ownername;
            feed.ownerthumb = block.ownerthumb;
            feed.entitytype = "feed";
            feed.target = block.entitytype;
            feed.targetid = block.id;
            feed.targetowner = block.targetowner;
            feed.targetaction = "added";
            feed.created = new Date();
            //save feed
            mgr.makePersistent(feed);   
            
			//TODO increment count
        }
                    
		} finally {
			mgr.close();
		}
		return block;
		
		} else {
			
			throw new BadRequestException("Your Not Logged In...");
		}
	}

	/**
	 * This method is used for updating an existing entity. If the entity does not
	 * exist in the datastore, an exception is thrown.
	 * It uses HTTP PUT method.
	 *
	 * @param block the entity to be updated.
	 * @return The updated entity.
	 */
	@ApiMethod(name = "updateBlock")
	public Block updateBlock(Block block) {
		PersistenceManager mgr = getPersistenceManager();
		try {
			if (!containsBlock(block)) {
				throw new EntityNotFoundException("Object does not exist");
			}
			mgr.makePersistent(block);
		} finally {
			mgr.close();
		}
		return block;
	}

	/**
	 * This method removes the entity with primary key id.
	 * It uses HTTP DELETE method.
	 *
	 * @param id the primary key of the entity to be deleted.
	 */
	@ApiMethod(name = "removeBlock")
	public void removeBlock(@Named("id") Long id) {
		PersistenceManager mgr = getPersistenceManager();
		try {
			Block block = mgr.getObjectById(Block.class, id);
			mgr.deletePersistent(block);
		} finally {
			mgr.close();
		}
	}
	
	/**
	 * This gets the blocks the current user owns
	 * It uses HTTP GET method.
	 *
	 * @param step the entity to be inserted.
	 * @return The inserted entity.
	 */
    @SuppressWarnings({"unchecked", "unused"})
    @ApiMethod(name = "listuserBlock", path = "listuserBlock/{id}")
    public CollectionResponse <Block> listuserBlock(@Nullable @Named("cursor") String cursorString, @Nullable @Named("limit") Integer limit, @Named("id") Long id) {

        PersistenceManager mgr = null;
        Cursor cursor = null;
        List <Block> execute = null;

        try {
            mgr = getPersistenceManager();

            Query query = mgr.newQuery(Block.class);
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

            execute = (List <Block> ) query.execute(uid);
            cursor = JDOCursorHelper.getCursor(execute);
            if (cursor != null)
                cursorString = cursor.toWebSafeString();

            // Tight loop for fetching all entities from datastore and accomodate
            // for lazy fetch.
            for (Block obj: execute)
            ;
        } finally {
            mgr.close();
        }

        return CollectionResponse. <Block> builder().setItems(execute)
            .setNextPageToken(cursorString).build();
    }
    

	private boolean containsBlock(Block block) {
		PersistenceManager mgr = getPersistenceManager();
		boolean contains = true;
		try {
			mgr.getObjectById(Block.class, block.getId());
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
