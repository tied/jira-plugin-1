package de.mtc.jira.wasaut;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.Issue;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

@Scanned
@Component
public class IssueListener implements InitializingBean, DisposableBean {

	private static final Logger log = LoggerFactory.getLogger(IssueListener.class);

	@ComponentImport
	private final EventPublisher eventPublisher;

	@Autowired
	public IssueListener(EventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		log.info("\n==================================\n");
		log.info("\n   Enabling Jira-Holiday Plugin   \n");
		log.info("\n   MTC-WASAUT plugin has been enabled    \n");
		eventPublisher.register(this);
	}

	@Override
	public void destroy() throws Exception {
		log.info("Plugin MTC-WASAUT is destroyed");
		eventPublisher.unregister(this);
	}

	@EventListener
	public void onIssueEvent(IssueEvent issueEvent) {
		log.debug("Registered issue update event ... ");
		if (!PluginConstants.isRelevantProject(issueEvent.getProject())) {
			log.debug("... but not in my projects");
			return;
		}
		Long eventTypeId = issueEvent.getEventTypeId();
		if (eventTypeId.equals(EventType.ISSUE_UPDATED_ID) || eventTypeId.equals(EventType.ISSUE_CREATED_ID)) {
			Issue issue = issueEvent.getIssue();
			try {
				new ProjectHelper().updateFields(issue, CSVParser.getData());
			} catch (Exception e) {
				log.error("An exception occured while trying to set field values for issue " + issue.getKey(), e);
			}
		}
	}
}
