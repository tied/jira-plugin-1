package de.mtc.jira.wasaut;

import java.util.Map;

import com.atlassian.jira.plugin.webfragment.conditions.AbstractJiraCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;

@SuppressWarnings("deprecation")
public class WebItemCondition extends AbstractJiraCondition {

	@Override
	public boolean shouldDisplay(ApplicationUser arg0, JiraHelper arg1) {
		@SuppressWarnings("rawtypes")
		Map params = arg1.getContextParams();
		Object project = params.get("project");
		if(project != null && (project instanceof Project)) {
			return PluginConstants.isRelevantProject((Project)project);
		}
		return false;
	}

}
