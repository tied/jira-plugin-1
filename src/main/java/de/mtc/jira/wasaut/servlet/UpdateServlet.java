package de.mtc.jira.wasaut.servlet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.component.ComponentAccessor;
import com.opensymphony.workflow.WorkflowException;

import de.mtc.jira.wasaut.CSVEntry;
import de.mtc.jira.wasaut.CSVParser;
import de.mtc.jira.wasaut.Message;
import de.mtc.jira.wasaut.MessageHandler;
import de.mtc.jira.wasaut.PluginCache;
import de.mtc.jira.wasaut.ProjectHelper;

public class UpdateServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final static String UPDATE_RESULT_KEY = "___result___";
	private final static String FORM_DATA_KEY = "___formData___";
	private final static String INPUT_DATA_KEY = "___input____";

	private final static Logger log = LoggerFactory.getLogger(UpdateServlet.class);

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		ServletOutputStream out = resp.getOutputStream();

		if (!ComponentAccessor.getJiraAuthenticationContext().isLoggedInUser()) {
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			out.println("<p>Please log in to jira.mtc.berlin first</p>");
			return;
		}

		String formData = req.getParameter("csv");
		String jql = req.getParameter("jql");

		Map<String, CSVEntry> inputData = CSVParser.readDataFromString(formData);

		out.println("<html>");
		out.println("<body>");

		Map<String, CSVEntry> oldData = null;
		try {
			oldData = CSVParser.getDataFromFile();
		} catch (FileNotFoundException e) {
			out.println("<p>Couldn't find any stored data, this seems to be an initial commit.</p>");
		}

		if (jql != null && !jql.equals(PluginCache.getJqlQuery())) {
			PluginCache.setJqlQuery(jql);
		}

		out.println("<p>Query: " + PluginCache.getJqlQuery() + "</p>");
		out.println("<p>Found stored data: " + CSVParser.getDataFile().getAbsolutePath() + "</p>");

		PluginCache.setData(inputData);

		ProjectHelper helper = new ProjectHelper();

		try {
			helper.getProjectUpdates(inputData);
		} catch (WorkflowException e) {
			out.println(e.getMessage());
		}

		out.println("<h1>Result</h1>");
		printMessages(helper, out);

		HttpSession session = req.getSession();

		session.setAttribute(UPDATE_RESULT_KEY, helper);
		session.setAttribute(FORM_DATA_KEY, formData);
		session.setAttribute(INPUT_DATA_KEY, inputData);

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
		HttpSession session = req.getSession();
		Object results = session.getAttribute(UPDATE_RESULT_KEY);
		ServletOutputStream out = resp.getOutputStream();

		out.println("<html>");
		out.println("<body>");

		List<ProjectHelper.IssueUpdate> updates = null;

		try {
			updates = ((ProjectHelper) results).getUpdates();
		} catch (Exception e) {
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			out.println("<p>No update results for this session</p>");
			return;
		}
		
		out.println("<h2>Commited " + updates.size() + " issues</h2>");

		try {
			for (ProjectHelper.IssueUpdate update : updates) {
				update.publish();
				out.println("<p>" + update.getHTMLString() + "</p>");
			}
		} catch (Exception e) {
			out.println("<p>Error " + e.getMessage() + "</p>");
			e.printStackTrace();
		}

		try {
			PluginCache.setData((Map<String, CSVEntry>) session.getAttribute(INPUT_DATA_KEY));
			out.println("<p>Cached data</p>");
		} catch (Exception e) {
			out.println("<p style=\"color:red;\">Error caching data: " + e.getMessage() + "</p>");
			log.error("Failed to cache data ", e);
		}

		try {
			CSVParser.writeCSVFile((String) session.getAttribute(FORM_DATA_KEY));
		} catch (Exception e) {
			out.println("<p style=\"color:red;\">Error caching data: " + e.getMessage() + "</p>");
			log.error("Failed " + e);
		}

		out.println("</body>");
		out.println("</html>");
	}

	private void printMessages(ProjectHelper helper, ServletOutputStream out) throws IOException {

		MessageHandler handler = helper.getMessageHandler();

		if (!handler.getErrors().isEmpty()) {
			out.println("<h3>Errors</h3>");
			for (Message msg : handler.getErrors()) {
				out.println("<p>" + msg.toString(true) + "</p>");
			}
		}

		if (!handler.getWarnings().isEmpty()) {
			out.println("<h3>Warnings</h3>");
			for (Message msg : handler.getWarnings()) {
				out.println("<p>" + msg.toString(true) + "</p>");
			}
		}

		if (!handler.getInfos().isEmpty()) {
			out.println("<h3>Infos</h3>");
			for (Message msg : handler.getInfos()) {
				out.println("<p>" + msg.toString(true) + "</p>");
			}
		}
	}
}
