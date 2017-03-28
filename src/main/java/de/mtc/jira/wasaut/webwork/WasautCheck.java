package de.mtc.jira.wasaut.webwork;

import java.util.ArrayList;
import java.util.List;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;

import de.mtc.jira.wasaut.CSVEntry;
import de.mtc.jira.wasaut.CSVParser;
import de.mtc.jira.wasaut.PluginConstants;

public class WasautCheck extends JiraWebActionSupport {

	private static final long serialVersionUID = 1L;
	private List<TablePair> tablePairs;
	
	public static class Pair {
		private String first;
		private String second;
		public Pair(String first, String second) {
			this.first = first;
			this.second = second;
		}
		public String getFirst() {
			return first;
		}
		public String getSecond() {
			return second;
		}
		public boolean isEqual() {
			if(first != null)
				return first.equals(second);
			if(second != null)
				return second.equals(first);
			return true;
		}
	}
	
	public List<String> getTableHeaders() {
		return PluginConstants.RELEVANT_FIELD_NAMES;
	}
	
	public static class TablePair {
		private String issueKey;
		private List<Pair> tableLine = new ArrayList<>();
		public TablePair(String issueKey) {
			this.issueKey = issueKey;
		}
		public List<Pair> getTableLine() {
			return tableLine;
		}
		public void add(String first, String second) {
			tableLine.add(new Pair(first, second));
		}
		public String getIssueKey() {
			return issueKey;
		}
	}

	public List<TablePair> getTablePairs() {
		return tablePairs;
	}
	
	@Override
	protected String doExecute() throws Exception {
		tablePairs = new ArrayList<>();
		JqlQueryBuilder builder = JqlQueryBuilder.newBuilder();
		Query query = builder.where().project("WASAUT", "WASWKOWMA").and().status().notEq("closed").buildQuery();
		SearchService searchService = ComponentAccessor.getComponent(SearchService.class);
		ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
		CustomFieldManager cfm = ComponentAccessor.getCustomFieldManager();
		SearchResults results = searchService.search(user, query, PagerFilter.getUnlimitedFilter());
		for (Issue issue : results.getIssues()) {
			TablePair tablePair = new TablePair(issue.getKey());
			String key = (String) issue.getCustomFieldValue(cfm.getCustomFieldObjectByName("Project / Contract"));
			for (String fieldName : PluginConstants.RELEVANT_FIELD_NAMES) {
				CSVEntry entry = CSVParser.getData().get(key);
				String csvValue = entry == null ? "undefined" : entry.get(fieldName);
				String fieldValue = (String) issue.getCustomFieldValue(cfm.getCustomFieldObjectByName(fieldName));
				tablePair.add(csvValue, fieldValue);
			}
			tablePairs.add(tablePair);
		}
		return SUCCESS;
	}
}
