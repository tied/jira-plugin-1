package de.mtc.jira.wasaut;

import java.util.Map;

public class CSVData {

	private final Map<String, CSVEntry> data;
	private String changedBy;
	private String lastChanged;
	
	public CSVData(Map<String, CSVEntry> data) {
		this.data = data;
	}
	
	public String getLastChanged() {
		return lastChanged;
	}
	
	public void setLastChanged(String lastChanged) {
		this.lastChanged = lastChanged;
	}
	
	public String getChangedBy() {
		return changedBy;
	}
	
	public void setChangedBy(String changedBy) {
		this.changedBy = changedBy;
	}
	
}
