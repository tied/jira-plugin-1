package mtc.jira.contracts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.workflow.WorkflowException;

public class CSVParser {

	private static String CSV_FILE = "data.csv";
	private static String FILE_PREFIX = "contract-data";
	private static String FILE_EXT = ".tmp";

	public static Map<String, List<String>> getData() throws IOException, WorkflowException {
		Map<String, List<String>> result = new HashMap<>();
		if(CSV_FILE == null) {
			CSV_FILE = findDataPath();
		}		
		File file = new File(CSV_FILE);
		if (!file.exists()) {
			throw new WorkflowException("File " + file.getAbsolutePath() + " does not exist");
		}
		InputStream in = new FileInputStream(file);
		try (BufferedReader read = new BufferedReader(new InputStreamReader(in))) {
			read.lines().forEach(l -> parseLine(l, result));
		}
		return result;
	}

	private static void parseLine(String line, Map<String, List<String>> result) {
		String[] parts = line.split(";");
		if (!isDataLine(parts)) {
			return;
		}
		List<String> entries = new ArrayList<>();
		for (int i = 2; i < parts.length; i++) {
			entries.add(parts[i]);
		}
		result.put(parts[1], entries);
	}

	private static boolean isDataLine(String[] parts) {
		if (parts.length < 4) {
			return false;
		}
		String id = parts[0].trim();
		return !id.isEmpty() && Character.isDigit(id.charAt(0));
	}
	
	private static String findDataPath() throws IOException, WorkflowException {
		File folder = File.createTempFile("yx", "tmp").getParentFile();
		File[] files = folder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(FILE_PREFIX) && name.endsWith(FILE_EXT);
			}
		});
		if(files != null && files.length > 0) {
			Arrays.sort(files, new Comparator<File>() {
				@Override
				public int compare(File o1, File o2) {
					return Long.compare(o1.lastModified(), o2.lastModified());
				}
			});
			return files[files.length-1].getAbsolutePath();
		}
		throw new WorkflowException("Unable to find data file");
	}
	
	
	
	public static void setCSVPath(String csv) {
		CSV_FILE = csv;
	}
}
