package mtc.jira.contracts.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UploadServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		getTextArea(resp.getOutputStream());
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
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
