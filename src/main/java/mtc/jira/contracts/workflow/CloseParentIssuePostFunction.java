package mtc.jira.contracts.workflow;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.workflow.JiraWorkflow;
import com.atlassian.jira.workflow.WorkflowManager;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;
import com.opensymphony.workflow.loader.ActionDescriptor;

/*
This is the post-function class that gets executed at the end of the transition.
Any parameters that were saved in your factory class will be available in the transientVars Map.
 */
@Scanned
public class CloseParentIssuePostFunction extends AbstractJiraFunctionProvider {

	private static final Logger log = LoggerFactory.getLogger(CloseParentIssuePostFunction.class);

	private final WorkflowManager workflowManager;
	private final SubTaskManager subTaskManager;
	private final JiraAuthenticationContext authenticationContext;
	
	private final Status closedStatus;

	@Inject
	public CloseParentIssuePostFunction(@ComponentImport ConstantsManager constantsManager,
			@ComponentImport WorkflowManager workflowManager, @ComponentImport SubTaskManager subTaskManager,
			@ComponentImport JiraAuthenticationContext authenticationContext) {
		super();
		this.workflowManager = workflowManager;
		this.subTaskManager = subTaskManager;
		this.authenticationContext = authenticationContext;
		closedStatus = constantsManager.getStatus(new Integer(IssueFieldConstants.CLOSED_STATUS_ID).toString());
	}

	// Move the specified issue to the 'CLOSED' status.
	private void closeIssue(Issue issue) throws WorkflowException {
		Status currentStatus = issue.getStatus();
		JiraWorkflow workflow = workflowManager.getWorkflow(issue);
		@SuppressWarnings("unchecked")
		List<ActionDescriptor> actions = workflow.getLinkedStep(currentStatus).getActions();
		// look for the closed transition
		ActionDescriptor closeAction = null;
		for (ActionDescriptor descriptor : actions) {
			if (descriptor.getUnconditionalResult().getStatus().equals(closedStatus.getName())) {
				closeAction = descriptor;
				break;
			}
		}
		if (closeAction != null) {
			ApplicationUser currentUser = authenticationContext.getLoggedInUser();
			IssueService issueService = ComponentAccessor.getIssueService();
			IssueInputParameters parameters = issueService.newIssueInputParameters();
			parameters.setRetainExistingValuesWhenParameterNotProvided(true);
			IssueService.TransitionValidationResult validationResult = issueService.validateTransition(currentUser,
					issue.getId(), closeAction.getId(), parameters);
			IssueService.IssueResult result = issueService.transition(currentUser, validationResult);
			log.info("Closed parent issue " + issue.getKey() + " [" + result + "]");
			log.info("Details:");
			result.getErrorCollection().getErrorMessages().stream().forEach(t -> log.info(t));
		} else {
			log.info("No appropriate close action found");
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void execute(Map transientVars, Map args, PropertySet ps) throws WorkflowException {

		log.info("Executing post function");

		// Retrieve the sub-task
		MutableIssue subTask = getIssue(transientVars);

		System.out.println(ComponentAccessor.getCustomFieldManager().getCustomFieldObjects());
		
		CustomField cField;
		for(CustomField field : ComponentAccessor.getCustomFieldManager().getCustomFieldObjects()) {
			if(field.getName().equals("My Field")) {
				cField = field;
				subTask.setCustomFieldValue(cField, new BigDecimal(9999));
				break;
			}
		}
		
		ComponentAccessor.getCustomFieldManager().getCustomFieldObjects().stream().filter(t -> t.getName().equals("My Field"));
		
		
		// Retrieve the parent issue
		MutableIssue parentIssue = ComponentAccessor.getIssueManager().getIssueObject(subTask.getParentId());
		
		// Ensure that the parent issue is not already closed
		if (parentIssue == null
				|| IssueFieldConstants.CLOSED_STATUS_ID == Integer.parseInt(parentIssue.getStatus().getId())) {
			return;
		}

		long unclosed = subTaskManager.getSubTaskObjects(parentIssue).stream()
				.filter(t -> !subTask.getId().equals(t.getId())
						&& IssueFieldConstants.CLOSED_STATUS_ID != Integer.parseInt(t.getStatus().getId()))
				.count();

		if (unclosed == 0) {
			// All sub-tasks are now closed - close the parent issue
			try {
				closeIssue(parentIssue);
			} catch (WorkflowException e) {
				log.error("Error occurred while closing the issue: " + parentIssue.getKey() + ": " + e, e);
				e.printStackTrace();
			}
		}
	}
}