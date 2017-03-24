package de.mtc.jira.wasaut;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

public class ProjectHelper {

	private static final Logger log = LoggerFactory.getLogger(ProjectHelper.class);
	private List<IssueUpdate> updates = new ArrayList<>();

	private MessageHandler messageHandler;

	public ProjectHelper() {
		this.messageHandler = new MessageHandler();
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

	public void checkUpdate(Issue issue, Map<String, CSVEntry> data) throws DataInputException {
		log.debug("Checking updates for issue: " + issue.getKey());
		List<CustomField> customFields = ComponentAccessor.getCustomFieldManager().getCustomFieldObjects(issue);
		Optional<CustomField> opField = customFields.stream()
				.filter(t -> t.getName().equals(PluginConstants.CF_FIELDS_NAMES[1])).findFirst();
		if (!opField.isPresent()) {
			Message message = new Message(issue, "Key field is missing");
			messageHandler.error(message);
			throw new DataInputException(message.toString(false));
		}
		CustomField typeField = opField.get();
		String type = (String) issue.getCustomFieldValue(typeField);
		if (type == null || type.isEmpty()) {
			Message message = new Message(issue, "The value for the \"Project/Contract\" field is not set");
			messageHandler.error(message);
			throw new DataInputException(message.toString(false));
		}
		CSVEntry entries = data.get(type);
		if (entries == null) {
			Message message = new Message(issue,
					"The value " + type + " for the \"Project/Contract\" field is deprecated or invalid");
			messageHandler.error(message);
			throw new DataInputException(message.toString(false));
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

	private List<Issue> getRelevantIssues(ApplicationUser currentUser) throws DataInputException {
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

	private void setFieldValues(Issue issue, Map<String, CSVEntry> data, boolean update) throws DataInputException {
		log.debug(new Message(issue, "Initializing custom fields").toString(false));
		CustomFieldManager cfm = ComponentAccessor.getCustomFieldManager();
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
		IssueService issueService = ComponentAccessor.getIssueService();
		IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
		for(String fieldName : PluginConstants.RELEVANT_FIELD_NAMES) {
			CustomField cf = cfm.getCustomFieldObjectByName(fieldName);
			if (cf == null) {
				throw new DataInputException("Field " + fieldName + " is missing for issue " + issue.getKey());
			}
			String newValue = csvEntry.get(fieldName);
			issueInputParameters.addCustomFieldValue(cf.getId(), newValue);
		}
		ApplicationUser currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
		UpdateValidationResult validationResult = issueService.validateUpdate(currentUser, issue.getId(), issueInputParameters);
		if(validationResult.isValid()) {
			issueService.update(currentUser, validationResult);
		}
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
			this.newValue = newValue;
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
			return ComponentAccessor.getApplicationProperties().getString("jira.baseurl") + "/browse/"
					+ issue.getKey();
		}

		public void publish() {
			IssueService issueService = ComponentAccessor.getIssueService();
			IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
			issueInputParameters.addCustomFieldValue(customField.getId(), newValue);
			ApplicationUser currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
			UpdateValidationResult validationResult = issueService.validateUpdate(currentUser, issue.getId(), issueInputParameters);
			log.debug("Setting {}={} for issue {}", customField.getName(), newValue, issue.getKey());
			if(validationResult.isValid()) {
				issueService.update(currentUser, validationResult);
			}
		}
			
//			
//			
//			
//			log.debug(new Message(issue, customField, "Publishing...").toString(false));
//
//			MutableIssue iss;
//
//			if (issue instanceof MutableIssue) {
//				iss = (MutableIssue) issue;
//			} else {
//				iss = ComponentAccessor.getIssueManager().getIssueObject(issue.getId());
//			}
//			customField.updateValue(null, iss, new ModifiedValue<Object>(oldValue, newValue),
//					new DefaultIssueChangeHolder());
//
//			// TODO
//			iss.store();
//
//			Object newFieldValue = customField.getValue(iss);
//
//			if ((newValue == null && newFieldValue != null) || !newValue.equals(newFieldValue)) {
//				Message message = new Message(issue, customField, "Update failed: " + newValue + " != " + oldValue);
//				messageHandler.error(message);
//				log.error(message.toString(false));
//			} else {
//				Message message = new Message(issue, customField, "Updated from " + oldValue + " to " + newValue);
//				messageHandler.info(message);
//				log.debug(message.toString(false));
//			}
//		}
	}
}
