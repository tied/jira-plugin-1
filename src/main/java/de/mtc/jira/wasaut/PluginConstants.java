package de.mtc.jira.wasaut;

import java.util.Arrays;
import java.util.List;

import com.atlassian.jira.project.Project;

public class PluginConstants {
	
	
	public final static String[] CF_FIELDS_NAMES = { "LE ID", "Project / Contract", "GET-ID", "BBS-Markup in %",
			"Site Area", "Site-Name", "BBS-Department", "Charging information", "Region", "Created By", "Modified By",
			"Item Type", "Path" };
	
	public final static String[] PROJECTS = new String[] {"WASAUT", "WASWKOWMA"};
	
	public final static String DEFAULT_QUERY = "project in (WASAUT,WASWKOWMA) and status != closed";

	public final static List<String> RELEVANT_FIELD_NAMES = Arrays.asList("Site Area", "Site-Name", "BBS-Department", "Charging information");
	
	public static boolean isRelevantProject(Project project) {
		String key = project.getKey();
		for(String str : PROJECTS) {
			if(key.equals(str)) {
				return true;
			}
		}
		return false;
	}
	
}
