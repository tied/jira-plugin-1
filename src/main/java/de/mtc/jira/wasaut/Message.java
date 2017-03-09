package de.mtc.jira.wasaut;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;

public class Message {

	private Issue issue;
	private CustomField customField;
	private String message;

	public Message(Issue issue, String message) {
		this.issue = issue;
		this.message = message;
	}

	public Message(Issue issue,  CustomField customField, String message) {
		this(issue, message);
		this.customField = customField;
	}

	public String toString(boolean html) {
		StringBuilder sb = new StringBuilder();
		if (issue != null) {
			if (html) {
				String url = ComponentAccessor.getApplicationProperties().getString("jira.baseurl") + "/browse/"
						+ issue.getKey();
				sb.append("<a href=\"" + url + "\">" + issue.getKey() + "<a>: ");
			} else {
				sb.append(issue.getKey());
			}
		}
		if (customField != null) {
			sb.append("Field " + customField.getName() + " [" + issue.getCustomFieldValue(customField) + "]");
		}
		sb.append(message);

		return sb.toString();
	}

	public Issue getIssue() {
		return issue;
	}
	
	public CustomField getCustomField() {
		return customField;
	}
	
	public String getMessage() {
		return message;
	}
	
}
