package de.mtc.jira.wasaut;

import java.util.HashMap;
import java.util.Map;

public class CSVEntry {
	
	private final Map<String,String> columns = new HashMap<>();
	
	public void put(String key, String value) {
		columns.put(key, value);
	}
	
	public String get(String key) {
		return columns.get(key);
	}
	
	public String toString() {
		return columns.toString();
	}
	
}
