package br.edu.ufape.taiti.settings;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Classe responsável por criar a interface gráfica na configuração do plugin, disponível em File > Settings > TAITI.
 */
public class TaitiSettingsComponent {

    private final JPanel mainPanel;
    private final JBTextField pivotalURLText;
    private final JBPasswordField pivotalToken;

    private final JBTextField githubURLText;
    private final JBTextField scenariosFolder;
    private final JBTextField stepDefinitionsFolder;
    private final JBTextField unityTestFolder;
    private final JBCheckBox structuralDependenciesCheckBox;
    private final JBCheckBox logicalDependenciesCheckBox;
    
    // Painel para o campo de token e botão de teste
    private final JPanel tokenPanel = new JPanel(new BorderLayout(5, 0));

    public TaitiSettingsComponent() {
        pivotalURLText = new JBTextField();
        pivotalToken = new JBPasswordField();
        githubURLText = new JBTextField();

        scenariosFolder = new JBTextField("features");
        stepDefinitionsFolder = new JBTextField("features/step_definitions");
        unityTestFolder = new JBTextField("spec");

        structuralDependenciesCheckBox = new JBCheckBox("Including structural dependencies between files");
        logicalDependenciesCheckBox = new JBCheckBox("Including logical dependencies between files");

        // Configurar o campo de token dentro do tokenPanel
        tokenPanel.add(pivotalToken, BorderLayout.CENTER);
        
        mainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JLabel("<html><b>Project settings</b></html>"),new JSeparator(),0)
                .addVerticalGap(10)
                .addLabeledComponent(new JBLabel("GitHub URL: "), githubURLText, 1, false)
                .addLabeledComponent(new JBLabel("PivotalTracker URL: "), pivotalURLText, 1, false)
                .addLabeledComponent(new JBLabel("PivotalTracker token: "), tokenPanel, 1, false)
                .addVerticalGap(10)
                .addLabeledComponent(new JLabel("<html><b>Test settings</b></html>"),new JSeparator(),0)
                .addVerticalGap(10)
                .addLabeledComponent(new JBLabel("Scenarios folder: "), scenariosFolder, 1, false)
                .addLabeledComponent(new JBLabel("Step definitions folder: "), stepDefinitionsFolder, 1, false)
                .addLabeledComponent(new JBLabel("Unity tests folder: "), unityTestFolder, 1, false)
                .addVerticalGap(10)
                .addLabeledComponent(new JLabel("<html><b>Code analysis settings</b></html>"), new JSeparator(), 0)
                .addVerticalGap(10)
                .addComponent(structuralDependenciesCheckBox)
                .addComponent(logicalDependenciesCheckBox)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();

    }
    
    /**
     * Adiciona o botão de teste ao lado do campo de token
     * 
     * @param button O botão de teste de conexão
     */
    public void addTestConnectionButton(JButton button) {
        tokenPanel.add(button, BorderLayout.EAST);
    }

    public JComponent getPreferredFocusedComponent() {
        return pivotalURLText;
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    @NotNull
    public String getPivotalURLText() {
        return pivotalURLText.getText();
    }

    public void setPivotalURLText(@NotNull String text) {
        pivotalURLText.setText(text);
    }
    @NotNull
    public String getPivotalToken() {
        return String.valueOf(pivotalToken.getPassword());
    }

    public void setPivotalToken(@NotNull String text) {
        pivotalToken.setText(text);
    }
    @NotNull
    public String getGithubURLText() {
        return githubURLText.getText();
    }
    public void setGithubURLText(@NotNull String text) {
        githubURLText.setText(text);
    }
    @NotNull
    public String getScenariosFolder() {
        return scenariosFolder.getText();
    }
    public void setScenariosFolder(@NotNull String text) {
        scenariosFolder.setText(text);
    }
    @NotNull
    public String getStepDefinitionsFolder() {
        return stepDefinitionsFolder.getText();
    }
    public void setStepDefinitionsFolder(@NotNull String text) {
        stepDefinitionsFolder.setText(text);
    }
    @NotNull
    public String getUnityTestFolder() {
        return unityTestFolder.getText();
    }
    public void setUnityTestFolder(@NotNull String text) {
        unityTestFolder.setText(text);
    }

    public boolean isStructuralDependenciesEnabled() {
        return structuralDependenciesCheckBox.isSelected();
    }

    public void setStructuralDependenciesEnabled(boolean enabled) {
        structuralDependenciesCheckBox.setSelected(enabled);
    }

    public boolean isLogicalDependenciesEnabled() {
        return logicalDependenciesCheckBox.isSelected();
    }

    public void setLogicalDependenciesEnabled(boolean enabled) {
        logicalDependenciesCheckBox.setSelected(enabled);
    }

}

