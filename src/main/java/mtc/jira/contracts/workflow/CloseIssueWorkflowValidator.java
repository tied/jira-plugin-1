package mtc.jira.contracts.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.atlassian.jira.issue.Issue;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.Validator;
import com.opensymphony.workflow.InvalidInputException;

import java.util.Map;

public class CloseIssueWorkflowValidator implements Validator {
	private static final Logger logger = LoggerFactory.getLogger(CloseIssueWorkflowValidator.class);
	@SuppressWarnings("rawtypes")
	@Override
	public void validate(Map transientVars, Map args, PropertySet ps) throws InvalidInputException {
		logger.info("## CloseIssueWorkflowValidator.validate");
		Issue issue = (Issue) transientVars.get("issue");
		// The issue must have a fixVersion otherwise you cannot close it
		if (null == issue.getFixVersions() || issue.getFixVersions().size() == 0) {
			throw new InvalidInputException("Issue must have a fix version");
		}
	}
}