# Asana Exporter

## Build project
`gradlew build fatjar`

The jar is located in the folder `/build/libs`.

## Run
Run parsing with:

`java -jar $personalAccessToken`

personalAccessToken is an authorization token for your asana account (more: https://asana.com/developers/documentation/getting-started/auth#personal-access-token). You can generate it from asana settings panel, click on your logo (top right corner) in asana webpanel. Then select `My Profile Settings > Apps > Manage Developer Apps > Create New Personal Access Token`.
