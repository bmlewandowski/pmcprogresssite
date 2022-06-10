package com.appspot.pmcprogresssite;

import com.appspot.pmcprogresssite.PMF;
import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiAuth;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;
import com.google.appengine.datanucleus.query.JDOCursorHelper;
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsOutputChannel;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Date;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.apache.commons.codec.binary.Base64;

@Api(auth = @ApiAuth (allowCookieAuth = AnnotationBoolean.TRUE), name = "progressendpoint", namespace = @ApiNamespace(ownerDomain = "appspot.com", ownerName = "appspot.com", packagePath = "pmcprogresssite"))
public class StepEndpoint {

	/**
	 * This method lists all the entities inserted in datastore.
	 * It uses HTTP GET method and paging support.
	 *
	 * @return A CollectionResponse class containing the list of all entities
	 * persisted and a cursor to the next page.
	 */
	
	 /**Used below to determine the size of chucks to read in. Should be > 1kb and < 10MB */
	  private static final int BUFFER_SIZE = 2 * 1024 * 1024;
	  	  
	@SuppressWarnings({"unchecked", "unused"})
	@ApiMethod(name = "listStep")
	public CollectionResponse<Step> listStep(
			@Nullable @Named("cursor") String cursorString,
			@Nullable @Named("limit") Integer limit) {

		PersistenceManager mgr = null;
		Cursor cursor = null;
		List<Step> execute = null;

		try {
			mgr = getPersistenceManager();
			Query query = mgr.newQuery(Step.class);
			
			if (cursorString != null && cursorString != "") {
				cursor = Cursor.fromWebSafeString(cursorString);
				HashMap<String, Object> extensionMap = new HashMap<String, Object>();
				extensionMap.put(JDOCursorHelper.CURSOR_EXTENSION, cursor);
				query.setExtensions(extensionMap);
			}

			if (limit != null) {
				query.setRange(0, limit);
			}

			execute = (List<Step>) query.execute();
			cursor = JDOCursorHelper.getCursor(execute);
			if (cursor != null)
				cursorString = cursor.toWebSafeString();

			// Tight loop for fetching all entities from datastore and accomodate
			// for lazy fetch.
			for (Step obj : execute)
				;
		} finally {
			mgr.close();
		}

		return CollectionResponse.<Step> builder().setItems(execute)
				.setNextPageToken(cursorString).build();
	}

	/**
	 * This method gets the entity having primary key id. It uses HTTP GET method.
	 *
	 * @param id the primary key of the java bean.
	 * @return The entity with primary key id.
	 */
	@ApiMethod(name = "getStep")
	public Step getStep(@Named("id") Long id) {
		PersistenceManager mgr = getPersistenceManager();
		Step step = null;
		try {
			step = mgr.getObjectById(Step.class, id);
		} finally {
			mgr.close();
		}
		return step;
	}

	/**
	 * This inserts a new entity into App Engine datastore. If the entity already
	 * exists in the datastore, an exception is thrown.
	 * It uses HTTP POST method.
	 *
	 * @param step the entity to be inserted.
	 * @return The inserted entity.
	 */
	@ApiMethod(name = "insertStep")
	public Step insertStep(HttpServletRequest req, Step step) throws BadRequestException, IOException {
		HttpSession session = req.getSession(false);
		// get user id from session
		Long uid = (Long) session.getAttribute("id");
		
		if (uid != null ){
			
			//Grab Image Data
			final String base64Image = step.thumbdata.getValue();
			byte[] imagedata = Base64.decodeBase64(base64Image);
			
			//File Name
		    String bucket = "progresssitebucket";
		    String object = UUID.randomUUID() + ".jpg";
		    //Add File Name to Model
		    step.thumbname = object;
		    step.entitytype = "step";
		    step.mediatype = "image";
		    
            //Make input stream from passed image data
            InputStream imgstream = new ByteArrayInputStream(imagedata);

			//Enable GcsService
    		GcsService gcsService = GcsServiceFactory.createGcsService();	
    		
            //Prepare File Name for Write to Bucket
            GcsFilename gcs_filename = new GcsFilename(bucket, object);
              		 
            //File Options
            GcsFileOptions.Builder options_builder = new GcsFileOptions.Builder();
            options_builder = options_builder.mimeType("image/jpeg");
            options_builder = options_builder.acl("public-read");
            GcsFileOptions options = options_builder.build();
            //Write from input stream to Data Store
    		GcsOutputChannel outputChannel = gcsService.createOrReplace(gcs_filename, options);
   		 	copyImage(imgstream, Channels.newOutputStream(outputChannel));
		    		    
		    //Get serving url
		    String gs_blob_key = "/gs/" + bucket + "/" + object;
		    BlobKey blob_key = BlobstoreServiceFactory.getBlobstoreService().createGsBlobKey(gs_blob_key);
		    ServingUrlOptions serving_options = ServingUrlOptions.Builder.withBlobKey(blob_key).secureUrl(true);
		    String serving_url = ImagesServiceFactory.getImagesService().getServingUrl(serving_options);
  		    
		    //Remove Thumbnail Item for Save in Datastore
		    step.setthumbnail(serving_url);
		    //add created Date
			step.created = new Date();
            //set current date
            step.modified = new Date();
			//clear thumbdata
			step.thumbdata = null;
		    
			PersistenceManager mgr = getPersistenceManager();
			try {
				if (step.getId() != null) {
					if (containsStep(step)) {
						throw new EntityExistsException("Object already exists");
					}
				}
				//save step
				mgr.makePersistent(step);
				
                //make feed
                Feed feed = new Feed();
                feed.ownerid = step.ownerid;
                feed.ownername = step.ownername;
                feed.ownerthumb = step.ownerthumb;
                feed.entitytype = "feed";
                feed.target = step.entitytype;
                feed.targetid = step.id;
                feed.targetowner = step.ownerid;
                feed.targetaction = "added";
                feed.targetthumb = step.thumbnail;
                feed.created = new Date();
                //save feed
                mgr.makePersistent(feed);				
				
			} finally {
				mgr.close();
			}
			
			return step;
		
		} else {
			
			throw new BadRequestException("Your Not Logged In...");

		}
	}

	/**
	 * This method is used for updating an existing entity. If the entity does not
	 * exist in the datastore, an exception is thrown.
	 * It uses HTTP PUT method.
	 *
	 * @param step the entity to be updated.
	 * @return The updated entity.
	 */
	@ApiMethod(name = "updateStep")
	public Step updateStep(HttpServletRequest req, Step step) throws BadRequestException, IOException {

        HttpSession session = req.getSession(false);
        // get user id from session
       Long uid = (Long) session.getAttribute("id");
        // get stepid from request
       Long eid = step.id;

       PersistenceManager mgr = getPersistenceManager();
        // see if logged in
       if (uid != null) {

           Query q = mgr.newQuery(Step.class);
           q.setFilter("ownerid == " + uid + " && id == " + eid);
           Map < String, Long > paramValues = new HashMap < String, Long > ();
           paramValues.put("uid", uid);
           paramValues.put("eid", eid);

           @SuppressWarnings("unchecked")
           List <Step> results = (List <Step> ) q.executeWithMap(paramValues);
           // see if owner
           if (!results.isEmpty()) {

               try {
                   if (!containsStep(step)) {
                       throw new EntityNotFoundException("Object does not exist");
                   }
                   
                   //set current date
                   step.modified = new Date();
                   
                   // see if its already a serving url
                   if (step.thumbdata.getValue() == "") {
       				//save step
       				mgr.makePersistent(step);
       				
                       //make feed
                       Feed feed = new Feed();
                       feed.ownerid = step.ownerid;
                       feed.ownername = step.ownername;
                       feed.ownerthumb = step.ownerthumb;
                       feed.entitytype = "feed";
                       feed.target = step.entitytype;
                       feed.targetid = step.id;
                       feed.targetowner = step.ownerid;
                       feed.targetaction = "edited";
                       feed.targetthumb = step.thumbnail;
                       feed.created = new Date();
                       //save feed
                       mgr.makePersistent(feed);	
                   } else {

                       //grab image data
                       final String base64Image = step.thumbdata.getValue();
                       byte[] imagedata = Base64.decodeBase64(base64Image);

                       //file name
                       String bucket = "progresssitebucket";
                       String object = UUID.randomUUID() + ".jpg";   
                       //get existing filename for delete
                       String delobject = step.thumbname;
                       
                       //make input stream from passed image data
                       InputStream imgstream = new ByteArrayInputStream(imagedata);

           				//enable GcsService
               			GcsService gcsService = GcsServiceFactory.createGcsService();	
               		
                       //prepare file name for write to bucket
                       GcsFilename gcs_filename = new GcsFilename(bucket, object);
                         		 
                       //file options
                       GcsFileOptions.Builder options_builder = new GcsFileOptions.Builder();
                       options_builder = options_builder.mimeType("image/jpeg");
                       options_builder = options_builder.acl("public-read");
                       GcsFileOptions options = options_builder.build();
                       //write from input stream to Data Store
               			GcsOutputChannel outputChannel = gcsService.createOrReplace(gcs_filename, options);
              		 	copyImage(imgstream, Channels.newOutputStream(outputChannel));

                       //get serving url
                       String gs_blob_key = "/gs/" + bucket + "/" + object;
                       BlobKey blob_key = BlobstoreServiceFactory.getBlobstoreService().createGsBlobKey(gs_blob_key);

                       ServingUrlOptions serving_options = ServingUrlOptions.Builder.withBlobKey(blob_key).secureUrl(true);
                       String serving_url = ImagesServiceFactory.getImagesService().getServingUrl(serving_options);
                       
           				//clear thumbdata
           				step.thumbdata = null;
           			
                       //set servingurl for new image
                       step.setthumbnail(serving_url);

                       //set name for new image
           		    	step.thumbname = object;
           		    	                       
                       //save step
                       mgr.makePersistent(step);
                                             
                       //make feed
                       Feed feed = new Feed();
                       feed.ownerid = step.ownerid;
                       feed.ownername = step.ownername;
                       feed.ownerthumb = step.ownerthumb;
                       feed.entitytype = "feed";
                       feed.target = step.entitytype;
                       feed.targetid = step.id;
                       feed.targetowner = step.ownerid;
                       feed.targetaction = "edited";
                       feed.targetthumb = step.thumbnail;
                       feed.created = new Date();
                       //save feed
                       mgr.makePersistent(feed);
                       
                       //prepare file name for old image delete from bucket
                       GcsFilename gcs_filename_del = new GcsFilename(bucket, delobject);

                       //delete from cloud storage
                       GcsServiceFactory.createGcsService().delete(gcs_filename_del);                      
                       
                   }

               } finally {
                   mgr.close();
               }
               return step;

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
	@ApiMethod(name = "removeStep")
	public void removeStep(HttpServletRequest req, @Named("id") Long id)throws BadRequestException, IOException  {

				PersistenceManager mgr = getPersistenceManager();	
				Step step = mgr.getObjectById(Step.class, id);
		        HttpSession session = req.getSession(false);
	        // get user id from session
		       Long uid = (Long) session.getAttribute("id");
		        // get stepid from request
		       Long eid = step.id;
		        // see if logged in
		       if (uid != null) {

		           Query q = mgr.newQuery(Step.class);
		           q.setFilter("ownerid == " + uid + " && id == " + eid);
		           Map <String, Long> paramValues = new HashMap <String, Long> ();
		           paramValues.put("uid", uid);
		           paramValues.put("eid", eid);

		           @SuppressWarnings("unchecked")
		           List <Step> results = (List <Step> ) q.executeWithMap(paramValues);
		           // see if owner
		           if (!results.isEmpty()) {

		               try {
		                   if (!containsStep(step)) {
		                       throw new EntityNotFoundException("Object does not exist");
		                   }			
		                	                       
		                       //delete image
		                       //File Name
		                       String bucket = "progresssitebucket";
		                       String object = step.thumbname;
		                       //Add File Name to Model
		                       step.thumbname = object;
		                       //Prepare File Name for Write to Bucket
		                       GcsFilename gcs_filename = new GcsFilename(bucket, object);

		                       //Delete to Cloud Storage
		                       GcsServiceFactory.createGcsService().delete(gcs_filename);
		                       
		                       //make feed
		                       Feed feed = new Feed();
		                       feed.ownerid = step.ownerid;
		                       feed.ownername = step.ownername;
		                       feed.ownerthumb = step.ownerthumb;
		                       feed.entitytype = "feed";
		                       feed.target = step.entitytype;
		                       feed.targetid = step.id;
		                       feed.targetowner = step.ownerid;
		                       feed.targetaction = "removed";
		                       feed.targetthumb = step.thumbnail;
		                       feed.created = new Date();
		                       //save feed
		                       mgr.makePersistent(feed);  
		                       //delete step
		                       mgr.deletePersistent(step);
		          
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
	 * This gets the steps of the given project
	 * It uses HTTP GET method.
	 *
	 * @param step the entity to be inserted.
	 * @return The inserted entity.
	 */
	@SuppressWarnings({ "unchecked", "unused" })
	@ApiMethod(name = "listprojectSteps", path="listprojectSteps/{id}")
	public CollectionResponse<Step> listconsoleStep(
			@Nullable @Named("cursor") String cursorString,
            @Nullable @Named("limit") Integer limit,
            @Named("id") Long id) {
		
		PersistenceManager mgr = null;
		Cursor cursor = null;
		List<Step> execute = null;

		try {
			mgr = getPersistenceManager();
						
			Query query = mgr.newQuery(Step.class);
			
			query.setFilter("projectid == projectidParam");
			query.declareParameters("Long projectidParam");
			query.setOrdering("created asc");
						
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
			
			execute = (List<Step>) query.execute(uid);
			cursor = JDOCursorHelper.getCursor(execute);
			if (cursor != null)
				cursorString = cursor.toWebSafeString();

			// Tight loop for fetching all entities from datastore and accomodate
			// for lazy fetch.
			for (Step obj : execute)
				;
		} finally {
			mgr.close();
		}

		return CollectionResponse.<Step> builder().setItems(execute)
				.setNextPageToken(cursorString).build();
	}
	

	private boolean containsStep(Step step) {
		PersistenceManager mgr = getPersistenceManager();
		boolean contains = true;
		try {
			mgr.getObjectById(Step.class, step.getId());
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

	/**
	 * Transfer the data from the inputStream to the outputStream. Then close both streams.
	 */
	private void copyImage(InputStream input, OutputStream output) throws IOException {
	  try {
	    byte[] buffer = new byte[BUFFER_SIZE];
	    int bytesRead = input.read(buffer);
	    while (bytesRead != -1) {
	      output.write(buffer, 0, bytesRead);
	      bytesRead = input.read(buffer);
	    }
	  } finally {
	    input.close();
	    output.close();
	  }
	}
	}