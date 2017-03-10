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

/**
 * Simple JIRA listener using the atlassian-event library and demonstrating
 * plugin lifecycle integration.
 */
@Scanned
@Component
public class IssueListener implements InitializingBean, DisposableBean {

	private static final Logger log = LoggerFactory.getLogger(IssueListener.class);

	@ComponentImport
	private final EventPublisher eventPublisher;

	/**
	 * Constructor.
	 * 
	 * @param eventPublisher
	 *            injected {@code EventPublisher} implementation.
	 */
	@Autowired
	public IssueListener(EventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	/**
	 * Called when the plugin has been enabled.
	 * 
	 * @throws Exception
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		log.info("\n==================================\n");
		log.info("\n   Enabling Jira-Holiday Plugin   \n");
		log.info("\n   Jira-Holiday has been enabled    \n");
		eventPublisher.register(this);
	}

	/**
	 * Called when the plugin is being disabled or removed.
	 * 
	 * @throws Exception
	 */
	@Override
	public void destroy() throws Exception {
		log.info("Plugin jira-holiday is destroyed");
		eventPublisher.unregister(this);
	}

	/**
	 * Receives any {@code IssueEvent}s sent by JIRA.
	 * 
	 * @param issueEvent
	 *            the IssueEvent passed to us
	 */
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
				log.error("An exception occured while trying to set field values for issue " + issue.getKey(),
						e.getMessage());
			}
		}
	}
}
