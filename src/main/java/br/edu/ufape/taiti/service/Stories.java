package br.edu.ufape.taiti.service;

import br.edu.ufape.taiti.exceptions.HttpException;
import br.ufpe.cin.tan.analysis.task.TodoTask;
import br.ufpe.cin.tan.conflict.PlannedTask;
import br.ufpe.cin.tan.exception.CloningRepositoryException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class Stories {
    private final List<Task> myUnstartedStories;
    private final List<Task> otherPendingStories; // Pending = started + unstarted

    private final List<Task> noScenarioTasks;
    private final PivotalTracker pivotalTracker;
    private final int ownerID;
    private Project project;
    private String githubURL;

    public Stories(PivotalTracker pivotalTracker, Project project, String githubURL) {
        this.githubURL = githubURL;
        this.project = project;
        this.pivotalTracker = pivotalTracker;
        myUnstartedStories = new ArrayList<>();
        otherPendingStories = new ArrayList<>();
        noScenarioTasks = new ArrayList<>();
        try {
            ownerID = pivotalTracker.getPersonId();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void clearLists() {
        myUnstartedStories.clear();
        otherPendingStories.clear();
        noScenarioTasks.clear();
    }

    public void startList(ProgressIndicator indicator) {
        clearLists();

        try {
            JSONArray plannedStoriesArray = pivotalTracker.getPlannedStories();
            for (int i = 0; i < plannedStoriesArray.length(); i++) {
                if (indicator.isCanceled()) {
                    break;
                }
                indicator.setFraction((double) i / plannedStoriesArray.length());
                indicator.setText("Processando story " + (i + 1) + " de " + plannedStoriesArray.length());
                indicator.setText2("Nome da story: " + plannedStoriesArray.getJSONObject(i).getString("name"));

                JSONObject obj = plannedStoriesArray.getJSONObject(i);
                int taskId = obj.getInt("id");

                Task plannedStory = new Task(obj, pivotalTracker, project);

                String state = plannedStory.getState();
                int plannedStoryOwnerID = plannedStory.getOwnerID();

                boolean isUnstarted = "unstarted".equals(state);
                boolean isStarted = "started".equals(state);

                // Se a tarefa é do usuário e está "unstarted"
                if (isUnstarted && plannedStoryOwnerID == ownerID) {
                    // Tarefas do usuário não iniciadas
                    if (plannedStory.hasScenarios()) {
                        processPlannedStory(plannedStory);
                        synchronized (myUnstartedStories) {
                            myUnstartedStories.add(plannedStory);
                        }
                    } else {
                        // Sem cenários
                        synchronized (noScenarioTasks) {
                            noScenarioTasks.add(plannedStory);
                        }
                    }
                } else if ((isStarted || isUnstarted) && plannedStoryOwnerID != ownerID) {
                    // Tarefas de outros desenvolvedores (iniciadas ou não)
                    if (plannedStory.hasScenarios()) {
                        processPlannedStory(plannedStory);
                        synchronized (otherPendingStories) {
                            otherPendingStories.add(plannedStory);
                        }
                    } else {
                        // Sem cenários
                        synchronized (noScenarioTasks) {
                            noScenarioTasks.add(plannedStory);
                        }
                    }
                }
            }
        } catch (HttpException | InterruptedException | IOException | CloningRepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    private void processPlannedStory(Task plannedStory) throws CloningRepositoryException {
        if (plannedStory.hasScenarios()) {
            ArrayList<LinkedHashMap<String, Serializable>> tests = plannedStory.getScenarios();
            int idTeste = plannedStory.getId();

            TodoTask todoTask = new TodoTask(githubURL, idTeste, tests);
            PlannedTask plannedTask = todoTask.generateTaskForConflictAnalysis();
            plannedStory.setiTesk(plannedTask);
        }
    }

    public List<Task> getMyUnstartedStories() {
        return myUnstartedStories;
    }

    public List<Task> getOtherPendingStories() {
        return otherPendingStories;
    }

    public List<Task> getNoScenarioTasks() {
        return noScenarioTasks;
    }


}
