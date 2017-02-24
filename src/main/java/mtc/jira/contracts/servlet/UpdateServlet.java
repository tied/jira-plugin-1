package mtc.jira.contracts.servlet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.opensymphony.workflow.WorkflowException;

import mtc.jira.contracts.CSVParser;
import mtc.jira.contracts.ProjectHelper;

public class UpdateServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String csv = req.getParameter("csv");

		Exception ex = null;
		ServletOutputStream out = resp.getOutputStream();

		File temp = File.createTempFile("contract-data", ".tmp");

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
		Map<String, List<String>> data = CSVParser.getDataFromFile();
		try {
			List<Issue> issues = ProjectHelper.getIssuesForProject("PLUG");
			for (Issue issue : issues) {
				out.println("<br><b>Updating Issue " + issue.getKey() + "</b>");
				try {
					String result = ProjectHelper.fillCustomFields(issue, data);
					out.println("<br>" + result + "<br>");
				} catch (WorkflowException e) {
					ex = e;
					out.println("<br>Exception: " + e.getMessage());
				}
			}
		} catch (SearchException e) {
			out.println("<br>Exception: " + e.getMessage());
			e.printStackTrace();
		}
		
		out.println("<p><input type=\"submit\"></p>");
	}

	private void getTextArea(ServletOutputStream out) throws IOException {

		out.println("<html>");
		out.println("<body>");

		out.println("<h1>Upload contract scheme</h1>");
		out.println("<p>Copy-and-paste the valid csv data to the text box below</p>");

		out.println("<form action=\"update\" id=\"usrform\" method=\"post\">");
		out.println("<textarea rows=\"50\" cols=\"150\" name=\"csv\" form=\"usrform\" ></textarea>");
		out.println("<p><input type=\"submit\"></p>");
		out.println("</form>");

		out.println("</body>");
		out.println("</html>");

	}

}
