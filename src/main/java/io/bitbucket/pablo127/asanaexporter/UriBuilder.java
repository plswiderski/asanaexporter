package io.bitbucket.pablo127.asanaexporter;

import io.bitbucket.pablo127.asanaexporter.util.StringUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Supplier;

final class UriBuilder implements Supplier<URL>  {

    private static final String BASE_PATH = "https://app.asana.com/api/1.0/";

    private static final String TASKS_OPT_FIELDS = "completed_at,due_on,name,notes,projects,created_at,modified_at," +
            "assignee,parent,parent.name,recurrence,memberships.section.name,memberships.project.name,attachments,num_subtasks," +
            "assignee_section,assignee_section.name";

    private static final String PROJECTS_OPT_FIELDS = "archived,name,color,created_at,notes,modified_at,due_on,icon";

    private URL url;

    public UriBuilder findTasks(String workspaceId, String assigneeId, String modifiedSince) throws MalformedURLException {
        url = new URL(BASE_PATH + "tasks?workspace=" + workspaceId + "&assignee=" + assigneeId
                + StringUtil.emptyIfNullOrTransform(modifiedSince, s -> "&modified_since=" + s)
                + "&limit=100&opt_fields=" + TASKS_OPT_FIELDS);

        return this;
    }

    public UriBuilder findTasksByProject(String projectId, String modifiedSince) throws MalformedURLException {
        url = new URL(BASE_PATH + "tasks?project=" + projectId + "&limit=100"
                + StringUtil.emptyIfNullOrTransform(modifiedSince, s -> "&modified_since=" + s)
                + "&opt_fields=" + TASKS_OPT_FIELDS);

        return this;
    }

    public UriBuilder findSubtasks(String taskId, String modifiedSince) throws MalformedURLException {
        url = new URL(BASE_PATH + "tasks/"+taskId+"/subtasks?limit=100"
                + StringUtil.emptyIfNullOrTransform(modifiedSince, s -> "&modified_since=" + s)
                + "&opt_fields=" + TASKS_OPT_FIELDS);

        return this;
    }

    public UriBuilder findWorkspacesUsers(String workspaceId) throws MalformedURLException {
        url = new URL(BASE_PATH + "workspaces/" + workspaceId + "/users");

        return this;
    }

    public UriBuilder findProjects(String workspaceId) throws MalformedURLException {
        url = new URL(BASE_PATH + "projects?workspace=" + workspaceId + "&opt_fields=" + PROJECTS_OPT_FIELDS);

        return this;
    }

    public UriBuilder uri(String uri) throws MalformedURLException {
        url = new URL(uri);

        return this;
    }

    @Override
    public URL get() {
        return url;
    }
}
