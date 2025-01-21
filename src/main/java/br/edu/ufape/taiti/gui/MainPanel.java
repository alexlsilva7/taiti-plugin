package br.edu.ufape.taiti.gui;

import br.edu.ufape.taiti.gui.configuretask.fileview.*;
import br.edu.ufape.taiti.gui.configuretask.tree.TaitiTree;
import br.edu.ufape.taiti.gui.configuretask.tree.TaitiTreeFileNode;
import br.edu.ufape.taiti.gui.configuretask.table.TestRow;
import br.edu.ufape.taiti.gui.configuretask.table.TestsTableModel;
import br.edu.ufape.taiti.gui.configuretask.table.TestsTableRenderer;
import br.edu.ufape.taiti.tool.ScenarioTestInformation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;

import com.intellij.openapi.vfs.VirtualFile;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

public class MainPanel {
    private JPanel rootPanel;
    private JPanel centerPanel;
    private JPanel northPanel;
    private JPanel southPanel;
    private JPanel treePanel;

    private JSplitPane mainSplit;
    private JSplitPane leftSplit;

    private TaitiTree tree;

    private JBTable table;
    private TestsTableModel tableModel;
    private FeatureFileView featureFileView;
    private FeatureFileViewModel featureFileViewModel;

    private final ArrayList<ScenarioTestInformation> scenarios;
    private final RepositoryOpenFeatureFile repositoryOpenFeatureFile;

    private final Project project;

    public MainPanel(Project project) {
        this.project = project;
        scenarios = new ArrayList<>();
        repositoryOpenFeatureFile = new RepositoryOpenFeatureFile();

        configurePanels();
        configureTree();
        initTable();
        initCenterPanel();
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    public ArrayList<ScenarioTestInformation> getScenarios() {
        return scenarios;
    }

    public JBTable getTable() {
        return table;
    }

    private File findFeatureFile(Project project, String relativePath) {
        VirtualFile projectDir = ProjectUtil.guessProjectDir(project);
        if (projectDir == null) {
            System.out.println("Project directory not found");
            return null;
        }

        try {
            // Converter caminho relativo para o formato do sistema
            String normalizedPath = Paths.get(relativePath).normalize().toString();

            // Usar a API do IntelliJ para encontrar o arquivo
            VirtualFile virtualFile = projectDir.findFileByRelativePath(normalizedPath);

            if (virtualFile == null || !virtualFile.exists()) {
                // Tentar encontrar com match case insensitivo
                virtualFile = findFileCaseInsensitive(projectDir, normalizedPath);
            }

            if (virtualFile != null && virtualFile.exists()) {
                return new File(virtualFile.getPath());
            }

            System.out.println("File not found: " + normalizedPath);
            return null;

        } catch (InvalidPathException e) {
            System.out.println("Invalid path format: " + relativePath);
            return null;
        }
    }

    private VirtualFile findFileCaseInsensitive(VirtualFile baseDir, String relativePath) {
        String[] parts = relativePath.split("[\\\\/]");
        VirtualFile current = baseDir;

        for (String part : parts) {
            if (current == null) break;

            VirtualFile child = current.findChild(part); // Tenta match exato primeiro
            if (child == null) {
                // Busca case insensitive
                for (VirtualFile f : current.getChildren()) {
                    if (f.getName().equalsIgnoreCase(part)) {
                        child = f;
                        break;
                    }
                }
            }
            current = child;
        }
        return current;
    }

    private String getScenarioName(File file, int lineNumber) {
        try (Scanner scanner = new Scanner(new FileReader(file))) {
            int currentLine = 1;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                if (currentLine == lineNumber) {
                    if (line.trim().toLowerCase().startsWith("scenario")) {
                        return line.trim();
                    } else {
                        // se a linha 13 não for a linha do "Scenario", retorna a string bruta
                        return line.trim();
                    }
                }
                currentLine++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Scenario at line: " + lineNumber;
    }


    public void loadExistingScenarios(List<ScenarioTestInformation> existingScenarios) {
        Map<File, List<Integer>> scenariosByFile = new HashMap<>();

        for (ScenarioTestInformation scenario : existingScenarios) {
            File absoluteFile = findFeatureFile(this.project, scenario.getFilePath());
            int lineNumber = scenario.getLineNumber();
            if (absoluteFile != null) {
                scenariosByFile
                        .computeIfAbsent(absoluteFile, k -> new ArrayList<>())
                        .add(lineNumber);
            }
        }

        // Adiciona cenários na tabela e na lista de cenários
        for (Map.Entry<File, List<Integer>> entry : scenariosByFile.entrySet()) {
            File file = entry.getKey();
            List<Integer> lines = entry.getValue();

            for (int lineNumber : lines) {
                scenarios.add(new ScenarioTestInformation(file.getAbsolutePath(), lineNumber));
                String scenarioName = getScenarioName(file, lineNumber);
                tableModel.addRow(new TestRow(file, false, scenarioName, lineNumber));
            }

            tableModel.fireTableDataChanged();
        }
    }

    public void updateCenterPanel(File file) {
        String filePath = file.getAbsolutePath();
        String fileName = file.getName();
        OpenFeatureFile openFeatureFile;

        if (repositoryOpenFeatureFile.exists(file)) {
            openFeatureFile = repositoryOpenFeatureFile.getFeatureFile(file);
        } else {
            ArrayList<FileLine> fileLines = new ArrayList<>();
            Scanner scanner;
            try {
                scanner = new Scanner(new FileReader(filePath));
                int countLine = 1;
                fileLines.add(new FileLine(false, fileName, -1));
                fileLines.add(new FileLine(false, "", -1));
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    fileLines.add(new FileLine(false, line, countLine));
                    countLine++;
                }
            } catch (FileNotFoundException e) {
                fileLines.add(new FileLine(false, "Error loading file.", -1));
            }

            openFeatureFile = new OpenFeatureFile(file, fileLines);
            if (openFeatureFile.getFileLines().size() >= 2) {
                repositoryOpenFeatureFile.addFeatureFile(openFeatureFile);
            }
        }

        featureFileViewModel = new FeatureFileViewModel(file, openFeatureFile.getFileLines(), scenarios, tableModel);
        featureFileView.setModel(featureFileViewModel);
        featureFileView.setTableWidth(centerPanel.getWidth());
        featureFileView.setRowHeight(0, 30);

        featureFileView.getColumnModel().getColumn(0).setCellRenderer(new CheckBoxCellRenderer(file));
        featureFileView.getColumnModel().getColumn(0).setCellEditor(new CheckBoxEditor(new JCheckBox(), file));
        featureFileView.getColumnModel().getColumn(1).setCellRenderer(new FileLineRenderer(file));
        featureFileView.getTableHeader().setUI(null);
    }

    private void initCenterPanel() {
        featureFileView = new FeatureFileView();
        featureFileView.setShowGrid(false);
        featureFileView.getTableHeader().setResizingAllowed(false);
        featureFileView.getTableHeader().setReorderingAllowed(false);
        featureFileView.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        featureFileView.setDragEnabled(false);
        featureFileView.setRowSelectionAllowed(false);

        centerPanel.add(new JScrollPane(featureFileView), BorderLayout.CENTER);
    }

    private void initTable() {
        table = new JBTable();
        table.setShowGrid(false);
        table.getTableHeader().setResizingAllowed(false);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setFillsViewportHeight(true);
        table.setRowSelectionAllowed(false);

        southPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton removeScenarioBtn = new JButton("Remove");
        removeScenarioBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ArrayList<TestRow> testRowsChecked = new ArrayList<>();

                // catch all rows checked
                for (int r = 1; r < tableModel.getRowCount(); r++) {
                    if ((boolean) tableModel.getValueAt(r, 0)) {
                        String test = (String) tableModel.getValueAt(r, 1);
                        TestRow testRow = tableModel.findTestRow(test);
                        testRowsChecked.add(testRow);
                    }
                }
                // remove all rows checked
                tableModel.getRow(0).setCheckbox(false);
                for (TestRow t : testRowsChecked) {
                    tableModel.removeRow(t);
                    OpenFeatureFile openFeatureFile = repositoryOpenFeatureFile.getFeatureFile(t.getFile());
                    int deselectedLine = openFeatureFile.deselectLine(t.getTest());
                    featureFileViewModel.fireTableDataChanged();

                    scenarios.remove(new ScenarioTestInformation(t.getFile().getAbsolutePath(), deselectedLine));
                }
            }
        });

        JPanel btnPanel = new JPanel(new BorderLayout());
        btnPanel.add(removeScenarioBtn, BorderLayout.EAST);
        southPanel.add(btnPanel, BorderLayout.NORTH);

        tableModel = new TestsTableModel();
        table.setModel(tableModel);

        tableModel.addRow(new TestRow(null, false, "Tests", 0));
        table.setRowHeight(40);
        table.getColumnModel().getColumn(0).setPreferredWidth(30);
        table.getColumnModel().getColumn(1).setPreferredWidth(270);
        table.getColumnModel().getColumn(0).setCellRenderer(new TestsTableRenderer());
        table.getColumnModel().getColumn(1).setCellRenderer(new TestsTableRenderer());
        table.getTableHeader().setUI(null);
    }

    private void configureTree() {
        String projectPath = "";
        String projectName = "";

        VirtualFile projectDir = ProjectUtil.guessProjectDir(project);
        if (projectDir != null) {
            projectPath = projectDir.getPath();
            projectName = projectDir.getName();
        }

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(projectName);
        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);

        tree = new TaitiTree(treeModel);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

                    if (node == null) return;

                    Object nodeInfo = node.getUserObject();
                    if (node.isLeaf() && !node.getAllowsChildren()) {
                        TaitiTreeFileNode taitiTreeFileNode = (TaitiTreeFileNode) nodeInfo;
                        updateCenterPanel(taitiTreeFileNode.getFile());
                    }
                }
            }
        });

        File featureDirectory = tree.findFeatureDirectory(projectPath);
        if (featureDirectory != null) {

            // get every parent directory of features directory
            File parent = featureDirectory.getParentFile();
            ArrayList<DefaultMutableTreeNode> parentsNodes = new ArrayList<>();
            while (!parent.getName().equals(projectName)) {
                parentsNodes.add(new DefaultMutableTreeNode(parent.getName()));
                parent = parent.getParentFile();
            }

            // add every parent directory of feature directory to the tree
            tree.addParentsNodeToTree(parentsNodes, rootNode, parentsNodes.size() - 1);

            // add the feature directory to the tree
            DefaultMutableTreeNode featureNode = new DefaultMutableTreeNode(featureDirectory.getName());
            if (parentsNodes.size() > 0) {
                parentsNodes.get(0).add(featureNode);
            } else {
                rootNode.add(featureNode);
            }

            // populating the tree with the files into feature directory
            tree.addNodesToTree(featureDirectory, featureNode);
            treePanel.add(new JScrollPane(tree), BorderLayout.CENTER);
        } else {
            JLabel label = new JLabel("Could not find feature directory");
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setForeground(JBColor.gray);
            treePanel.add(label, BorderLayout.CENTER);
        }
    }

    private void configurePanels() {
        rootPanel.setBorder(null);
        centerPanel.setLayout(null);
        mainSplit.setBorder(null);
        leftSplit.setBorder(null);
        northPanel.setBorder(null);
        southPanel.setBorder(null);
        treePanel.setBorder(null);

        rootPanel.setBorder(BorderFactory.createLineBorder(JBColor.border()));
        centerPanel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, JBColor.border()));
        southPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor.border()));
        leftSplit.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, JBColor.border()));

        mainSplit.setDividerLocation(300);
        mainSplit.setDividerSize(2);
        leftSplit.setDividerLocation(400);
        leftSplit.setDividerSize(2);

        centerPanel.setLayout(new BorderLayout());
        centerPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                featureFileView.setTableWidth(e.getComponent().getWidth());
            }
        });
        treePanel.setLayout(new BorderLayout());
        southPanel.setLayout(new BorderLayout());
        southPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                table.getColumnModel().getColumn(1).setPreferredWidth(e.getComponent().getWidth() - 35);
            }
        });
    }
}