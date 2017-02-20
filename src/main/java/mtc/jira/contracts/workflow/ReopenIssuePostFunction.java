package com.example.plugins.tutorial.jira.workflow;

import java.math.BigDecimal;
import java.util.Map;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;

public class ReopenIssuePostFunction extends AbstractJiraFunctionProvider{

	@SuppressWarnings("rawtypes")
	@Override
	public void execute(Map transientVars, Map args, PropertySet ps) throws WorkflowException {
		MutableIssue issue = getIssue(transientVars);
		for(CustomField field : ComponentAccessor.getCustomFieldManager().getCustomFieldObjects()) {
			if(field.getName().equals("My Field")) {
				issue.setCustomFieldValue(field, new BigDecimal(2222));
				break;
			}
		}	
	}
}
