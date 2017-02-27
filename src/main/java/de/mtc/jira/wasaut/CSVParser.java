package de.mtc.jira.wasaut;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.atlassian.jira.workflow.WorkflowException;

public class CSVParser {

	private final static String DATA_FILE = "/var/atlassian/application-data/jira/data/wasaut.csv";

	public static Map<String, CSVEntry> getData() throws IOException, WorkflowException {
		Map<String, CSVEntry> result = PluginCache.getData();
		if (result == null) {
			result = getDataFromFile();
		}
		return result;
	}

	public static Map<String, CSVEntry> getDataFromFile() throws IOException, WorkflowException {
		File file = new File(DATA_FILE);
		if (!file.exists()) {
			throw new FileNotFoundException(DATA_FILE + " does not exist");
		}
		InputStream in = new FileInputStream(file);
		try (BufferedReader read = new BufferedReader(new InputStreamReader(in))) {
			return readData(read.lines().collect(Collectors.toList()));
		}
	}

	public static Map<String, CSVEntry> readDataFromString(String str) throws WorkflowException {
		return readData(Arrays.asList(str.split("\n")));
	}

	private static Map<String, CSVEntry> readData(List<String> lines) throws WorkflowException {
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

	public static void writeCSVFile(String csv) throws IOException {
		File file = new File(DATA_FILE);
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			writer.write(csv);
		}
	}
	
	private static void checkFirstLine(String line) throws WorkflowException {
		String[] parts = line.split(";");
		if (parts.length < 2) {
			return;
		}
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
			throw new WorkflowException("Incorrect start line");
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

	public static String getDataPath() {
		return DATA_FILE;
	}
	
	
	// private static File findFile() throws WorkflowException {
	// if (CSV_FILE == null) {
	// CSV_FILE = findDataPath();
	// }
	// File file = new File(CSV_FILE);
	// if (!file.exists()) {
	// throw new WorkflowException("File " + file.getAbsolutePath() + " does not
	// exist");
	// }
	// return file;
	// }
	//
	// private static String findDataPath() throws WorkflowException {
	// File folder;
	// try {
	// // Just test if you can access the temp folder
	// folder = File.createTempFile("xy", "tmp").getParentFile();
	// } catch (IOException e) {
	// throw new WorkflowException("Cannot write to temporary folder");
	// }
	// File[] files = folder.listFiles(new FilenameFilter() {
	// @Override
	// public boolean accept(File dir, String name) {
	// return name.startsWith(FILE_PREFIX) && name.endsWith(FILE_EXT);
	// }
	// });
	// if (files != null && files.length > 0) {
	// Arrays.sort(files, new Comparator<File>() {
	// @Override
	// public int compare(File o1, File o2) {
	// return Long.compare(o1.lastModified(), o2.lastModified());
	// }
	// });
	// return files[files.length - 1].getAbsolutePath();
	// }
	// throw new WorkflowException("Unable to find data file");
	// }

//	public static void setCSVPath(String csv) {
//		CSV_FILE = csv;
//	}

	public static void main(String[] args) throws IOException {
		String path = "C:\\Users\\EMJVK\\Downloads\\BBS_Contracts_2017_02_16.csv";
		InputStream in = new FileInputStream(new File(path));
		try (BufferedReader read = new BufferedReader(new InputStreamReader(in))) {
			Map<String, CSVEntry> result = readData(read.lines().collect(Collectors.toList()));
			for (Map.Entry<String, CSVEntry> entry : result.entrySet()) {
				System.out.println();
				System.out.println(entry.getKey());
				CSVEntry csv = entry.getValue();
				System.out.println(csv.get(CSVEntry.DEPARTMENT));
				System.out.println(csv.get(CSVEntry.SITE_AREA));
				System.out.println(csv.get(CSVEntry.SITE_NAME));
			}
		}
	}
}
