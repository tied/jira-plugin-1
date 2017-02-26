package mtc.jira.contracts.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;

public class CreateFieldServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		CustomFieldManager manager = ComponentAccessor.getCustomFieldManager();
		// manager.createCustomField("TEST_FIELD", CustomFieldType., arg2, arg3, arg4, arg5)
	}
	
	
}
