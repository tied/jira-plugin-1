package mtc.jira.contracts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CSVParser {


	private final static String CSV_FILE = "data.csv";

	public static Map<String, List<String>> getData() throws IOException {
		Map<String, List<String>> result = new HashMap<>();
		File file = new File(CSV_FILE);
		if (!file.exists()) {
			throw new FileNotFoundException("File " + file.getAbsolutePath() + " does not exist");
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
}
