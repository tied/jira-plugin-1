package de.mtc.jira.wasaut;

public class PluginConstants {


	
	public final static String[] CF_FIELDS_NAMES = { "LE ID", "Project / Contract", "GET-ID", "BBS-Markup in %",
			"Site Area", "Site-Name", "BBS-Department", "Charging information", "Region", "Created By", "Modified By",
			"Item Type", "Path" };
	
	public final static String[] PROJECTS = new String[] {"WASAUT", "WASWKOWMA"};
	
	public final static String DEFAULT_QUERY = "project in (WASAUT,WASWKOWMA) and status != closed";

}
