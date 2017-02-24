package mtc.jira.contracts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.ModifiedValue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.user.ApplicationUser;
import com.opensymphony.workflow.WorkflowException;

public class ProjectHelper {

	private static final Logger log = LoggerFactory.getLogger(CSVParser.class);

	public static List<Issue> getIssuesForProject(String pluginKey) throws SearchException {

		log.debug("Getting issues for project " + pluginKey);

		SearchService searchService = ComponentAccessor.getComponentOfType(SearchService.class);
		ProjectManager projectManager = ComponentAccessor.getProjectManager();
		Project project = projectManager.getProjectByCurrentKey(pluginKey);
		ApplicationUser currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
		Collection<IssueType> issueTypes = project.getIssueTypes();
		List<Issue> result = new ArrayList<>();

		for (IssueType issueType : issueTypes) {
			String jqlQuery = "project = '" + project.getKey() + "' and status != 'Closed' and issuetype='"
					+ issueType.getName() + "'";
			final SearchService.ParseResult parseResult = searchService.parseQuery(currentUser, jqlQuery);

			log.debug("Executing jql Query: " + jqlQuery);

			if (parseResult.isValid()) {
				final SearchResults results = searchService.search(currentUser, parseResult.getQuery(),
						new com.atlassian.jira.web.bean.PagerFilter<>());
				final List<Issue> issues = results.getIssues();
				log.debug("Result " + issues.stream().map(t -> t.getKey()).collect(Collectors.joining(",")));
				result.addAll(issues);
			} else {
				log.debug("Search result not valid");
			}
		}
		return result;
	}

	public static String fillCustomFields(Issue issue, Map<String, List<String>> data) throws WorkflowException {
		log.debug("### fillCustomFields");
		List<CustomField> customFields = ComponentAccessor.getCustomFieldManager().getCustomFieldObjects(issue);
		log.debug("Custom fields for issue: ");
		customFields.stream().map(t -> t.getName()).forEach(t -> log.debug(t));
		Optional<CustomField> opField = customFields.stream().filter(t -> t.getName().equals("CF_CONTRACT"))
				.findFirst();
		if (!opField.isPresent()) {
			throw new WorkflowException("CF_TYPE field is missing");
		}
		CustomField typeField = opField.get();
		String type = (String) issue.getCustomFieldValue(typeField);
		if (type == null || type.isEmpty()) {
			throw new WorkflowException("Empty type field");
		}
		List<String> entries = data.get(type);
		if (entries == null) {
			String msg = "Unknown project / contracts key: " + type;
			// msg += "\nAvailable types are:\n";
			// msg += data.keySet().stream().collect(Collectors.joining(","));
			throw new WorkflowException(msg);
		}

		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < entries.size(); i++) {
			String name = "CF_" + (i + 1);
			Optional<CustomField> cf = customFields.stream().filter(t -> t.getName().equals(name)).findFirst();
			if (cf.isPresent()) {
				log.debug("Setting field value: " + name + " to " + entries.get(i));
				Object oldValue = cf.get().getValue(issue);
				String newValue = entries.get(i);
				if (issue instanceof MutableIssue) {
					((MutableIssue) issue).setCustomFieldValue(cf.get(), newValue);
				} else {
					upDateCustomField(cf.get(), issue, newValue);
				}
				buf.append(name + " : " + oldValue + " ==> " + newValue);
			}
		}
		return buf.toString();
	}

	private static void upDateCustomField(CustomField cf, Issue issue, String newValue) {
		cf.updateValue(null, issue, new ModifiedValue<Object>(cf.getValue(issue), newValue),
				new DefaultIssueChangeHolder());
		log.debug("Updatet field value " + cf.getValue(issue) + " " + newValue);
	}
}
