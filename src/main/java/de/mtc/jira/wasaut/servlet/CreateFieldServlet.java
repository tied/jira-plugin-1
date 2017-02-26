package de.mtc.jira.wasaut.servlet;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CreateFieldServlet extends HttpServlet {

	private final static String context = "/filesearch";
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		String path = req.getParameter("path");
		
		File file = new File(path);
		
		for(File f : file.listFiles()) {
			
		}
		
		
	}
	
	
}
