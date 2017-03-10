package de.mtc.jira.wasaut.webwork;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.web.action.JiraWebActionSupport;

import de.mtc.jira.wasaut.CSVEntry;
import de.mtc.jira.wasaut.CSVParser;
import de.mtc.jira.wasaut.DataInputException;
import de.mtc.jira.wasaut.Message;
import de.mtc.jira.wasaut.MessageHandler;
import de.mtc.jira.wasaut.PluginCache;
import de.mtc.jira.wasaut.ProjectHelper;
import webwork.action.ActionContext;

public class WasautUpload extends JiraWebActionSupport {

	private static final Logger log = LoggerFactory.getLogger(WasautUpload.class);

	private final static String UPDATE_RESULT_KEY = "___result___";
	private final static String FORM_DATA_KEY = "___formData___";
	private final static String INPUT_DATA_KEY = "___input____";

	private static final long serialVersionUID = 1L;

	private String csv;
	private String jqlQuery = PluginCache.getJqlQuery();
	private List<ProjectHelper.IssueUpdate> issueUpdates;
	private List<ProjectHelper.IssueUpdate> failedUpdates;
	private List<ProjectHelper.IssueUpdate> succeededUpdates;
	private ProjectHelper projectHelper;
	private Exception ex;

	public void setCsv(String csv) {
		this.csv = csv;
	}

	public List<ProjectHelper.IssueUpdate> getIssueUpdates() {
		return issueUpdates;
	}

	public void setJqlQuery(String jqlQuery) {
		this.jqlQuery = jqlQuery;
	}

	public String getJqlQuery() {
		return jqlQuery;
	}

	public String getCsv() {
		return csv;
	}

	public List<ProjectHelper.IssueUpdate> getFailedUpdates() {
		return failedUpdates;
	}
	
	public List<ProjectHelper.IssueUpdate> getSucceededUpdates() {
		return succeededUpdates;
	}
	
	public MessageHandler getMessageHandler() {
		return projectHelper.getMessageHandler();
	}

	public int getHashCode() {
		return System.identityHashCode(this);
	}
	
	public String getIssueLink(Issue issue) {
		return ComponentAccessor.getApplicationProperties().getString("jira.baseurl") + "/browse/"
				+ issue.getKey();
	}
	
	
	public String getStackTraceAsString() {
		if (ex == null) {
			return "An unknown exception has occured";
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintWriter out = new PrintWriter(baos);
		ex.printStackTrace(out);
		return new String(baos.toByteArray());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void checkUpdate() throws DataInputException {
		Map<String, CSVEntry> inputData = CSVParser.readDataFromString(csv);

		projectHelper = new ProjectHelper();
		MessageHandler messageHandler = projectHelper.getMessageHandler();

		try {
			CSVParser.getDataFromFile();
		} catch (DataInputException e) {
			messageHandler.info(new Message(null, "No stored data file found on server, assuming an initial commit."));
		}

		if (jqlQuery != null && !jqlQuery.equals(PluginCache.getJqlQuery())) {
			PluginCache.setJqlQuery(jqlQuery);
		}

		messageHandler.info(new Message(null, "Found stored data: " + CSVParser.getDataFile().getAbsolutePath()));
		messageHandler.info(new Message(null, "Using jql query " + PluginCache.getJqlQuery()));

		projectHelper.getProjectUpdates(inputData);

		Map session = ActionContext.getSession();
		session.put(UPDATE_RESULT_KEY, projectHelper);
		session.put(FORM_DATA_KEY, csv);
		session.put(INPUT_DATA_KEY, inputData);

		issueUpdates = projectHelper.getUpdates();
	}

	@Override
	protected void doValidation() {
		super.doValidation();
	}

	@Override
	public String doDefault() throws Exception {
		return INPUT;
	}

	@Override
	protected String doExecute() throws Exception {
		try {
			checkUpdate();
		} catch (Exception e) {
			log.error("Exception while updating data", e);
			this.ex = e;
			return ERROR;
		}
		return SUCCESS;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public String doCommit() {

		Map session = ActionContext.getSession();
		Object results = session.get(UPDATE_RESULT_KEY);
		List<ProjectHelper.IssueUpdate> updates = null;

		try {
			updates = ((ProjectHelper) results).getUpdates();
		} catch (Exception e) {
			log.error("Unable to commit data", e);
			this.ex = e;
			return ERROR;
		}

		failedUpdates = new ArrayList<>();
		succeededUpdates = new ArrayList<>();
		for (ProjectHelper.IssueUpdate update : updates) {
			try {
				update.publish();
				succeededUpdates.add(update);
			} catch (Exception e) {
				update.setException(e);
				failedUpdates.add(update);
				log.error("Unable to publish data", e);
			}
		}

		try {
			PluginCache.setData((Map<String, CSVEntry>) session.get(INPUT_DATA_KEY));
			log.debug("Cached data");
		} catch (Exception e) {
			log.error("Failed to cache data ", e);
		}

		try {
			CSVParser.writeCSVFile((String) session.get(FORM_DATA_KEY));
		} catch (Exception e) {
			log.error("Failed " + e);
		}

		return "commit";
	}
}
