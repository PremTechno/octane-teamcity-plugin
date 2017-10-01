 TeamCity server-side plugin

 This is an empty project to develop TeamCity plugin that operates on server-side only.

 1. Implement
 Put your implementing classes to "<artifactId>-server" module. Do not forget to update spring context file in 'main/resources/META-INF'. See TeamCity documentation for details.

 2. teamcity-ci-plugin-build
 Issue 'mvn package' command from the root project to teamcity-ci-plugin-build your plugin. Resulting package <artifactId>.zip will be placed in 'target' directory.
 
 3. Install
 To install the plugin, put zip archive to 'plugins' dir under TeamCity data directory and restart the server.

Certain versions of software accessible here may contain branding from Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017, the software is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the property of their respective owners.
