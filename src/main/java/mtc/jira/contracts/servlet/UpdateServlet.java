package mtc.jira.contracts.servlet;

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

import mtc.jira.contracts.CSVEntry;
import mtc.jira.contracts.CSVParser;
import mtc.jira.contracts.MessageHandler;
import mtc.jira.contracts.ProjectHelper;

public class UpdateServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private String UPDATE_RESULT_KEY = "___result___";

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		String csv = req.getParameter("csv");

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

		Map<String, CSVEntry> data = CSVParser.getDataFromFile();
		ProjectHelper helper = new ProjectHelper();

		try {
			helper.getProjectUpdates(data);
		} catch (WorkflowException e) {
			out.println(e.getMessage());
		}

		MessageHandler handler = helper.getMessageHandler();
		out.println("<h1>Errors</h1>");
		for (String msg : handler.getErrors()) {
			out.println(msg);
		}
		out.println("<h1>Warnings</h1>");
		for (String msg : handler.getErrors()) {
			out.println(msg);
		}
		out.println("<h1>Infos</h1>");
		for (String msg : handler.getErrors()) {
			out.println(msg);
		}

		out.println("<h1>Result</h1>");

		req.getSession().setAttribute(UPDATE_RESULT_KEY, helper);

		for (ProjectHelper.IssueUpdate update : helper.getUpdates()) {
			out.println("<p>" + update.getHTMLString() + "</p>");
		}

		out.println("<form action=\"update\" method=\"get\">");
		out.println("<p><input type=\"submit\"></p>");
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
}
