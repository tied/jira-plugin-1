package de.mtc.jira.wasaut;

import java.util.ArrayList;
import java.util.List;

public class MessageHandler {

	private List<Message> errors = new ArrayList<>();
	private List<Message> warnings = new ArrayList<>();
	private List<Message> infos = new ArrayList<>();
	
	public void error(Message message) {
		errors.add(message);
	}
	
	public void warn(Message message) {
		warnings.add(message);
	}
	
	public void info(Message message) {
		infos.add(message);
	}
	
	public List<Message> getInfos() {
		return infos;
	}
	
	public List<Message> getWarnings() {
		return warnings;
	}
	
	public List<Message> getErrors() {
		return errors;
	}
	
	
}
