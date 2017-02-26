package de.mtc.jira.wasaut.servlet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.opensymphony.workflow.WorkflowException;

import de.mtc.jira.wasaut.CSVEntry;
import de.mtc.jira.wasaut.CSVParser;
import de.mtc.jira.wasaut.Message;
import de.mtc.jira.wasaut.MessageHandler;
import de.mtc.jira.wasaut.PluginCache;
import de.mtc.jira.wasaut.ProjectHelper;

public class UpdateServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private String UPDATE_RESULT_KEY = "___result___";

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		String csv = req.getParameter("csv");

		Map<String, CSVEntry> data = CSVParser.readDataFromString(csv);
		
		Exception ex = null;
		ServletOutputStream out = resp.getOutputStream();

		File temp = File.createTempFile("contract-data", ".tmp");

		out.println("<html>");
		out.println("<body>");
		out.println("<br> " + temp.getAbsolutePath() + " " + temp.canWrite());
		CSVParser.setCSVPath(temp.getAbsolutePath());

		try (FileWriter writer = new FileWriter(temp)) {
			writer.write(csv);
		} catch (Exception e) {
			ex = e;
			out.println("<br>Failed to write on " + temp.getAbsolutePath());
			out.println("<br>Exception: " + e.getMessage());
			for (StackTraceElement el : e.getStackTrace()) {
				out.println("<br>" + el.getFileName() + "." + el.getMethodName() + "." + el.getLineNumber());
				return;
			}
		}
		if (ex == null) {
			out.println("<br>Wrote to file " + temp.getAbsolutePath());
		}
		ex = null;


		PluginCache.setData(data);
		
		ProjectHelper helper = new ProjectHelper();

		try {
			helper.getProjectUpdates(data);
		} catch (WorkflowException e) {
			out.println(e.getMessage());
		}

		out.println("<h1>Result</h1>");
		printMessages(helper, out);
		
		req.getSession().setAttribute(UPDATE_RESULT_KEY, helper);

		for (ProjectHelper.IssueUpdate update : helper.getUpdates()) {
			out.println("<p>" + update.getHTMLString() + "</p>");
		}

		out.println("<form action=\"update\" method=\"get\">");
		out.println("<p><input type=\"submit\" value=\"Commit Changes\"></p>");
		out.println("</form>");

		out.println("</body>");
		out.println("</html>");
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Object results = req.getSession().getAttribute(UPDATE_RESULT_KEY);
		System.out.println("Result Key " + results);
		ServletOutputStream out = resp.getOutputStream();

		out.println("<html>");
		out.println("<body>");
		out.println(results.getClass().getName());

		try {
			for (ProjectHelper.IssueUpdate update : ((ProjectHelper) results).getUpdates()) {
				out.println("<p>" + update.getHTMLString() + "</p>");
				update.publish();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		out.println("</body>");
		out.println("</html>");
	}

	private void printMessages(ProjectHelper helper, ServletOutputStream out) throws IOException {
		
		MessageHandler handler = helper.getMessageHandler();
		
		out.println("<h3>Errors</h3>");

		if(handler.getErrors().isEmpty()) {
			out.println("No errors");
		} else {
			for (Message msg : handler.getErrors()) {
			out.println("<p>" + msg.toString(true) + "</p>");
			}
		}
		
		out.println("<h3>Warnings</h3>");
		if(handler.getWarnings().isEmpty()) {
			out.println("No warnings");
		} else {
			for (Message msg : handler.getWarnings()) {
			out.println("<p>" + msg.toString(true) + "</p>");
			}
		}
		
		out.println("<h3>Infos</h3>");
		if(handler.getInfos().isEmpty()) {
			out.println("No infos");
		} else {
			for (Message msg : handler.getInfos()) {
			out.println("<p>" + msg.toString(true) + "</p>");
			}
		}
	}
}
