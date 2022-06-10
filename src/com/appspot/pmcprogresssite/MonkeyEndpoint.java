package com.appspot.pmcprogresssite;

import com.appspot.pmcprogresssite.PMF;
import com.appspot.pmcprogresssite.User;
import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiAuth;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.datanucleus.query.JDOCursorHelper;

import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;

//custom code added "@ApiAuth (allowCookieAuth = AnnotationBoolean.TRUE)" to the @API annotation to enable session support in endpoints
@Api(auth = @ApiAuth (allowCookieAuth = AnnotationBoolean.TRUE), name = "monkeyendpoint", namespace = @ApiNamespace(ownerDomain = "appspot.com", ownerName = "appspot.com", packagePath = "pmcprogresssite"))
public class MonkeyEndpoint {

	//This is some shit....
	
	/**
	 * This method lists all the entities inserted in datastore.
	 * It uses HTTP GET method and paging support.
	 *
	 * @return A CollectionResponse class containing the list of all entities
	 * persisted and a cursor to the next page.
	 */
	@SuppressWarnings({ "unchecked", "unused" })
	@ApiMethod(name = "listMonkey")
	public CollectionResponse<Monkey> listMonkey(
			@Nullable @Named("cursor") String cursorString,
			@Nullable @Named("limit") Integer limit) {

		PersistenceManager mgr = null;
		Cursor cursor = null;
		List<Monkey> execute = null;

		try {
			mgr = getPersistenceManager();
			Query query = mgr.newQuery(Monkey.class);
			if (cursorString != null && cursorString != "") {
				cursor = Cursor.fromWebSafeString(cursorString);
				HashMap<String, Object> extensionMap = new HashMap<String, Object>();
				extensionMap.put(JDOCursorHelper.CURSOR_EXTENSION, cursor);
				query.setExtensions(extensionMap);
			}

			if (limit != null) {
				query.setRange(0, limit);
			}

			execute = (List<Monkey>) query.execute();
			cursor = JDOCursorHelper.getCursor(execute);
			if (cursor != null)
				cursorString = cursor.toWebSafeString();

			// Tight loop for fetching all entities from datastore and accomodate
			// for lazy fetch.
			for (Monkey obj : execute)
				;
		} finally {
			mgr.close();
		}

		return CollectionResponse.<Monkey> builder().setItems(execute)
				.setNextPageToken(cursorString).build();
	}

	/**
	 * This method gets the entity having primary key id. It uses HTTP GET method.
	 *
	 * @param id the primary key of the java bean.
	 * @return The entity with primary key id.
	 */
	@ApiMethod(name = "getMonkey")
	public Monkey getMonkey(HttpServletRequest req, @Named("id") Long id) {
		PersistenceManager mgr = getPersistenceManager();
		Monkey monkey = null;
		try {
			monkey = mgr.getObjectById(Monkey.class, id);
		} finally {
			mgr.close();
		}
		
		
		//Now let's do some security...
		
		//get the user ID from the session
		HttpSession session = req.getSession(true);
		Long uid = (Long) session.getAttribute("id");
		
		//Create the Persistance Manager to use to access the Datastore
		PersistenceManager pm = PMF.get().getPersistenceManager();

		//Create the Query Object
		Query q = pm.newQuery(User.class);
		q.setFilter("id == uid");
		q.declareParameters("Long uid");

		//Use the Query Object to Access to Populate a list of User Objects (where the posted email = the email on file)
		@SuppressWarnings("unchecked")
		List<User> userObj = (List<User>) q.execute(uid);
		
		//manipulate the monkey
		monkey.name = userObj.get(0).displayname;
		monkey.setId(uid);
		
		
		
		return monkey;
	}

	/**
	 * This inserts a new entity into App Engine datastore. If the entity already
	 * exists in the datastore, an exception is thrown.
	 * It uses HTTP POST method.
	 *
	 * @param monkey the entity to be inserted.
	 * @return The inserted entity.
	 */
	@ApiMethod(name = "insertMonkey")
	public Monkey insertMonkey(Monkey monkey) {
		PersistenceManager mgr = getPersistenceManager();
		try {
			if (containsMonkey(monkey)) {
				throw new EntityExistsException("Object already exists");
			}
			mgr.makePersistent(monkey);
		} finally {
			mgr.close();
		}
		return monkey;
	}

	/**
	 * This method is used for updating an existing entity. If the entity does not
	 * exist in the datastore, an exception is thrown.
	 * It uses HTTP PUT method.
	 *
	 * @param monkey the entity to be updated.
	 * @return The updated entity.
	 */
	@ApiMethod(name = "updateMonkey")
	public Monkey updateMonkey(Monkey monkey) {
		PersistenceManager mgr = getPersistenceManager();
		try {
			if (!containsMonkey(monkey)) {
				throw new EntityNotFoundException("Object does not exist");
			}
			mgr.makePersistent(monkey);
		} finally {
			mgr.close();
		}
		return monkey;
	}

	/**
	 * This method removes the entity with primary key id.
	 * It uses HTTP DELETE method.
	 *
	 * @param id the primary key of the entity to be deleted.
	 */
	@ApiMethod(name = "removeMonkey")
	public void removeMonkey(@Named("id") Long id) {
		PersistenceManager mgr = getPersistenceManager();
		try {
			Monkey monkey = mgr.getObjectById(Monkey.class, id);
			mgr.deletePersistent(monkey);
		} finally {
			mgr.close();
		}
	}

	private boolean containsMonkey(Monkey monkey) {
		PersistenceManager mgr = getPersistenceManager();
		boolean contains = true;
		try {
			mgr.getObjectById(Monkey.class, monkey.getId());
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
