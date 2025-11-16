# AsanaExporter
Application for exporting your tasks from Asana. It allows to download all tasks that you have saved in Asana Cloud. With asana's export it is possible to get only 2000 tasks.

Tutorials for this tool are available on my blog: 
* https://plswiderski.medium.com/exporting-tasks-from-asana-for-backup-and-analytic-9780567e430f
* https://plswiderski.medium.com/asanaexporter-v1-1-new-features-30a60067f52b

## Build project
`gradlew clean build fatjar`

The jar is located in the folder `/build/libs`.

You can download built project from https://github.com/plswiderski/asanaexporter/releases

Project is based on JVM 11. If you are using newer one, please build with JVM 11 by setting `JAVA_HOME` environment variable.

Built artifact can be run by any newer than JVM 11.

## Run
Run exporter by:

`java -jar asanaexporter-all-2.0.0.jar $personalAccessToken`

or

`java -jar asanaexporter-all-2.0.0.jar $personalAccessToken "YOUR WORKSPACE NAME" "MODE"`

`personalAccessToken` is an authorization token for your asana account (more: https://asana.com/developers/documentation/getting-started/auth#personal-access-token). You can generate it from asana settings panel, click on your logo (top right corner) in asana webpanel. Then select `My Profile Settings > Apps > Manage Developer Apps > Create New Personal Access Token`. 

`mode` is `BACKUP` or `RESTORE`. `BACKUP` is default one which can be omitted in the list of arguments. 

Remember to `Deauthorize` token that you do not use. It is safer to generate token before each usage of AsanaExporter.

### Results
The result is asanaTasks.csv document that consists of all exported tasks. Remember that this file is always overriden by each run of the AsanaExporter tool.

File `lastModification.txt` is created after each run. It contains the date of the last modification. Next run of tool will use that date to omit tasks that were modified before this date. If you would like to download all tasks — just remove the `lastModification.txt` file.


## Verification of restored results

The tool may not handle each case of task types or structure. Please perform verification procedure after restore.

1. Perform BACKUP on the workspace that was restored.
2. Use built-in comparator to check that backup file used in restore process contains consistent data as one from point 1.

Run verification by command:
```
java -cp asanaexporter-all-2.0.0.jar io.bitbucket.pablo127.asanaexporter.BackupsComparatorMain "pathToBaseCsvFile" "pathToCurrentCsvFile"
```

First pass path to the base one - file that restore was based on.
