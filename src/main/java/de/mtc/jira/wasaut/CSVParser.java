package de.mtc.jira.wasaut;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jfree.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.util.JiraHome;

public class CSVParser {

	private final static String CSV_FILE_NAME = "wasaut.csv";
	private final static Logger log = LoggerFactory.getLogger("CSVParser.class");

	public static Map<String, CSVEntry> getData() throws DataInputException {
		Map<String, CSVEntry> result = PluginCache.getData();
		log.debug("Returning cached data");
		if (result == null) {
			result = getDataFromFile();
		}
		return result;
	}

	public static Map<String, CSVEntry> readDataFromString(String str) throws DataInputException {
		return readDataLines(Arrays.asList(str.split("\n")));
	}
	
	public static File getDataFile() {
		File dataFolder = ComponentAccessor.getComponent(JiraHome.class).getDataDirectory();
		Log.debug("Found data folder " + dataFolder);
		return new File(dataFolder, CSV_FILE_NAME);
	}

	public static void writeCSVFile(String csv) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(getDataFile()))) {
			writer.write(csv);
		}
	}

	private static Map<String, CSVEntry> getDataFromFile() throws DataInputException {
		File file = getDataFile();
		if (!file.exists()) {
			throw new DataInputException("File " + file + " does not exist");
		}
		try {
			InputStream in = new FileInputStream(file);
			log.debug("Reading file " + file);
			try (BufferedReader read = new BufferedReader(new InputStreamReader(in))) {
				return readDataLines(read.lines().collect(Collectors.toList()));
			}
		} catch (Exception e) {
			throw new DataInputException("Error parsing file ", e);
		}
	}
	

	private static Map<String, CSVEntry> readDataLines(List<String> lines) throws DataInputException {
		Map<String, CSVEntry> result = new LinkedHashMap<>();
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if (i == 0) {
				checkFirstLine(line);
			} else {
				parseLine(line, result);
			}
		}
		return result;
	}

	private static void checkFirstLine(String line) throws DataInputException {
		String[] parts = line.split(";");
		boolean check = parts.length == PluginConstants.CF_FIELDS_NAMES.length;
		if (check) {
			for (int i = 0; i < parts.length; i++) {
				if (!parts[i].trim().equals(PluginConstants.CF_FIELDS_NAMES[i])) {
					check = false;
					break;
				}
			}
		}
		if (!check) {
			throw new DataInputException("Incorrect start line: " + line);
		}
	}

	private static void parseLine(String line, Map<String, CSVEntry> result) {
		String[] parts = line.split(";");
		if (parts.length < 2) {
			return;
		}
		CSVEntry entry = new CSVEntry();
		for (int i = 2; i < parts.length; i++) {
			entry.put(PluginConstants.CF_FIELDS_NAMES[i], parts[i]);
		}
		result.put(parts[1], entry);
	}
}
