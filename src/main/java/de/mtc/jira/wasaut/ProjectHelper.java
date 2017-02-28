package de.mtc.jira.wasaut;

import java.util.ArrayList;
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
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder;
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
		ApplicationUser currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
		List<Issue> issues = getRelevantIssues(currentUser);
		for (Issue issue : issues) {
			log.debug("Checking " + issue.getKey());
			try {
				checkUpdate(issue, data);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	public void checkUpdate(Issue issue, Map<String, CSVEntry> data) throws WorkflowException {
		log.debug("Checking updates for issue: " + issue.getKey());
		List<CustomField> customFields = ComponentAccessor.getCustomFieldManager().getCustomFieldObjects(issue);
		customFields.stream().map(t -> t.getName()).forEach(t -> log.debug(t));
		Optional<CustomField> opField = customFields.stream()
				.filter(t -> t.getName().equals(PluginConstants.CF_FIELDS_NAMES[1])).findFirst();
		if (!opField.isPresent()) {
			Message message = new Message(issue, "Key field is missing");
			messageHandler.error(message);
			throw new WorkflowException(message.toString(false));
		}
		CustomField typeField = opField.get();
		String type = (String) issue.getCustomFieldValue(typeField);
		if (type == null || type.isEmpty()) {
			Message message = new Message(issue, "Project/Contract key is missing");
			messageHandler.error(message);
			throw new WorkflowException(message.toString(false));
		}
		CSVEntry entries = data.get(type);
		if (entries == null) {
			Message message = new Message(issue, "Unknown Project/Contract type: " + type);
			messageHandler.error(message);
			throw new WorkflowException(message.toString(false));
		}
		for (CustomField customField : customFields) {
			String newValue = entries.get(customField.getName());
			log.debug("### Issue: " + issue.getKey() + " " + customField.getName() + " "
					+ issue.getCustomFieldValue(customField) + " " + newValue);
			if (newValue == null) {
				continue;
			}
			Object oldValue = customField.getValue(issue);
			if (!newValue.equals(oldValue)) {
				updates.add(new IssueUpdate(issue, customField, oldValue, newValue));
			}
		}
	}


	
	private List<Issue> getRelevantIssues(ApplicationUser currentUser) throws WorkflowException {
		String jqlQuery = PluginCache.getJqlQuery();
		List<Issue> result = new ArrayList<>();
		SearchService searchService = ComponentAccessor.getComponentOfType(SearchService.class);
		SearchService.ParseResult parseResult = searchService.parseQuery(currentUser, jqlQuery);
		if (parseResult.isValid()) {
			SearchResults results = null;
			try {
				results = searchService.search(currentUser, parseResult.getQuery(),
						new com.atlassian.jira.web.bean.PagerFilter<>());
			} catch (SearchException e) {
				Message message = new Message(null, "JQL Error for " + parseResult.getQuery());
				messageHandler.error(message);
				log.debug(message.toString(false));
				log.debug(e.getMessage());
			}
			if (results != null) {
				final List<Issue> issues = results.getIssues();
				result.addAll(issues);
				String issueList = issues.stream().map(t -> t.getKey()).collect(Collectors.joining(","));
				log.debug("Result " + issueList);
			}
		} else {
			log.debug("Search result not valid " + parseResult.getErrors());
		}
		return result;
		
	}
	
	
	private void setFieldValues(Issue issue, Map<String, CSVEntry> data, boolean update) throws WorkflowException {
		log.debug(new Message(issue, "Initializing custom fields").toString(false));
		List<CustomField> customFields = ComponentAccessor.getCustomFieldManager().getCustomFieldObjects(issue);
		Optional<CustomField> opField = customFields.stream()
				.filter(t -> t.getName().equals(PluginConstants.CF_FIELDS_NAMES[1])).findFirst();
		if (!opField.isPresent()) {
			Message message = new Message(issue, "Key custom field is missing");
			throw new WorkflowException(message.toString(false));
		}
		CustomField typeField = opField.get();
		String type = (String) issue.getCustomFieldValue(typeField);
		CSVEntry csvEntry = data.get(type);
		if (csvEntry == null) {
			Message message = new Message(issue, "Unknown value in key field " + type);
			throw new WorkflowException(message.toString(false));
		}
		for (CustomField customField : customFields) {
			String newValue = csvEntry.get(customField.getName());
			if (newValue == null) {
				continue;
			}
			if (update) {
				Object oldValue = customField.getValue(issue);
				if (!newValue.equals(oldValue)) {
					new IssueUpdate(issue, customField, oldValue, newValue).publish();
				}
			} else {
				((MutableIssue) issue).setCustomFieldValue(customField, newValue);
			}
		}
	}

	public void updateFields(Issue issue, Map<String, CSVEntry> data) throws WorkflowException {
		setFieldValues(issue, data, true);
	}

	public void initFields(MutableIssue issue, Map<String, CSVEntry> data) throws WorkflowException {
		setFieldValues(issue, data, false);
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
			log.debug(new Message(issue, customField, "Publishing...").toString(false));

			MutableIssue iss;

			if (issue instanceof MutableIssue) {
				iss = (MutableIssue) issue;
			} else {
				iss = ComponentAccessor.getIssueManager().getIssueObject(issue.getId());
			}
			customField.updateValue(null, iss, new ModifiedValue<Object>(oldValue, newValue),
					new DefaultIssueChangeHolder());
			
			iss.store();

			Object newFieldValue = customField.getValue(iss);

			if ((newValue == null && newFieldValue != null) || !newValue.equals(newFieldValue)) {
				Message message = new Message(issue, customField, "Update failed: " + newValue + " != " + oldValue);
				messageHandler.error(message);
				log.error(message.toString(false));
			} else {
				Message message = new Message(issue, customField, "Updated from " + oldValue + " to " + newValue);
				messageHandler.info(message);
				log.debug(message.toString(false));
			}
		}

		public String getHTMLString() {
			StringBuilder sb = new StringBuilder();
			String url = ComponentAccessor.getApplicationProperties().getString("jira.baseurl") + "/browse/"
					+ issue.getKey();

			sb.append("<a href=\"" + url + "\">" + issue.getKey() + "<a>: ");
			sb.append("Field " + customField.getName() + " will be changed from ");
			sb.append(oldValue + " to " + newValue);

			return sb.toString();
		}
	}
}
