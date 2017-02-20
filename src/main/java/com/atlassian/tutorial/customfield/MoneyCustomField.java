package com.atlassian.tutorial.customfield;

import java.math.BigDecimal;

import javax.inject.Inject;

import com.atlassian.jira.issue.customfields.impl.AbstractSingleFieldType;
import com.atlassian.jira.issue.customfields.impl.FieldValidationException;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.customfields.persistence.PersistenceFieldType;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

@Scanned
public class MoneyCustomField extends AbstractSingleFieldType<BigDecimal> {

	@ComponentImport
	CustomFieldValuePersister persister;

	@ComponentImport
	GenericConfigManager configManager;

	@Inject
	public MoneyCustomField(CustomFieldValuePersister customFieldValuePersister,
			GenericConfigManager genericConfigManager) {
		super(customFieldValuePersister, genericConfigManager);
	}

	@Override
	public String getStringFromSingularObject(final BigDecimal singularObject) {
		return singularObject == null ? null : singularObject.toString();
	}

	@Override
	public BigDecimal getSingularObjectFromString(final String string) throws FieldValidationException {
		if (string == null) {
			return null;
		}
		try {
			final BigDecimal decimal = new BigDecimal(string);
			// Check that we don't have too many decimal places
			if (decimal.scale() > 2) {
				throw new FieldValidationException("Maximum of 2 decimal places are allowed.");
			}
			return decimal.setScale(2);
		} catch (NumberFormatException ex) {
			throw new FieldValidationException("Not a valid number.");
		}
	}

	@Override
	protected PersistenceFieldType getDatabaseType() {
		return PersistenceFieldType.TYPE_LIMITED_TEXT;
	}

	@Override
	protected BigDecimal getObjectFromDbValue(final Object databaseValue) throws FieldValidationException {
		return getSingularObjectFromString((String) databaseValue);
	}

	@Override
	protected Object getDbValueFromObject(final BigDecimal customFieldObject) {
		return getStringFromSingularObject(customFieldObject);
	}
}
