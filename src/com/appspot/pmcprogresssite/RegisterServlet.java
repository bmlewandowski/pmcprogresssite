package com.appspot.pmcprogresssite;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.Date;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.appspot.pmcprogresssite.PMF;
import com.appspot.pmcprogresssite.PasswordHash;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsOutputChannel;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;

public class RegisterServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	/**Used below to determine the size of chucks to read in. Should be > 1kb and < 10MB */
	private static final int BUFFER_SIZE = 2 * 1024 * 1024;
	  
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		// Get incoming email and password from the post header
		String postEmail = req.getParameter("email");
		String postDisplayName = req.getParameter("displayname");
		String postPassword = req.getParameter("password");

		// Handle the nulls...
		if (postEmail == null) {
			postEmail = "";
		}
		if (postDisplayName == null) {
			postDisplayName = "";
		}
		if (postPassword == null) {
			postPassword = "";
		}

		// Start the Validation
		boolean postdataValid = true;

		// Validate Email
		String emailError = "";
		if (postEmail == "") {
			emailError = "Oops! This Can't Be Blank";
			postdataValid = false;
		} else if (postEmail.length() > 255) {
			emailError = "Email must be 255 Charatures or less";
			postdataValid = false;
		} else if (postEmail.length() < 10) {
			emailError = "Email must be more that 10 Charatures";
			postdataValid = false;
		} else {
			// Validate Email Format
			try {
				InternetAddress internetAddress = new InternetAddress(postEmail);
				internetAddress.validate();
			} catch (AddressException e) {
				emailError = "Email should be like someone@somewhere.com";
				postdataValid = false;
			}
		}

		// Validate Display Name
		String displaynameError = "";
		if (postDisplayName == "") {
			displaynameError = "Oops! This Can't Be Blank";
			postdataValid = false;
		} else if (postDisplayName.length() > 50) {
			displaynameError = "Sorry! Must be 50 Charatures or less";
			postdataValid = false;
		} else if (postDisplayName.length() < 5) {
			displaynameError = "Sorry! Must be more that 5 Charatures";
			postdataValid = false;
		} else {
			displaynameError = "";
		}

		// Validate Password
		String passwordError = "";
		if (postPassword == "") {
			passwordError = "Oops! This Can't Be Blank";
			postdataValid = false;
		} else if (postPassword.length() > 20) {
			passwordError = "Sorry! Must be 20 Charatures or less";
			postdataValid = false;
		} else if (postPassword.length() < 5) {
			passwordError = "Sorry! Must be more that 5 Charatures";
			postdataValid = false;
		} else {
			passwordError = "";
		}

		// Check to see if the Email Address is taken
		PersistenceManager pm = PMF.get().getPersistenceManager();

		Query q = pm.newQuery(User.class);
		q.setFilter("email == lastNameParam");
		q.declareParameters("String lastNameParam");

		@SuppressWarnings("unchecked")
		List<User> results = (List<User>) q.execute(postEmail);

		if (!results.isEmpty()) {
			emailError = "Doh! This Email is already taken";
			postdataValid = false;
			// String emailCheck = results.get(0).email;
		}

		// Check to see if the Display Name is taken
		q.setFilter("displayname == displaynameParam");
		q.declareParameters("String displaynameParam");

		@SuppressWarnings("unchecked")
		List<User> displaynameResults = (List<User>) q.execute(postDisplayName);
		q.closeAll();

		if (!displaynameResults.isEmpty()) {
			displaynameError = "Bummer! This Display Name is taken";
			postdataValid = false;
			// String emailCheck = results.get(0).email;
		}

		if (postdataValid == true) {

			String hashedpass = "";
			try {
				hashedpass = PasswordHash.createHash(postPassword);
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidKeySpecException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// Map the incoming parameters to a new user object
			User user = new User();
			user.email = postEmail;
			user.displayname = postDisplayName;
			user.password = hashedpass;
			user.entitytype = "user";
			user.created = new Date();

			// Fire up the Persistence Manager and save the user to the data store
			PersistenceManager mgr = PMF.get().getPersistenceManager();
			mgr.makePersistent(user);
			
			//Make the default folder
			Folder folder = new Folder();
			folder.title = "Default";
			folder.entitytype = "folder";
			folder.ownerid = user.id;
			folder.ownername = user.displayname;
			folder.ownerthumb = user.thumbnail;
			folder.systemfolder = true;
			folder.defaultfolder = true;
			folder.created = new Date();
			folder.modified = new Date();

			mgr.makePersistent(folder);
			
            //make feed
            Feed feed = new Feed();
            feed.ownerid = user.id;
            feed.ownername = user.displayname;
            feed.ownerthumb = user.id + "_TN.jpg";
            feed.entitytype = "feed";
            feed.target = user.entitytype;
            feed.targetid = user.id;
            feed.targetowner = user.id;
            feed.targetaction = "created";
            feed.created = new Date();
            //save feed
            mgr.makePersistent(feed);   			
			
			
            //File Name
            	String userid = user.id.toString();
            	String bucket = "progresssiteusers";
            	String portraitname = userid + ".jpg";
                    
    		//Get Input Stream from public default user image           	
            	String defaultuserurl = "http://storage.googleapis.com/progresssiteusers/defaultuser.jpg";
    		    URL url = new URL(defaultuserurl);
    		    InputStream instream = url.openStream();
    		    

			//Enable GcsService
    			GcsService gcsService = GcsServiceFactory.createGcsService();	
    		
            //Prepare File Name for Write to Bucket
            	GcsFilename gcs_filename = new GcsFilename(bucket, portraitname);
              		 
            //File Options
            	GcsFileOptions.Builder options_builder = new GcsFileOptions.Builder();
            	options_builder = options_builder.mimeType("image/jpeg");
                //commented out to leave bucket default
                //options_builder = options_builder.acl("public-read");
            	GcsFileOptions options = options_builder.build();
            //Write from input stream to Data Store
    			GcsOutputChannel outputChannel = gcsService.createOrReplace(gcs_filename, options);
    			copyImage(instream, Channels.newOutputStream(outputChannel));
   		 
            //Get serving url
            	String gs_blob_key = "/gs/" + bucket + "/" + portraitname;
            	BlobKey blob_key = BlobstoreServiceFactory.getBlobstoreService().createGsBlobKey(gs_blob_key);

            	ServingUrlOptions serving_options = ServingUrlOptions.Builder.withBlobKey(blob_key).secureUrl(true);
            	String serving_url = ImagesServiceFactory.getImagesService().getServingUrl(serving_options);
            	
           // Save smaller version
        		//Get Input Stream from public default user image           	
            	String newthumburl = serving_url + "=s100";
    		    URL thurl = new URL(newthumburl);
    		    InputStream tnstream = thurl.openStream();
            	
                //File Name
            	String thumbnailname = userid + "_TN.jpg";
            	
            	GcsFilename gcs_filename_th = new GcsFilename(bucket, thumbnailname);           	
                //Write from input stream to Data Store
        			GcsOutputChannel thoutputChannel = gcsService.createOrReplace(gcs_filename_th, options);
        			copyImage(tnstream, Channels.newOutputStream(thoutputChannel));            	

            //Remove Thumbnail Item for Save in Datastore
        		user.setportrait("http://storage.googleapis.com/progresssiteusers/" + portraitname);	
            	user.setthumbnail("http://storage.googleapis.com/progresssiteusers/" + thumbnailname);	
						
			// Send Back that the User was created
			resp.setContentType("application/json");
				//resp.getWriter().println("{\"status\":\"User Created\"}");
			
			resp.getWriter().println(user.id);

		} else {

			// Create and Return the Validation Error Object
			String generalError = "";

			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			resp.setContentType("application/json");
			resp.getWriter().println(
					"{" + "\"general\":\"" + generalError + "\", "
							+ "\"email\":\"" + emailError + "\", "
							+ "\"displayname\":\"" + displaynameError + "\", "
							+ "\"password\":\"" + passwordError + "\"" + "}");
		}
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

