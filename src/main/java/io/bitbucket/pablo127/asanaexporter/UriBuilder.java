package io.bitbucket.pablo127.asanaexporter;

import java.net.MalformedURLException;
import java.net.URL;

final class UriBuilder {

    private static final String BASE_PATH = "https://app.asana.com/api/1.0/";

    private URL url;

    public UriBuilder findTasks(String workspaceId, String assigneeId) throws MalformedURLException {
        url = new URL(BASE_PATH + "tasks?workspace=" + workspaceId + "&assignee=" + assigneeId
                + "&limit=100&opt_fields=completed_at,due_on,name,notes,projects,created_at,modified_at," +
                "assignee,parent");

        return this;
    }

    public UriBuilder findTasksByProject(String projectId) throws MalformedURLException {
        url = new URL(BASE_PATH + "tasks?project=" + projectId + "&limit=100&" +
                "opt_fields=completed_at,due_on,name,notes,projects,created_at,modified_at,assignee,parent");

        return this;
    }

    public UriBuilder findWorkspacesUsers(String workspaceId) throws MalformedURLException {
        url = new URL(BASE_PATH + "workspaces/" + workspaceId + "/users");

        return this;
    }

    public UriBuilder findProjects(String workspaceId) throws MalformedURLException {
        url = new URL(BASE_PATH + "projects?workspace=" + workspaceId);

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
