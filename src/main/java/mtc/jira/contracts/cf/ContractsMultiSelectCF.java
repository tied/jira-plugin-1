package mtc.jira.contracts.cf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.impl.FieldValidationException;
import com.atlassian.jira.issue.customfields.impl.GenericTextCFType;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.TextFieldCharacterLengthValidator;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import mtc.jira.contracts.CSVParser;

@Scanned
public class ContractsMultiSelectCF extends GenericTextCFType {

	@Inject
	protected ContractsMultiSelectCF(@ComponentImport CustomFieldValuePersister customFieldValuePersister,
			@ComponentImport GenericConfigManager genericConfigManager,
			@ComponentImport TextFieldCharacterLengthValidator textFieldCharacterLengthValidator,
			@ComponentImport JiraAuthenticationContext jiraAuthenticationContext) {
		super(customFieldValuePersister, genericConfigManager, textFieldCharacterLengthValidator,
				jiraAuthenticationContext);
	}

	@Override
	public String getSingularObjectFromString(String string) throws FieldValidationException {
		return super.getSingularObjectFromString(string);
	}
	
	@Override
	protected Object getDbValueFromObject(String customFieldObject) {
		return super.getDbValueFromObject(customFieldObject);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Map getVelocityParameters(Issue issue, CustomField field, FieldLayoutItem fieldLayoutItem) {
		Map<String, List<String>> map = new HashMap<>();
		System.out.println("I AM RUNNING");
		try {
			List<String> results = new ArrayList<>(CSVParser.getDataFromFile().keySet());
			map.put("result", results);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return map;
	}
}
