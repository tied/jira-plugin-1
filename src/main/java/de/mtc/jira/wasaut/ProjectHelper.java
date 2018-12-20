package de.mtc.jira.wasaut;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.IssueService.UpdateValidationResult;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.bean.PagerFilter;

public class ProjectHelper {
	


	private static final Logger log = LoggerFactory.getLogger(ProjectHelper.class);
	private List<IssueUpdate> updates = new ArrayList<>();
	private CustomFieldManager cfm;

	private MessageHandler messageHandler;

	public ProjectHelper() {
		this.messageHandler = new MessageHandler();
		this.cfm = ComponentAccessor.getCustomFieldManager();
	}

	public void getProjectUpdates(Map<String, CSVEntry> data) throws DataInputException {
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

	private boolean hasEqualValue(Object o1, Object o2) {
		if (o1 == null) {
			return o2 == null;
		}
		if (o2 == null) {
			return o1 == null;
		}
		return o1.equals(o2);
	}

	private CSVEntry getCSVEntry(Issue issue, Map<String, CSVEntry> data) throws DataInputException {
		log.debug(new Message(issue, "Initializing custom fields").toString(false));
		CustomField typeField = cfm.getCustomFieldObjectByName(PluginConstants.CF_FIELDS_NAMES[1]);
		if (typeField == null) {
			throw new DataInputException("Type Field is missing for issue " + issue.getKey());
		}
		String type = (String) issue.getCustomFieldValue(typeField);
		CSVEntry csvEntry = data.get(type);
		if (csvEntry == null) {
			Message message = new Message(issue, "Unknown value in key field " + type);
			throw new DataInputException(message.toString(false));
		}
		return csvEntry;
	}

	public void checkUpdate(Issue issue, Map<String, CSVEntry> data) throws DataInputException {
		log.debug("Checking updates for issue: " + issue.getKey());
		CSVEntry csvEntry = getCSVEntry(issue, data);
		for (String fieldName : PluginConstants.RELEVANT_FIELD_NAMES) {
			CustomField cf = cfm.getCustomFieldObjectByName(fieldName);
			if (cf == null) {
				throw new DataInputException("Field " + fieldName + " is missing for issue " + issue.getKey());
			}
			Object oldValue = cf.getValue(issue);
			String newValue = csvEntry.get(fieldName);
			if (!hasEqualValue(oldValue, newValue)) {
				updates.add(new IssueUpdate(issue, cf, oldValue, newValue));
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void setFieldValues(Issue issue, Map<String, CSVEntry> data, boolean update) throws DataInputException {
		log.debug(new Message(issue, "Initializing custom fields").toString(false));
		CSVEntry csvEntry = getCSVEntry(issue, data);
		IssueService issueService = ComponentAccessor.getIssueService();
		IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
		Map<String, Object> check = new HashMap<>();
		for (String fieldName : PluginConstants.RELEVANT_FIELD_NAMES) {
			CustomField cf = cfm.getCustomFieldObjectByName(fieldName);
			if (cf == null) {
				throw new DataInputException("Field " + fieldName + " is missing for issue " + issue.getKey());
			}
			String newValue = csvEntry.get(fieldName);
			if (newValue == null || newValue.isEmpty()) {
				newValue = PluginConstants.NONE;
			}
			log.debug("Preparing to set field {} to value {}", cf.getName(), newValue);
			issueInputParameters.addCustomFieldValue(cf.getId(), newValue);
			check.put(cf.getName(), newValue);
		}
		ApplicationUser currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
		UpdateValidationResult validationResult = issueService.validateUpdate(currentUser, issue.getId(),
				issueInputParameters);
		if (validationResult.isValid()) {
			issueService.update(currentUser, validationResult);
		} else {
			log.error("Issue {} cannot be updated, invalid validation result", issue.getKey());
			for (String message : validationResult.getErrorCollection().getErrorMessages()) {
				log.error(message);
			}
		}

		for (String fieldName : check.keySet()) {
			Object fieldValue = issue.getCustomFieldValue(cfm.getCustomFieldObjectByName(fieldName));
			Object expected = check.get(fieldName);
			if ((fieldValue == null && expected != null) || (fieldValue != null && expected == null)
					|| !fieldValue.equals(expected)) {
				log.error("Expected: {}, Actual: {}", expected, fieldValue);
			}
		}
	}

	private List<Issue> getRelevantIssues(ApplicationUser currentUser) throws DataInputException {
		String jqlQuery = PluginCache.getJqlQuery();
		List<Issue> result = new ArrayList<>();
		SearchService searchService = ComponentAccessor.getComponentOfType(SearchService.class);
		SearchService.ParseResult parseResult = searchService.parseQuery(currentUser, jqlQuery);
		if (parseResult.isValid()) {
			SearchResults results = null;
			try {
				results = searchService.search(currentUser, parseResult.getQuery(), PagerFilter.getUnlimitedFilter());
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

	public void updateFields(Issue issue, Map<String, CSVEntry> data) throws DataInputException {
		setFieldValues(issue, data, true);
	}

	public void initFields(MutableIssue issue, Map<String, CSVEntry> data) throws DataInputException {
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
			this.newValue = newValue == null || newValue.isEmpty() ? PluginConstants.NONE : newValue;
		}

		public Object getOldValue() {
			return oldValue;
		}

		public String getNewValue() {
			return newValue;
		}

		public CustomField getCustomField() {
			return customField;
		}

		public Issue getIssue() {
			return issue;
		}

		public String getLink() {
			return ComponentAccessor.getApplicationProperties().getString("jira.baseurl") + "/browse/" + issue.getKey();
		}

		public void publish() {
			IssueService issueService = ComponentAccessor.getIssueService();
			IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
			issueInputParameters.addCustomFieldValue(customField.getId(), newValue);
			ApplicationUser currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
			UpdateValidationResult validationResult = issueService.validateUpdate(currentUser, issue.getId(),
					issueInputParameters);
			log.debug("Setting {}={} for issue {}", customField.getName(), newValue, issue.getKey());
			if (validationResult.isValid()) {
				issueService.update(currentUser, validationResult);
			}
		}
	}
}
