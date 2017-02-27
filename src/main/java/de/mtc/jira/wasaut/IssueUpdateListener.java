package de.mtc.jira.wasaut;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.event.issue.AbstractIssueEventListener;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.issue.Issue;

public class IssueUpdateListener extends AbstractIssueEventListener {

	private static final Logger log = LoggerFactory.getLogger(IssueUpdateListener.class);
	
	@Override
	public void issueCreated(IssueEvent event) {
		log.debug("Registered issue create event ... ");
		if(!Arrays.asList(PluginConstants.PROJECTS).contains(event.getProject().getKey())) {
			log.debug("... but not in my projects");
		}
		Issue issue = event.getIssue();
		debugFields(issue, "BEFORE");
		try {
			new ProjectHelper().updateFields(issue, CSVParser.getData());
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
		debugFields(issue, "AFTER");
	}
	
	@Override
	public void issueUpdated(IssueEvent event) {
		log.debug("Registered issue update event ... ");
		if(!Arrays.asList(PluginConstants.PROJECTS).contains(event.getProject().getKey())) {
			log.debug("... but not in my projects");
		}		
		Issue issue = event.getIssue();
		debugFields(issue, "BEFORE");
		try {
			new ProjectHelper().updateFields(issue, CSVParser.getData());
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
		debugFields(issue, "AFTER");
	}

	private static void debugFields(Issue issue, String head) {
		log.debug("Custom Field values of " + issue.getKey() + " " + head + " update:");		
		ComponentAccessor.getCustomFieldManager().getCustomFieldObjects().forEach(t -> log.debug(t.getName() + ": " + t.getValue(issue)));
	}
}
