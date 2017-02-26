package de.mtc.jira.wasaut;

import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginCache {
	
	private final static Logger log = LoggerFactory.getLogger(PluginCache.class);
	
	private static Map<String, CSVEntry> data;
		
	public static void setData(Map<String, CSVEntry> pData) {
		log.debug("Caching data on");
		data = pData;
	}
	
	public static Map<String, CSVEntry> getData() {
		return data;
	}

	public static Collection<String> getOptions() {
		return data.keySet();
	}
}
