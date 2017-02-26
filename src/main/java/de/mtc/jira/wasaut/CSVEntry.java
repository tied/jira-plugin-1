package mtc.jira.contracts;

import java.util.HashMap;
import java.util.Map;

public class CSVEntry {
	
	public final static String SITE_AREA = "Site Area";
	public final static String SITE_NAME = "Site-Name";
	public final static String DEPARTMENT = "BBS-Department";

	Map<String,String> columns = new HashMap<>();
	
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
