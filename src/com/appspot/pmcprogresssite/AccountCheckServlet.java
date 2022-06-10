package com.appspot.pmcprogresssite;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class AccountCheckServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		
		//Adjust the header to enable CORS
		resp.addHeader("Access-Control-Allow-Origin", "https://www.progresssite.com");
		resp.addHeader("Access-Control-Allow-Credentials", "true");

        HttpSession session = req.getSession();
        
		// get user id from session
        Long userID = (Long) session.getAttribute("id");
		
        
		// Return the Login Object to the Browser.
		resp.setContentType("application/json");
		resp.getWriter().println("{" + "\"id from session\":\"" + userID + "\"}");
        
        
		//resp.setContentType("text/plain");
		//resp.getWriter().println("fuuuu");
		
	}	
}
