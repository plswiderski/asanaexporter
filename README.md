# AsanaExporter
Application for exporting your tasks from Asana. It allows to download all tasks that you have saved in Asana Cloud. With asana's export it is possible to get only 2000 tasks.

Tutorials for this tool are available on my blog: 
* https://medium.com/@pablo127/exporting-tasks-from-asana-for-backup-and-analytic-9780567e430f
* https://medium.com/@pablo127/asanaexporter-v1-1-new-features-30a60067f52b

## Build project
`gradlew build fatjar`

The jar is located in the folder `/build/libs`.

You can download built project from https://pablo127.bitbucket.io/asanaexporter/asanaexporter-all-1.1.0.jar

## Run
Run exporter by:

`java -jar asanaexporter-all-1.1.0.jar $personalAccessToken`

or

`java -jar asanaexporter-all-1.1.0.jar $personalAccessToken "YOUR WORKSPACE NAME"`

personalAccessToken is an authorization token for your asana account (more: https://asana.com/developers/documentation/getting-started/auth#personal-access-token). You can generate it from asana settings panel, click on your logo (top right corner) in asana webpanel. Then select `My Profile Settings > Apps > Manage Developer Apps > Create New Personal Access Token`. 

Remember to `Deauthorize` token that you do not use. It is safer to generate token before each usage of AsanaExporter.

### Results
The result is asanaTasks.csv document that consists of all exported tasks. Remember that this file is always overriden by each run of the AsanaExporter tool.

File lastModification.txt is created after each run. It contains the date of the last modification. Next run of tool will use that date to omit tasks that were modified before this date. If you would like to download all tasks — just remove the file.