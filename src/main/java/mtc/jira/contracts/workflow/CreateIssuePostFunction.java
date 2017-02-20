package com.example.plugins.tutorial.jira.workflow;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.util.AttachmentUtils;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;

public class CreateIssuePostFunction extends AbstractJiraFunctionProvider {

	private static Logger log = LoggerFactory.getLogger(CreateIssuePostFunction.class);

	private void fillCustomFields(MutableIssue issue, Map<String, List<String>> data) throws WorkflowException {
		log.debug("### fillCustomFields");
		List<CustomField> customFields = ComponentAccessor.getCustomFieldManager().getCustomFieldObjects(issue);
		log.debug("Custom fields for issue: ");
		customFields.stream().map(t -> t.getName()).forEach(t -> log.debug(t));
		Optional<CustomField> opField = customFields.stream().filter(t -> t.getName().equals("CF_TYPE")).findFirst();
		if (!opField.isPresent()) {
			throw new WorkflowException("CF_TYPE field is missing");
		}
		CustomField typeField = opField.get();
		String type = (String) issue.getCustomFieldValue(typeField);
		if(type == null || type.isEmpty()) {
			throw new WorkflowException("Empty type field");
		}
		List<String> entries = data.get(type);
		if (entries == null) {
			String msg = "Unknown type " + type;
			msg += "\nAvailable types are:\n";
			msg += data.keySet().stream().collect(Collectors.joining(","));
			throw new WorkflowException(msg);
		}
		for (int i = 0; i < entries.size(); i++) {
			String name = "CF_" + (i + 1);
			Optional<CustomField> cf = customFields.stream().filter(t -> t.getName().equals(name)).findFirst();
			if (cf.isPresent()) {
				log.debug("Setting field value: " + name + " to " + entries.get(i));
				issue.setCustomFieldValue(cf.get(), entries.get(i));
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "deprecation" })
	@Override
	public void execute(Map transientVars, Map args, PropertySet ps) throws WorkflowException {
		log.debug("Executing create post function");
		MutableIssue issue = getIssue(transientVars);
		MutableIssue atIssue = ComponentAccessor.getIssueManager().getIssueObject("PLUG-1");
		if (atIssue == null) {
			log.error("Unable to get data.csv: Unknown issue PLUG-1");
			throw new WorkflowException("Unable to get data.csv: Unknown issue PLUG-1");
		}
		Optional<Attachment> opAttachment = atIssue.getAttachments().stream()
				.filter(t -> t.getFilename().equals("data.csv")).findAny();
		if (!opAttachment.isPresent()) {
			log.error("No appropriate attachment found in issue PLUG-1");
			throw new WorkflowException("No appropriate attachment found in issue PLUG-1");
		}
		File attachmentFile = AttachmentUtils.getAttachmentFile(opAttachment.get());
		try {
			Map<String, List<String>> data = FileParser.getCSVData(attachmentFile);
			log.debug("Parsed data.csv: " + data);
			fillCustomFields(issue, data);
			return;
		} catch (FileNotFoundException e) {
			log.error("Error reading data.csv", e);
			throw new WorkflowException(e);
		}
	}
}
