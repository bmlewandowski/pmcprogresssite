package com.appspot.pmcprogresssite;

import com.appspot.pmcprogresssite.PMF;
import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiAuth;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.BadRequestException;
//import com.google.api.server.spi.response.CollectionResponse;
//import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.images.Image;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.Transform;
//import com.google.appengine.datanucleus.query.JDOCursorHelper;
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;



//import javax.annotation.Nullable;
import javax.inject.Named;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.jdo.PersistenceManager;
//import javax.jdo.Query;



import org.apache.commons.codec.binary.Base64;

@Api(auth = @ApiAuth(allowCookieAuth = AnnotationBoolean.TRUE), name = "progressendpoint", namespace = @ApiNamespace(ownerDomain = "appspot.com", ownerName = "appspot.com", packagePath = "pmcprogresssite"))
public class UserEndpoint {
		
	/**
	 * This method lists all the entities inserted in datastore.
	 * It uses HTTP GET method and paging support.
	 *
	 * @return A CollectionResponse class containing the list of all entities
	 * persisted and a cursor to the next page.
	 */
//	@SuppressWarnings({ "unchecked", "unused" })
//	@ApiMethod(name = "listUser")
//	public CollectionResponse<User> listUser(
//			@Nullable @Named("cursor") String cursorString,
//			@Nullable @Named("limit") Integer limit) {
//
//		PersistenceManager mgr = null;
//		Cursor cursor = null;
//		List<User> execute = null;
//
//		try {
//			mgr = getPersistenceManager();
//			Query query = mgr.newQuery(User.class);
//			if (cursorString != null && cursorString != "") {
//				cursor = Cursor.fromWebSafeString(cursorString);
//				HashMap<String, Object> extensionMap = new HashMap<String, Object>();
//				extensionMap.put(JDOCursorHelper.CURSOR_EXTENSION, cursor);
//				query.setExtensions(extensionMap);
//			}
//
//			if (limit != null) {
//				query.setRange(0, limit);
//			}
//
//			execute = (List<User>) query.execute();
//			cursor = JDOCursorHelper.getCursor(execute);
//			if (cursor != null)
//				cursorString = cursor.toWebSafeString();
//
//			// Tight loop for fetching all entities from datastore and accomodate
//			// for lazy fetch.
//			for (User obj : execute)
//				;
//		} finally {
//			mgr.close();
//		}
//
//		return CollectionResponse.<User> builder().setItems(execute)
//				.setNextPageToken(cursorString).build();
//	}

	/**
	 * This method gets the entity having primary key id. It uses HTTP GET method.
	 *
	 * @param id the primary key of the java bean.
	 * @return The entity with primary key id.
	 */
	@ApiMethod(name = "getUser")
	public User getUser(@Named("id") Long id) {
		PersistenceManager mgr = getPersistenceManager();
		User user = null;
		try {
			user = mgr.getObjectById(User.class, id);
		} finally {
			mgr.close();
		}
		
		user.password = "";
		
		return user;
	}

	/**
	 * This inserts a new entity into App Engine datastore. If the entity already
	 * exists in the datastore, an exception is thrown.
	 * It uses HTTP POST method.
	 *
	 * @param user the entity to be inserted.
	 * @return The inserted entity.
	 */
	@ApiMethod(name = "insertUser")
	public User insertUser(User user) {
		PersistenceManager mgr = getPersistenceManager();
		try {
			if (containsUser(user)) {
				throw new EntityExistsException("Object already exists");
			}
			mgr.makePersistent(user);
		} finally {
			mgr.close();
		}
		return user;
	}

	/**
	 * This method is used for updating an existing entity. If the entity does not
	 * exist in the datastore, an exception is thrown.
	 * It uses HTTP PUT method.
	 *
	 * @param user the entity to be updated.
	 * @return The updated entity.
	 */
    @ApiMethod(name = "updateUser")
    public User updateUser(HttpServletRequest req, User incomingUser) throws BadRequestException, IOException {
    	
        HttpSession session = req.getSession(false);
        // get user id from session
       Long uid = (Long) session.getAttribute("id");

       PersistenceManager mgr = getPersistenceManager();
        // see if logged in
       if (uid != null) {
           
    	   User user = getUser(uid);
                	           	  
               try {
                   if (!containsUser(user)) {
                       throw new EntityNotFoundException("User does not exist");
                   }
                   

                  //set current date
                  user.modified = new Date();
            	  
                   // see if theres a new image
                   if (incomingUser.thumbdata.getValue() == "") {
                	   //save user
                       mgr.makePersistent(user);
                   } else {

                       //Grab Image Data
                       final String base64Image = incomingUser.thumbdata.getValue();
                       byte[] imagedata = Base64.decodeBase64(base64Image);

                       //File Name
                   	   String userid = user.id.toString();
                       String bucket = "progresssiteusers";
                   	   String portraitname = userid + ".jpg";
                	

                       //Prepare File Name for Write to Bucket
                       GcsFilename gcs_filename = new GcsFilename(bucket, portraitname);

                       //file options
                       GcsFileOptions.Builder options_builder = new GcsFileOptions.Builder();
                       options_builder = options_builder.mimeType("image/jpeg");
                       //commented out to leave bucket default
                       //options_builder = options_builder.acl("public-read");
                       GcsFileOptions options = options_builder.build();

                       //write to Cloud Storage
                       GcsServiceFactory.createGcsService().createOrReplace(gcs_filename, options, ByteBuffer.wrap(imagedata));
                      
                    // Save smaller version
                    //make temp copy to resize with serving_url  
                       
                       ImagesService imagesService = ImagesServiceFactory.getImagesService();
                       Image oldImage = ImagesServiceFactory.makeImage(imagedata);
                       Transform resize = ImagesServiceFactory.makeResize(200, 300);
                       Image newImage = imagesService.applyTransform(resize, oldImage);
                       byte[] newImageData = newImage.getImageData();
                       
                                         	
                    //File Name
                   	String thumbnailname = userid + "_TN.jpg";
                   		                   	
                   	GcsFilename gcs_filename_th = new GcsFilename(bucket, thumbnailname); 
                   	
                    //write to Cloud Storage
                    GcsServiceFactory.createGcsService().createOrReplace(gcs_filename_th, options, ByteBuffer.wrap(newImageData));        	

               			//save image paths to user
               			user.setportrait("http://storage.googleapis.com/progresssiteusers/" + portraitname);	
                   		user.setthumbnail("http://storage.googleapis.com/progresssiteusers/" + thumbnailname);	
                   	                                    
                       //clear thumbdata
                       incomingUser.setthumbdata(null);
                       
                       //save user
                       mgr.makePersistent(user);

                   }

               } finally {
                   mgr.close();
               }
               
               return user;


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
	@ApiMethod(name = "removeUser")
	public void removeUser(@Named("id") Long id) {
		PersistenceManager mgr = getPersistenceManager();
		try {
			User user = mgr.getObjectById(User.class, id);
			mgr.deletePersistent(user);
		} finally {
			mgr.close();
		}
	}

	private boolean containsUser(User user) {
		PersistenceManager mgr = getPersistenceManager();
		boolean contains = true;
		try {
			mgr.getObjectById(User.class, user.getId());
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
