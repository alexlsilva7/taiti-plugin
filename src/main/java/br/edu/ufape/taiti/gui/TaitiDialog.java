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
    private boolean servicesReady = false;

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

            // Iterate through returned scenarios
            for (LinkedHashMap<String, Serializable> scenario : selectedTask.getScenarios()) {
                String filePath = (String) scenario.get("path");
                List<Integer> lines = (List<Integer>) scenario.get("lines");

                // Create ScenarioTestInformation objects for each scenario line
                for (int line : lines) {
                    scenarios.add(new ScenarioTestInformation(filePath, line));
                }
            }

            // Load the converted scenarios in MainPanel
            mainPanel.loadExistingScenarios(scenarios);
        }
    }

    //getMainPanel
    public MainPanel getMainPanel() {
        return mainPanel;
    }
    private void prepareServices() {
        TaitiSettingsState settings = TaitiSettingsState.getInstance(project);
        settings.retrieveStoredCredentials(project).thenRun(() -> {
            try {
                taiti = new TaitiTool(project);
                pivotalTracker = new PivotalTracker(settings.getToken(), settings.getPivotalURL(), project);
                servicesReady = true;
            } catch (Exception e) {
                JOptionPane.showMessageDialog(getRootPane(),
                    "Error initializing services: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return mainPanel.getRootPanel();
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (!servicesReady) {
            return new ValidationInfo("The services are not ready yet. Please wait.", table);
        }
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
            JOptionPane.showMessageDialog(getRootPane(),
                "Error creating file: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        } catch (HttpException e) {
            JOptionPane.showMessageDialog(getRootPane(),
                "Communication error: " + e.getStatusText() + " - " + e.getStatusNumber(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
            taiti.deleteScenariosFile();
        } catch (InterruptedException e) {
            JOptionPane.showMessageDialog(getRootPane(),
                "Operation interrupted: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
            throw new RuntimeException(e);
        }
    }
}