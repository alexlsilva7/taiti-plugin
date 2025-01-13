package br.edu.ufape.taiti.gui;

import br.edu.ufape.taiti.exceptions.HttpException;
import br.edu.ufape.taiti.gui.taskbar.LoadingScreen;
import br.edu.ufape.taiti.gui.taskbar.TaskBarGUI;
import br.edu.ufape.taiti.service.PivotalTracker;
import br.edu.ufape.taiti.service.Task;
import br.edu.ufape.taiti.settings.TaitiSettingsState;
import br.edu.ufape.taiti.tool.ScenarioTestInformation;
import br.edu.ufape.taiti.tool.TaitiTool;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.internal.StringUtil;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;


public class TaitiDialog extends DialogWrapper {

    private final MainPanel mainPanel;
    private final Task selectedTask;
    private final JBTable table;
    private final TaskBarGUI taskBarGUI;

    private PivotalTracker pivotalTracker;
    private TaitiTool taiti;

    private final Project project;
    private boolean executed = false;

    public TaitiDialog(Project project, TaskBarGUI taskBarGUI,  Task selectedTask) {
        super(true);

        this.taskBarGUI = taskBarGUI;
        this.project = project;
        this.selectedTask = selectedTask;

        this.mainPanel = new MainPanel(project);
        this.table = mainPanel.getTable();

        prepareServices();

        setTitle("TAITIr - Add Tests to: " + selectedTask.getName());
        setSize(1000,810);
        init();

        if (selectedTask.hasScenarios()) {
            List<ScenarioTestInformation> scenarios = new ArrayList<>();

            // Iterar pelos cenários retornados
            for (LinkedHashMap<String, Serializable> scenario : selectedTask.getScenarios()) {
                String filePath = (String) scenario.get("path");
                List<Integer> lines = (List<Integer>) scenario.get("lines");

                // Criar objetos ScenarioTestInformation para cada linha do cenário
                for (int line : lines) {
                    scenarios.add(new ScenarioTestInformation(filePath, line));
                }
            }

            // Carregar os cenários convertidos no MainPanel
            mainPanel.loadExistingScenarios(scenarios);
        }
    }

    //getMainPanel
    public MainPanel getMainPanel() {
        return mainPanel;
    }
    private void prepareServices() {
        TaitiSettingsState settings = TaitiSettingsState.getInstance(project);
        settings.retrieveStoredCredentials(project);

        taiti = new TaitiTool(project);
        pivotalTracker = new PivotalTracker(settings.getToken(), settings.getPivotalURL(), project);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return mainPanel.getRootPanel();
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (table.getRowCount() == 1) {
            return new ValidationInfo("Select at least one scenario.", table);
        }
        return null;
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{getOKAction(), getCancelAction()};
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();

        String taskID = String.valueOf(selectedTask.getId());

        try {
            File file = taiti.createScenariosFile(mainPanel.getScenarios());
            pivotalTracker.saveScenarios(file, taskID);
            LoadingScreen loading = taskBarGUI.getLoading();
            taskBarGUI.changeJpanel(loading);
            taskBarGUI.refresh();
            taiti.deleteScenariosFile();
        } catch (IOException e) {
            System.out.println("Erro ao criar o arquivo!");
        } catch (HttpException e) {
            System.out.println(e.getStatusText() + " - " + e.getStatusNumber());
            taiti.deleteScenariosFile();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}