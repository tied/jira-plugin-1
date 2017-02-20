package com.example.plugins.tutorial.jira.workflow;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.inject.Inject;

import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.issue.comparator.ConstantsComparator;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory;
import com.atlassian.jira.plugin.workflow.WorkflowPluginConditionFactory;
import com.atlassian.jira.util.collect.MapBuilder;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.opensymphony.workflow.loader.AbstractDescriptor;
import com.opensymphony.workflow.loader.ConditionDescriptor;

/*
This is the factory class responsible for dealing with the UI for the post-function.
This is typically where you put default values into the velocity context and where you store user input.
 */
@Scanned
public class ParentIssueBlockingConditionFactory extends AbstractWorkflowPluginFactory
		implements WorkflowPluginConditionFactory {

	@ComponentImport
	private final ConstantsManager constantsManager;

	@Inject
	public ParentIssueBlockingConditionFactory(ConstantsManager constantsManager) {
		this.constantsManager = constantsManager;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void getVelocityParamsForInput(Map velocityParams) {
		// all available statuses
		Collection<Status> statuses = constantsManager.getStatuses();
		velocityParams.put("statuses", Collections.unmodifiableCollection(statuses));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void getVelocityParamsForEdit(Map velocityParams, AbstractDescriptor descriptor) {
		getVelocityParamsForInput(velocityParams);
		velocityParams.put("selectedStatuses", getSelectedStatusIds(descriptor));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void getVelocityParamsForView(Map velocityParams, AbstractDescriptor descriptor) {
		Collection<String> selectedStatusIds = getSelectedStatusIds(descriptor);
		List<Status> selectedStatuses = new LinkedList<>();
		for (Iterator<String> iterator = selectedStatusIds.iterator(); iterator.hasNext();) {
			String statusId = iterator.next();
			Status selectedStatus = constantsManager.getStatus(statusId);
			if (selectedStatus != null) {
				selectedStatuses.add(selectedStatus);
			}
		}
		// Sort the list of statuses so as they are displayed consistently
		Collections.sort(selectedStatuses, new ConstantsComparator());

		velocityParams.put("statuses", Collections.unmodifiableCollection(selectedStatuses));
	}

	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Map getDescriptorParams(Map conditionParams) {
		// process the map which will contain the request parameters
		// for now simply concatenate into a comma separated string
		// production code would do something more robust, for starters it would
		// remove the params
		// you are not interested in, like atl_token and workflowMode
		Collection<String> statusIds = conditionParams.keySet();
		StringBuffer statIds = new StringBuffer();

		for (Iterator<String> iterator = statusIds.iterator(); iterator.hasNext();) {
			statIds.append(iterator.next() + ",");
		}

		return MapBuilder.build("statuses", statIds.substring(0, statIds.length() - 1));
	}

	private Collection<String> getSelectedStatusIds(AbstractDescriptor descriptor) {
		Collection<String> selectedStatusIds = new LinkedList<>();
		if (!(descriptor instanceof ConditionDescriptor)) {
			throw new IllegalArgumentException("Descriptor must be a ConditionDescriptor.");
		}

		ConditionDescriptor conditionDescriptor = (ConditionDescriptor) descriptor;

		String statuses = (String) conditionDescriptor.getArgs().get("statuses");
		StringTokenizer st = new StringTokenizer(statuses, ",");

		while (st.hasMoreTokens()) {
			selectedStatusIds.add(st.nextToken());
		}

		return selectedStatusIds;
	}

}
