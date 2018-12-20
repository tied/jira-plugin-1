package de.mtc.jira.wasaut.cf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.impl.GenericTextCFType;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.TextFieldCharacterLengthValidator;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import de.mtc.jira.wasaut.CSVParser;
import de.mtc.jira.wasaut.DataInputException;


/**
 * com.atlassian.tutorial.de.mtc.wasaut:wasaut-custom-field
 * @author EMJVK
 *
 */
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
	public void updateValue(CustomField customField, Issue issue, String value) {
		super.updateValue(customField, issue, value);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Map getVelocityParameters(Issue issue, CustomField field, FieldLayoutItem fieldLayoutItem) {
		Map<String, Object> map = new HashMap<>();
		String value = null;
		try {
			value = (String) issue.getCustomFieldValue(field);
			List<String> results = new ArrayList<>();
			if (value == null || value.isEmpty() || value.equalsIgnoreCase("None")) {
				map.put("selected", Boolean.FALSE);
			} else {
				map.put("selected", Boolean.TRUE);
				results.add(value);
			}
			results.addAll(CSVParser.getData().keySet());
			map.put("result", results);
		} catch (DataInputException e) {
			log.error("An error occured while parsing the content for issue " + issue, e);
		}
		return map;
	}
}
