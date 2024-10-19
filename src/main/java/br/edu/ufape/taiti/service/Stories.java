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
        try {
            ownerID = pivotalTracker.getPersonId();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void clearLists() {
        myUnstartedStories.clear();
        otherPendingStories.clear();
    }

    public void startList(ProgressIndicator indicator) {
        clearLists();

        try {
            JSONArray plannedStories = pivotalTracker.getPlannedStories();
            for (int i = 0; i < plannedStories.length(); i++) {
                if (indicator.isCanceled()) {
                    break;
                }
                indicator.setFraction((double) i / plannedStories.length());
                indicator.setText("Processando story " + (i + 1) + " de " + plannedStories.length());

                JSONObject obj = plannedStories.getJSONObject(i);
                int taskId = obj.getInt("id");

                JSONObject taitiComment = pivotalTracker.getTaitiComment(pivotalTracker.getComments(String.valueOf(taskId)));
                //Seleciono apenas as tasks que contem o arquivo [TAITI] Scenarios, ou seja, que já foram adicionados
                if (taitiComment != null && taitiComment.getString("text").equals("[TAITI] Scenarios")) {
                    Task plannedStory = new Task(obj, pivotalTracker, project);
                    ArrayList<LinkedHashMap<String, Serializable>> tests = plannedStory.getScenarios();
                    int idTeste = plannedStory.getId();

                    // Aqui está o processamento pesado
                    TodoTask todoTask = new TodoTask(githubURL, idTeste, tests);
                    PlannedTask plannedTask = todoTask.generateTaskForConflictAnalysis();
                    plannedStory.setiTesk(plannedTask);

                    String state = plannedStory.getState();
                    int plannedStoryOwnerID = plannedStory.getOwnerID();

                    // Adiciona às listas de forma thread-safe
                    if (state.equals("unstarted") && plannedStoryOwnerID == ownerID) {
                        //Adiciono a uma lista as MINHAS tasks que ainda não começaram
                        synchronized (myUnstartedStories) {
                            myUnstartedStories.add(plannedStory);
                        }
                    } else if ((state.equals("started") || state.equals("unstarted"))
                            && plannedStoryOwnerID != ownerID) {
                        //Adiciono a uma lista as tasks que já começaram de OUTROS membros
                        synchronized (otherPendingStories) {
                            otherPendingStories.add(plannedStory);
                        }
                    }
                }
            }
        } catch (HttpException | InterruptedException | IOException  | CloningRepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Task> getMyUnstartedStories() {
        return myUnstartedStories;
    }

    public List<Task> getOtherPendingStories() {
        return otherPendingStories;
    }


}
