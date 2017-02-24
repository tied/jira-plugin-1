package mtc.jira.contracts;

import java.util.ArrayList;
import java.util.List;

public class MessageHandler {

	private List<String> errors = new ArrayList<>();
	private List<String> warnings = new ArrayList<>();
	private List<String> infos = new ArrayList<>();
	
	public void error(String message) {
		errors.add(message);
	}
	
	public void warn(String message) {
		warnings.add(message);
	}
	
	public void info(String message) {
		infos.add(message);
	}
	
	public List<String> getInfos() {
		return infos;
	}
	
	public List<String> getWarnings() {
		return warnings;
	}
	
	public List<String> getErrors() {
		return errors;
	}
	
}
