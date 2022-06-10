package com.appspot.pmcprogresssite;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.appspot.pmcprogresssite.PMF;
import com.appspot.pmcprogresssite.User;

public class AccountLoginServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		// Adjust the header to enable CORS
		resp.addHeader("Access-Control-Allow-Origin", "https://www.progresssite.com");
		resp.addHeader("Access-Control-Allow-Credentials", "true");
		//resp.setContentType("application/json");

		//Get incoming session name from the post header and put inside a string called sessionName
		String postEmail = req.getParameter("email");
		String postPassword = req.getParameter("password");
		
		// Handle the nulls...
		if (postEmail == null) {
			postEmail = "";
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
		} else if (postEmail.length() < 5) {
			emailError = "Email must be more that 5 Charatures";
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

		// Validate Password
		String passwordError = "";
		if (postPassword == "") {
			passwordError = "Oops! This Can't Be Blank";
			postdataValid = false;
		} else if (postPassword.length() > 250) {
			passwordError = "Sorry! Must be 250 Charatures or less";
			postdataValid = false;
		} else if (postPassword.length() < 4) {
			passwordError = "Sorry! Must be more that 4 Charatures";
			postdataValid = false;
		} else {
			passwordError = "";
		}
		
		
		if (postdataValid == true) {
		
			// Create the Persistance Manager to use to access the Datastore
			PersistenceManager pm = PMF.get().getPersistenceManager();
	
			// Create the Query Object
			Query q = pm.newQuery(User.class);
			q.setFilter("email == emailParam");
			q.declareParameters("String emailParam");
	
			// Use the Query Object to Access to Populate a list of User Objects
			// (where the posted email = the email on file)
			@SuppressWarnings("unchecked")
			List<User> userObj = (List<User>) q.execute(postEmail);
	
			// If you find the Email Account on file...
			if (!userObj.isEmpty()) {
	
				//Check the Password....
				try {
					String storedpass = userObj.get(0).password;
					boolean passright = PasswordHash.validatePassword(postPassword,storedpass);
					if (passright == true) {
								
							// Create a session on the server that has the users account (userID) in it.
							HttpSession mySession = req.getSession();
							mySession.setAttribute("id", userObj.get(0).id);

							// Return the Login Object to the Browser.
							String jsonStr = "{\"status\": \"loggedin\", \"id\":\""
									+ userObj.get(0).id + "\", \"email\":\""
									+ userObj.get(0).email + "\", \"displayname\":\""
									+ userObj.get(0).displayname + "\", \"Role\": 1}";
							resp.setContentType("application/json");
							resp.getWriter().println(jsonStr);
					}
					
					else {
						// Return a Failed Login
						resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						resp.setContentType("application/json");
						resp.getWriter().println("{\"general\":\"Sorry! Password is incorrect...\"}");
					}
					
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvalidKeySpecException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		
			else {
				// Return a Failed Login
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				resp.setContentType("application/json");
				resp.getWriter().println("{\"general\":\"Sorry! Email Address not found.\"}");
			}
		
		} 
	else {

		// Create and Return the Validation Error Object
		String generalError = "";

		resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		resp.setContentType("application/json");
		resp.getWriter().println(
				"{" + "\"general\":\"" + generalError + "\", "
						+ "\"email\":\"" + emailError + "\", "
						+ "\"password\":\"" + passwordError + "\"" + "}");
	}

	}

}