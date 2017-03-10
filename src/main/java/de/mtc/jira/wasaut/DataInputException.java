package de.mtc.jira.wasaut;

public class DataInputException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public DataInputException(String message) {
		super(message);
	}

	public DataInputException(String message, Throwable e) {
		super(message, e);
	}
	
}
