package com.example.plugins.tutorial.jira.workflow;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FileParser {
	
	private static void parseLine(String line, Map<String, List<String>> data) {
		List<String> values = new ArrayList<>();
		String[] parts = line.split(";");
		for(int i=1; i<parts.length; i++) {
			values.add(parts[i]);
		}
		data.put(parts[0], values);
	}
	
	public static Map<String, List<String>> getCSVData(File file) throws FileNotFoundException {
		Map<String, List<String>> result = new HashMap<>();
		InputStream in = new FileInputStream(file);
		BufferedReader read = new BufferedReader(new InputStreamReader(in));
		read.lines().forEach(l -> parseLine(l, result));
		return result;
	}
	
	static String getRandomString() {
		char[] ch = new char[5];
		for(int i=0; i<ch.length; i++) {
			int n = new Random().nextInt(26) + 65;
			ch[i] = (char)n;
		}
		return new String(ch);
	}
	
	public static void createFile() {
		StringBuilder sb = new StringBuilder();
		for(int i=1; i<10; i++) {
			sb.append("TYPE_"+i+";");
			String str = IntStream.range(0, 5).mapToObj(t -> getRandomString()).collect(Collectors.joining(";"));
			sb.append(str);
			sb.append("\n");
		}
		Path path = Paths.get("data.csv");
		try(BufferedWriter writer = Files.newBufferedWriter(path)) {
			writer.write(sb.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// System.out.println(getCSVData());
	}
}
