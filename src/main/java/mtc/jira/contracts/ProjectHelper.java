package mtc.jira.contracts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.ModifiedValue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.user.ApplicationUser;
import com.opensymphony.workflow.WorkflowException;

public class ProjectHelper {

	private static final Logger log = LoggerFactory.getLogger(CSVParser.class);
	private List<IssueUpdate> updates = new ArrayList<>();
	
	private MessageHandler messageHandler;

	public ProjectHelper() {
		this.messageHandler = new MessageHandler();
	}

	public void getProjectUpdates(Map<String, CSVEntry> data) throws WorkflowException {
		List<Issue> issues = getIssuesForProject(new String[] { "PLUG" });		
		for (Issue issue : issues) {
			
			log.debug("Checking " + issue.getKey() );
			
			try {
				checkUpdate(issue, data);
			} catch(Exception e) {
				log.debug(e.getMessage());
				e.printStackTrace();
			}
		}
	}

	public void checkUpdate(Issue issue, Map<String, CSVEntry> data) throws WorkflowException {
		log.debug("Checking updates for issue: " + issue.getKey());
		List<CustomField> customFields = ComponentAccessor.getCustomFieldManager().getCustomFieldObjects(issue);
		customFields.stream().map(t -> t.getName()).forEach(t -> log.debug(t));
		Optional<CustomField> opField = customFields.stream().filter(t -> t.getName().equals(PluginConstants.CF_FIELDS_NAMES[1]))
				.findFirst();
		if (!opField.isPresent()) {
			throw new WorkflowException("CF_TYPE field is missing");
		}
		CustomField typeField = opField.get();
		String type = (String) issue.getCustomFieldValue(typeField);
		if (type == null || type.isEmpty()) {
			type = data.keySet().iterator().next();			
			// throw new WorkflowException("Empty type field");
		}
		CSVEntry entries = data.get(type);
		if (entries == null) {
			String msg = "Unknown project / contracts key: " + type;
			throw new WorkflowException(msg);
		}

		for(CustomField customField : customFields) {
			String newValue = entries.get(customField.getName());
			if(newValue == null) {
				continue;
			}
			Object oldValue = customField.getValue(issue);
			if(!newValue.equals(oldValue)) {
				updates.add(new IssueUpdate(issue, customField, oldValue, newValue));
			}
		}
	}

	public List<Issue> getIssuesForProject(String[] projectKeys) throws WorkflowException {

		log.debug("Getting issues for project " + projectKeys);

		List<Issue> result = new ArrayList<>();
		SearchService searchService = ComponentAccessor.getComponentOfType(SearchService.class);
		ProjectManager projectManager = ComponentAccessor.getProjectManager();
		ApplicationUser currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();

		for (String projectKey : projectKeys) {
			result.addAll(getIssueByProjectKey(projectKey, searchService, projectManager, currentUser));
		}

		messageHandler.info("Found " + result.size() + " issues for project(s) " + Arrays.toString(projectKeys));
		
		log.debug("Issues to check: " + result);
		
		return result;
	}

	private List<Issue> getIssueByProjectKey(String projectKey, SearchService searchService,
			ProjectManager projectManager, ApplicationUser currentUser) throws WorkflowException {
		List<Issue> result = new ArrayList<>();
		Project project = projectManager.getProjectByCurrentKey(projectKey);

		Collection<IssueType> issueTypes = project.getIssueTypes();

		for (IssueType issueType : issueTypes) {
			String jqlQuery = "project = '" + project.getKey() + "' and status != 'Closed' and issuetype='"
					+ issueType.getName() + "'";
			final SearchService.ParseResult parseResult = searchService.parseQuery(currentUser, jqlQuery);

			log.debug("Executing jql Query: " + jqlQuery);

			if (parseResult.isValid()) {

				SearchResults results;
				try {
					results = searchService.search(currentUser, parseResult.getQuery(),
							new com.atlassian.jira.web.bean.PagerFilter<>());
				} catch (SearchException e) {
					throw new WorkflowException("Unable to get search results for project " + projectKey, e);
				}
				final List<Issue> issues = results.getIssues();
				log.debug("Result " + issues.stream().map(t -> t.getKey()).collect(Collectors.joining(",")));
				result.addAll(issues);
			} else {
				log.debug("Search result not valid");
			}
		}

		return result;
	}

	
	public void initFields(Issue issue, Map<String,CSVEntry> data) throws WorkflowException {
		List<CustomField> customFields = ComponentAccessor.getCustomFieldManager().getCustomFieldObjects(issue);
		Optional<CustomField> opField = customFields.stream().filter(t -> t.getName().equals(PluginConstants.CF_FIELDS_NAMES[1]))
				.findFirst();
		if (!opField.isPresent()) {
			throw new WorkflowException("CF_TYPE field is missing");
		}
		CustomField typeField = opField.get();
		String type = (String) issue.getCustomFieldValue(typeField);
		CSVEntry csvEntry = data.get(type);
		if(csvEntry == null) {
			throw new WorkflowException("No entries for this value: " + type);
		}
		
		for(CustomField customField : customFields) {
			String newValue = csvEntry.get(customField.getName());
			if(newValue == null) {
				continue;
			}
			Object oldValue = customField.getValue(issue);
			if(!newValue.equals(oldValue)) {
				new IssueUpdate(issue, customField, oldValue, newValue).publish();
			}
		}
	}
	
	public MessageHandler getMessageHandler() {
		return messageHandler;
	}
	
	
	public List<IssueUpdate> getUpdates() {
		return updates;
	}
	
	public class IssueUpdate {
		
		private Object oldValue;
		private String newValue;
		private CustomField customField;
		private Issue issue;
		
		public IssueUpdate(Issue issue, CustomField customField, Object oldValue, String newValue) {
			this.issue = issue;
			this.customField = customField;
			this.oldValue = oldValue;
			this.newValue = newValue;
		}
		
		public void publish() {
			if (issue instanceof MutableIssue) {
				((MutableIssue) issue).setCustomFieldValue(customField, newValue);
			} else {
				customField.updateValue(null, issue, new ModifiedValue<Object>(oldValue, newValue),
						new DefaultIssueChangeHolder());
			}
			messageHandler.info("Updating " + issue.getKey() + " from " + oldValue + " to " + newValue);
			log.debug("Updated field value " + customField.getValue(issue) + " " + newValue);
		}
		
		public String getHTMLString() {
			StringBuilder sb = new StringBuilder();
			String url = ComponentAccessor.getApplicationProperties().getString("jira.baseurl") + "/browse/" + issue.getKey();
			
			sb.append("<a href=\"" + url + "\">" + issue.getKey() + "<a>: ");
			sb.append("Field " + customField.getName() + " will be changed from ");
			sb.append(oldValue + " to " + newValue);
			
			return sb.toString();
		}	
	}
}
