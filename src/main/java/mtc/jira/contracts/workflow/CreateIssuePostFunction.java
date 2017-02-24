package mtc.jira.contracts.workflow;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;

import mtc.jira.contracts.CSVEntry;
import mtc.jira.contracts.CSVParser;
import mtc.jira.contracts.ProjectHelper;

public class CreateIssuePostFunction extends AbstractJiraFunctionProvider {

	private static Logger log = LoggerFactory.getLogger(CreateIssuePostFunction.class);

	@SuppressWarnings({ "rawtypes" })
	@Override
	public void execute(Map transientVars, Map args, PropertySet ps) throws WorkflowException {
		log.debug("Executing create post function");
		MutableIssue issue = getIssue(transientVars);
		
		try {
			Map<String, CSVEntry> data = CSVParser.getDataFromFile();
			ProjectHelper helper = new ProjectHelper();
			helper.initFields(issue, data);
			return;
		} catch (IOException e) {
			log.error("Error reading data.csv", e);
			throw new WorkflowException(e);
		}
	}
}
