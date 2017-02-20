package mtc.jira.contracts.workflow;

import com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory;
import com.atlassian.jira.plugin.workflow.WorkflowPluginValidatorFactory;
import com.google.common.collect.Maps;
import com.opensymphony.workflow.loader.AbstractDescriptor;

import java.util.Map;

@SuppressWarnings({"rawtypes","unchecked"})
public class CloseIssueWorkflowValidatorFactory extends AbstractWorkflowPluginFactory
		implements WorkflowPluginValidatorFactory {
	public static final String FIELD_WORD = "word";

	@Override
	protected void getVelocityParamsForInput(Map velocityParams) {

	}

	@Override
	protected void getVelocityParamsForEdit(Map velocityParams, AbstractDescriptor descriptor) {

	}

	@Override
	protected void getVelocityParamsForView(Map velocityParams, AbstractDescriptor descriptor) {

	}

	@Override
	public Map getDescriptorParams(Map validatorParams) {
		// Process The map
		return Maps.newHashMap();
	}
}
