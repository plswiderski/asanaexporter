package io.bitbucket.pablo127.asanaexporter;

import java.net.MalformedURLException;
import java.net.URL;

public class UriBuilder {

    private URL url;

    public UriBuilder findTasks(String workspaceId, String assigneeId) throws MalformedURLException {
        url = new URL("https://app.asana.com/api/1.0/tasks?workspace=" + workspaceId + "&assignee=" + assigneeId
                + "&limit=100&opt_fields=completed_at,due_on,name,notes,projects,created_at,modified_at," +
                "assignee,parent");

        return this;
    }

    public UriBuilder findTasksByProject(String projectId) throws MalformedURLException {
        url = new URL("https://app.asana.com/api/1.0/tasks?project=" + projectId + "&limit=100&" +
                "opt_fields=completed_at,due_on,name,notes,projects,created_at,modified_at,assignee,parent");

        return this;
    }

    public UriBuilder uri(String uri) throws MalformedURLException {
        url = new URL(uri);

        return this;
    }

    public URL getUrl() {
        return url;
    }
}
