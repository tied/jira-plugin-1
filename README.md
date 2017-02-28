<h2>Jira Plugin for automatically managing the business requirements for WASAUT/WASWKOWMA projects.</h2>

<h3>Initialization</h3>

<h4>System Admin</h4>
* Register a new Listener in Jira. The class is <tt>de.mtc.jira.wasaut.IssueUpdateListener</tt>, the name is irrelevant.
* Enable debugging for project <tt>de.mtc.jira</tt> 

<h4>Project Admin</h4>
1. Create projects WASAUT and WASKOWMA.
2. In Jira, click on the wheel in the upper right corner and select <tt>Issues</tt>. In the left panel, 
select <tt>Custom Fields</tt> and then <tt>Add custom Field</tt>. in the <tt>Advanced</tt> section of the custom field 
popup, select the custom field 
with the name <tt>wasaut.custom.field.name</tt>, click <tt>Next</tt>. In the <tt>Name</tt> text field, enter <tt>Project / Contract</tt> 
(Note: the name must match exactly this pattern). After that, associate the new field with the WASAUT and WASKOWMA project screens.
3. Similarly, create four new costum fields, but of type <tt>Textfield Read Only</tt> (once again under the <tt>Advanced</tt> section). 
Name them exactly <tt>Site Area</tt>, <tt>Site-Name</tt>, <tt>BBS-Department</tt> and <tt>Charging information</tt> and assiciate 
them with the project screens.
4. To initially upload the information that defines the business logic, go to 
<tt>https://jira.mtc.berlin/plugins/servlet/upload</tt>. 
In the text area, copy-and-paste the (semicolon-separated) csv data as plain text, press <tt>Save</tt> and <tt>Commit</tt>. 
<br>
The csv file must exactly match the structure as in the example below:

<pre>
LE ID;Project / Contract;GET-ID;BBS-Markup in %;Site Area;Site-Name;BBS-Department;Charging information;Region;Created By;Modified By;Item Type;Path
0210;WebAS, TWM%ZA-0210-BHC%02, Intranet;;;Intranet;various;WebCM;TWM#ZA-0210-BHC#02;EMEA (ohne DE);Marcel Knoll;Marcel Knoll;Element;sites/000185/WebAgency/Lists/BBS Contracts
0210;WebAS, TWM%ZA-0210-BHC%01, Internet;10064724;;Internet;various;WebCM;TWM#ZA-0210-BHC#01;EMEA (ohne DE);Marcel Knoll;Marcel Knoll;Element;sites/000185/WebAgency/Lists/BBS Contracts
.................................
</pre>






