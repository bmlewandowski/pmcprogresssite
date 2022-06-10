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
import java.util.Iterator;
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

@Api(auth = @ApiAuth(allowCookieAuth = AnnotationBoolean.TRUE), name = "progressendpoint", namespace = @ApiNamespace(ownerDomain = "appspot.com", ownerName = "appspot.com", packagePath = "pmcprogresssite"))
public class ProjectEndpoint {
	
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
    @ApiMethod(name = "listProject")
    public CollectionResponse<Project> listProject(
    		@Nullable @Named("cursor") String cursorString, 
    		@Nullable @Named("limit") Integer limit) {

        PersistenceManager mgr = null;
        Cursor cursor = null;
        List<Project> execute = null;

        try {
            mgr = getPersistenceManager();
            Query query = mgr.newQuery(Project.class);

            if (cursorString != null && cursorString != "") {
                cursor = Cursor.fromWebSafeString(cursorString);
                HashMap<String, Object> extensionMap = new HashMap<String, Object> ();
                extensionMap.put(JDOCursorHelper.CURSOR_EXTENSION, cursor);
                query.setExtensions(extensionMap);
            }

            if (limit != null) {
                query.setRange(0, limit);
            }

            execute = (List < Project > ) query.execute();
            cursor = JDOCursorHelper.getCursor(execute);
            if (cursor != null)
                cursorString = cursor.toWebSafeString();

            // Tight loop for fetching all entities from datastore and accomodate
            // for lazy fetch.
            for (Project obj: execute)
            ;
        } finally {
            mgr.close();
        }

        return CollectionResponse. < Project > builder().setItems(execute)
            .setNextPageToken(cursorString).build();
    }

    /**
     * This method gets the entity having primary key id. It uses HTTP GET method.
     *
     * @param id the primary key of the java bean.
     * @return The entity with primary key id.
     */
    @ApiMethod(name = "getProject")
    public Project getProject(@Named("id") Long id) {
        PersistenceManager mgr = getPersistenceManager();
        Project project = null;
        try {
            project = mgr.getObjectById(Project.class, id);
        } finally {
            mgr.close();
        }
        return project;
    }

    /**
     * This inserts a new entity into App Engine datastore. If the entity already
     * exists in the datastore, an exception is thrown.
     * It uses HTTP POST method.
     *
     * @param project the entity to be inserted.
     * @return The inserted entity.
     * @throws BadRequestException
     * @throws IOException
     */
    @ApiMethod(name = "insertProject")
    public Project insertProject(HttpServletRequest req, Project project) throws BadRequestException, IOException {
        HttpSession session = req.getSession(false);
        // get user id from session
        Long uid = (Long) session.getAttribute("id");

        if (uid != null) {

            //Grab Image Data
            final String base64Image = project.thumbdata.getValue();
            byte[] imagedata = Base64.decodeBase64(base64Image);

            //File Name
            String bucket = "progresssitebucket";
            String object = UUID.randomUUID() + ".jpg";
            //Add File Name to Model
            project.thumbname = object;
            project.entitytype = "project";

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
            project.setthumbnail(serving_url);
            //set current date
            project.modified = new Date();
            project.created = new Date();
            //clear thumbdata
            project.thumbdata = null;


            PersistenceManager mgr = getPersistenceManager();
            try {
                if (project.getId() != null) {
                    if (containsProject(project)) {
                        throw new EntityExistsException("Object already exists");
                    }
                }
                //save project
                mgr.makePersistent(project);
                
                //make feed
                Feed feed = new Feed();
                feed.ownerid = project.ownerid;
                feed.ownername = project.ownername;
                feed.ownerthumb = project.ownerthumb;
                feed.entitytype = "feed";
                feed.target = project.entitytype;
                feed.targetid = project.id;
                feed.targetowner = project.ownerid;
                feed.targetaction = "added";
                feed.targetthumb = project.thumbnail;
                feed.created = new Date();
                //save feed
                mgr.makePersistent(feed);
                
            } finally {
                mgr.close();
            }

            return project;

        } else {

            throw new BadRequestException("Your Not Logged In...");

        }
    }

    /**
     * This method is used for updating an existing entity. If the entity does not
     * exist in the datastore, an exception is thrown.
     * It uses HTTP PUT method.
     *
     * @param project the entity to be updated.
     * @return The updated entity.
     */
    @ApiMethod(name = "updateProject")
    public Project updateProject(HttpServletRequest req, Project project) throws BadRequestException, IOException {
    	
        HttpSession session = req.getSession(false);
        // get user id from session
       Long uid = (Long) session.getAttribute("id");
        // get projectid from request
       Long eid = project.id;

       PersistenceManager mgr = getPersistenceManager();
        // see if logged in
       if (uid != null) {

           Query q = mgr.newQuery(Project.class);
           q.setFilter("ownerid == " + uid + " && id == " + eid);
           Map < String, Long > paramValues = new HashMap < String, Long > ();
           paramValues.put("uid", uid);
           paramValues.put("eid", eid);

           @SuppressWarnings("unchecked")
           List <Project> results = (List<Project>)q.executeWithMap(paramValues);
           
           // see if owner
           if (!results.isEmpty()) {

               try {
                   if (!containsProject(project)) {
                       throw new EntityNotFoundException("Object does not exist");
                   }
                   
                   //set current date
                   project.modified = new Date();
                   
                   // see if its already a serving url
                   if (project.thumbdata.getValue() == "") {
                	   
                       //save project
                       mgr.makePersistent(project);
                       
                       //make feed
                       Feed feed = new Feed();
                       feed.ownerid = project.ownerid;
                       feed.ownername = project.ownername;
                       feed.ownerthumb = project.ownerthumb;
                       feed.entitytype = "feed";
                       feed.target = project.entitytype;
                       feed.targetid = project.id;
                       feed.targetowner = project.ownerid;
                       feed.targetaction = "edited";
                       feed.targetthumb = project.thumbnail;
                       feed.created = new Date();
                       //save feed
                       mgr.makePersistent(feed);                
                       
                   } else {

                       //grab image data
                       final String base64Image = project.thumbdata.getValue();
                       byte[] imagedata = Base64.decodeBase64(base64Image);

                       //file name
                       String bucket = "progresssitebucket";
                       String object = UUID.randomUUID() + ".jpg";   
                       //get existing filename for delete
                       String delobject = project.thumbname;

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
                       project.thumbdata = null;

                       // Set servingurl for new image
                       project.setthumbnail(serving_url);

                       //set name for new image
           		    	project.thumbname = object;
           		    	
                       //save project
                       mgr.makePersistent(project);
                                                              
                       //make feed
                       Feed feed = new Feed();
                       feed.ownerid = project.ownerid;
                       feed.ownername = project.ownername;
                       feed.ownerthumb = project.ownerthumb;
                       feed.entitytype = "feed";
                       feed.target = project.entitytype;
                       feed.targetid = project.id;
                       feed.targetowner = project.ownerid;
                       feed.targetaction = "edited";
                       feed.targetthumb = project.thumbnail;
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
               return project;

           } else {

               throw new BadRequestException("Your Not The Owner");
           }

       } else {

           throw new BadRequestException("session.getAttribute(id) is coming back null");
       }
       }

    /**
     * This method removes the entity with primary key id.
     * It uses HTTP DELETE method.
     *
     * @param id the primary key of the entity to be deleted.
     */
	@SuppressWarnings({ "unchecked", "unused" })
    @ApiMethod(name = "removeProject")
	public void removeProject(HttpServletRequest req, @Named("id") Long id) throws BadRequestException, IOException {

		PersistenceManager mgr = getPersistenceManager();	
		Project project = mgr.getObjectById(Project.class, id);		
        HttpSession session = req.getSession(false);
    // get user id from session
       Long uid = (Long) session.getAttribute("id");
        // get projectid from request
       Long eid = project.id;
        // see if logged in
       if (uid != null) {

           Query q = mgr.newQuery(Project.class);
           q.setFilter("ownerid == " + uid + " && id == " + eid);
           Map <String, Long> paramValues = new HashMap <String, Long> ();
           paramValues.put("uid", uid);
           paramValues.put("eid", eid);

           List <Project> results = (List<Project>) q.executeWithMap(paramValues);
           // see if owner
           if (!results.isEmpty()) {

               try {
                   if (!containsProject(project)) {
                       throw new EntityNotFoundException("Object does not exist");
                   }
                                    
                // do delete loop                                              
                String cursorString = null;
                Integer limit = null;
        		Cursor cursor = null;
        		List<Step> execute = null;
      			     			
        			Query query = mgr.newQuery(Step.class);
        			
        			query.setFilter("projectid == projectidParam");
        			query.declareParameters("Long projectidParam");
        			query.setOrdering("created asc");
        			     			
        			execute = (List<Step>) query.execute(eid);
        			cursor = JDOCursorHelper.getCursor(execute);

        			// Tight loop for fetching all entities from datastore and accomodate
        			// for lazy fetch.
        			for (Step obj : execute);
        			
        		CollectionResponse.<Step> builder().setItems(execute).setNextPageToken(cursorString).build();   
        		
        		for(Iterator<Step> i = ((List<Step>) execute).iterator(); i.hasNext(); ) {
        		    Step step = i.next();
        		                	   
		                       //file name
		                       String bucket = "progresssitebucket";
		                       String object = step.thumbname;
		                       //add file name to model
		                       step.thumbname = object;
		                       //prepare file name for delete from Bucket
		                       GcsFilename gcs_filename = new GcsFilename(bucket, object);

		                       //delete from Cloud Storage
		                       GcsServiceFactory.createGcsService().delete(gcs_filename);
		                       
		                       mgr.deletePersistent(step);
		          
        					}

	                       String bucket = "progresssitebucket";
	                       String object = project.thumbname;
	                       //add file name to model
	                       project.thumbname = object;
	                       //prepare file name for delete from Bucket
	                       GcsFilename gcs_filename = new GcsFilename(bucket, object);

	                       //delete from Cloud Storage
	                       GcsServiceFactory.createGcsService().delete(gcs_filename);
	                       
	                       //make feed
	                       Feed feed = new Feed();
	                       feed.ownerid = project.ownerid;
	                       feed.ownername = project.ownername;
	                       feed.ownerthumb = project.ownerthumb;
	                       feed.entitytype = "feed";
	                       feed.target = project.entitytype;
	                       feed.targetid = project.id;
	                       feed.targetowner = project.ownerid;
	                       feed.targetaction = "removed";
	                       feed.targetthumb = project.thumbnail;
	                       feed.created = new Date();
	                       //save feed
	                       mgr.makePersistent(feed);  
	                       
	                       //delete project
	                       mgr.deletePersistent(project);
	          
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
	 * This gets the projects the current user owns
	 * It uses HTTP GET method.
	 *
	 * @param step the entity to be inserted.
	 * @return The inserted entity.
	 */
    @SuppressWarnings({"unchecked", "unused"})
    @ApiMethod(name = "listconsoleProject", path = "listconsoleProject/{id}")
    public CollectionResponse <Project> listconsoleProject(@Nullable @Named("cursor") String cursorString, @Nullable @Named("limit") Integer limit, @Named("id") Long id) {

        PersistenceManager mgr = null;
        Cursor cursor = null;
        List <Project> execute = null;

        try {
            mgr = getPersistenceManager();

            Query query = mgr.newQuery(Project.class);
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


            execute = (List <Project> ) query.execute(uid);
            cursor = JDOCursorHelper.getCursor(execute);
            if (cursor != null)
                cursorString = cursor.toWebSafeString();

            // Tight loop for fetching all entities from datastore and accomodate
            // for lazy fetch.
            for (Project obj: execute)
            ;
        } finally {
            mgr.close();
        }

        return CollectionResponse. <Project> builder().setItems(execute)
            .setNextPageToken(cursorString).build();
    }

    private boolean containsProject(Project project) {
        PersistenceManager mgr = getPersistenceManager();
        boolean contains = true;
        try {
            mgr.getObjectById(Project.class, project.getId());
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

