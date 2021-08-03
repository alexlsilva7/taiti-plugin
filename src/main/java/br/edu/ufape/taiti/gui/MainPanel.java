package br.edu.ufape.taiti.gui;

import br.edu.ufape.taiti.gui.fileview.*;
import br.edu.ufape.taiti.gui.tree.TaitiTree;
import br.edu.ufape.taiti.gui.tree.TaitiTreeFileNode;
import br.edu.ufape.taiti.gui.table.TestRow;
import br.edu.ufape.taiti.gui.table.TestsTableModel;
import br.edu.ufape.taiti.gui.table.TestsTableRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.table.JBTable;
import com.intellij.uiDesigner.core.AbstractLayout;
import com.intellij.util.ui.GridBag;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Scanner;

public class MainPanel {
    // panels
    private JPanel mainPanel;
    private JPanel treePanel;
    private JPanel rightPanel;
    private JPanel centerPanel;
    private JPanel inputPanel;
    private JPanel tablePanel;

    private JSplitPane splitPane;

    // components
    private TaitiTree tree;

    private JLabel labelGithubURL;
    private JLabel labelPivotalURL;
    private JLabel labelTaskID;

    private JTextField textGithubURL;
    private JTextField textPivotalURL;
    private JTextField textTaskID;

    private JBTable table;
    private TestsTableModel tableModel;

    private FeatureFileView featureFileView;
    private FeatureFileViewModel featureFileViewModel;

    private final ArrayList<ScenarioTestInformation> scenarios;
    private final RepositoryOpenFeatureFile repositoryOpenFeatureFile;

    public MainPanel() {
        scenarios = new ArrayList<>();
        repositoryOpenFeatureFile = new RepositoryOpenFeatureFile();

        configurePanels();
        configureTree();
        configureInputPanel();
        initTable();
        initCenterPanel();
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public ArrayList<ScenarioTestInformation> getScenarios() {
        return scenarios;
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
                e.printStackTrace();
            }

            openFeatureFile = new OpenFeatureFile(file, fileLines);
            repositoryOpenFeatureFile.addFeatureFile(openFeatureFile);
        }

        featureFileViewModel = new FeatureFileViewModel(file, openFeatureFile.getFileLines(), scenarios, tableModel);
        featureFileView.setModel(featureFileViewModel);
        featureFileView.setTableWidth();
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

        centerPanel.setLayout(new BorderLayout());
        centerPanel.add(new JScrollPane(featureFileView), BorderLayout.CENTER);
    }

    private void initTable() {
        table = new JBTable();
        table.setShowGrid(false);
        table.getTableHeader().setResizingAllowed(false);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setFillsViewportHeight(true);

        tablePanel.setLayout(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0));
        tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);

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
                for (TestRow t : testRowsChecked) {
                    tableModel.removeRow(t);
                    tableModel.getRow(0).setCheckbox(false);
                    OpenFeatureFile openFeatureFile = repositoryOpenFeatureFile.getFeatureFile(t.getFile());
                    int deselectedLine = openFeatureFile.deselectLine(t.getTest());
                    featureFileViewModel.fireTableDataChanged();

                    scenarios.remove(new ScenarioTestInformation(t.getFile().getAbsolutePath(), deselectedLine));
                }
            }
        });

        JPanel btnPanel = new JPanel(new BorderLayout());
        btnPanel.add(removeScenarioBtn, BorderLayout.EAST);
        tablePanel.add(btnPanel, BorderLayout.NORTH);

        tableModel = new TestsTableModel();
        table.setModel(tableModel);

        tableModel.addRow(new TestRow(null, false, "Tests"));
        table.setRowHeight(0, 30);
        table.getColumnModel().getColumn(0).setPreferredWidth(30);
        table.getColumnModel().getColumn(1).setPreferredWidth(350);
        table.getColumnModel().getColumn(0).setCellRenderer(new TestsTableRenderer());
        table.getColumnModel().getColumn(1).setCellRenderer(new TestsTableRenderer());
        table.getTableHeader().setUI(null);
    }

    private void configureTree() {
        // TODO: pegar caminho e nome do projeto dinamicamente
        String projectPath = "C:\\Users\\usuario\\Projects\\diaspora";
        String projectName = "diaspora";

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
            DefaultMutableTreeNode featureNode = new DefaultMutableTreeNode(featureDirectory.getName());
            rootNode.add(featureNode);
            tree.addNodesToTree(featureDirectory.getAbsolutePath(), featureNode);
        } else {
            tree.getEmptyText().setText("Could not find feature directory");
        }

        treePanel.setLayout(new BorderLayout());
        treePanel.add(new JScrollPane(tree), BorderLayout.CENTER);
    }

    private void configureInputPanel() {
        JLabel title = new JLabel("Task Identification");
        title.setHorizontalAlignment(JLabel.CENTER);
        title.setFont(new Font(null, Font.PLAIN, 15));

        labelGithubURL = new JLabel("GitHub Project URL:");
        textGithubURL = new JTextField();

        labelPivotalURL = new JLabel("PivotalTracker Project URL:");
        textPivotalURL = new JTextField();

        labelTaskID = new JLabel("Task ID:");
        textTaskID = new JTextField();

        JPanel panel = new JPanel(new GridBagLayout());
        inputPanel.setLayout(new BorderLayout());
        inputPanel.add(title, BorderLayout.NORTH);
        inputPanel.add(panel, BorderLayout.CENTER);

        GridBag gb = new GridBag()
                .setDefaultInsets(0, 0, AbstractLayout.DEFAULT_VGAP, AbstractLayout.DEFAULT_HGAP)
                .setDefaultWeightX(1.0)
                .setDefaultFill(GridBagConstraints.HORIZONTAL)
                .setDefaultPaddingX(0, -25);

        panel.add(labelGithubURL, gb.nextLine().next().weightx(0.2));
        panel.add(textGithubURL, gb.next().weightx(0.8));

        panel.add(labelPivotalURL, gb.nextLine().next().weightx(0.2));
        panel.add(textPivotalURL, gb.next().weightx(0.8));

        panel.add(labelTaskID, gb.nextLine().next().weightx(0.2));
        panel.add(textTaskID, gb.next().weightx(0.6));

        panel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        Border border = BorderFactory.createCompoundBorder
                (BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
                BorderFactory.createEmptyBorder(0, 10, 30, 0));

        inputPanel.setBorder(border);
    }

    private void configurePanels() {
        mainPanel = new JPanel();
        treePanel = new JPanel();
        centerPanel = new JPanel();
        rightPanel = new JPanel();
        inputPanel = new JPanel();
        tablePanel = new JPanel();

        mainPanel.setLayout(new FlowLayout());
//        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setMinimumSize(new Dimension(1300, 720));

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, treePanel, centerPanel);
        splitPane.setDividerLocation(300);
        splitPane.setDividerSize(4);
        splitPane.setPreferredSize(new Dimension(900, 700));
        splitPane.setMinimumSize(new Dimension(900, 700));

        rightPanel.setLayout(new BorderLayout(0, 50));
        rightPanel.setPreferredSize(new Dimension(390, 700));
        rightPanel.setMinimumSize(new Dimension(390, 700));

//        GridBag gb = new GridBag()
//                .setDefaultInsets(0, 0, 0, 0)
//                .setDefaultWeightX(1.0)
//                .setDefaultFill(GridBagConstraints.VERTICAL);
//
//        mainPanel.add(splitPane, gb.nextLine().next().weightx(0.7));
//        mainPanel.add(rightPanel, gb.next().weightx(0.3));

        mainPanel.add(splitPane);
        mainPanel.add(rightPanel);
        rightPanel.add(inputPanel, BorderLayout.NORTH);
        rightPanel.add(tablePanel, BorderLayout.CENTER);
    }
}
