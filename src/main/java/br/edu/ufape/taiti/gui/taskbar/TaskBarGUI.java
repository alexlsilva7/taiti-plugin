    package br.edu.ufape.taiti.gui.taskbar;


    import br.edu.ufape.taiti.exceptions.HttpException;
    import br.edu.ufape.taiti.gui.TaitiDialog;
    import br.edu.ufape.taiti.gui.conflicts.ConflictsGUI;
    import br.edu.ufape.taiti.service.PivotalTracker;
    import br.edu.ufape.taiti.service.Stories;
    import br.edu.ufape.taiti.service.Task;
    import br.edu.ufape.taiti.settings.TaitiSettingsState;
    import br.ufpe.cin.tan.conflict.ConflictAnalyzer;
    import br.ufpe.cin.tan.conflict.PlannedTask;
    import com.intellij.openapi.application.ApplicationManager;
    import com.intellij.openapi.progress.ProgressIndicator;
    import com.intellij.openapi.progress.ProgressManager;
    import com.intellij.openapi.project.Project;
    import com.intellij.openapi.wm.ToolWindow;
    import com.intellij.openapi.wm.ToolWindowManager;
    import com.intellij.ui.JBColor;
    import org.jetbrains.annotations.NotNull;

    import javax.swing.*;
    import javax.swing.table.DefaultTableCellRenderer;

    import javax.swing.table.DefaultTableModel;
    import javax.swing.table.TableColumnModel;
    import javax.swing.table.TableRowSorter;

    import java.awt.*;
    import java.awt.event.*;
    import java.io.IOException;
    import java.util.ArrayList;
    import java.util.List;
    import java.util.concurrent.Executors;
    import java.util.concurrent.ScheduledExecutorService;
    import java.util.concurrent.TimeUnit;


    public class TaskBarGUI {

        private final DefaultTableModel modelo1;
        private final DefaultTableModel modelo2;
        private JPanel TaskBar;
        private JPanel buttonsPanel;
        private JButton refreshButton;
    //    private JButton addButton;
        private JTextField txtSearch;

        private JTable unstartedTable;
        private JTable startedTable;
        private JPanel tables;
        private JPanel content;

        private final ArrayList<Task> myUnstartedStoriesList;

        private final ArrayList<Task> otherPendingStoriesList; // Pending = started + unstarted

        private final Project project;
        static public ConflictAnalyzer conflictAnalyzer;
        private final LoadingScreen loading;

        public TaskBarGUI(ToolWindow toolWindow, Project project) {

            this.project = project;
            conflictAnalyzer = new ConflictAnalyzer();

            // Inicializa os componentes
            loading = new LoadingScreen();
            content = new JPanel();
            content.setLayout(new BorderLayout());

            content.add(loading, BorderLayout.CENTER);

            addPlaceHolderStyle(txtSearch);
            myUnstartedStoriesList = new ArrayList<>();
            otherPendingStoriesList = new ArrayList<>();

            modelo1 = new DefaultTableModel(null, new String[]{"<html><b>My unstarted tasks</b></html>", "<html><b>Conflict Rate</b></html>"}) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false; // Tornar todas as células não editáveis
                }
            };
            unstartedTable.setModel(modelo1);

            // Definir a largura da segunda coluna como 20 pixels
            unstartedTable.getColumnModel().getColumn(1).setMaxWidth(100);

            //centralizar os numeros de Scenarios
            TableColumnModel columns = unstartedTable.getColumnModel();
            DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
            centerRenderer.setHorizontalAlignment(JLabel.CENTER);
            columns.getColumn(1).setCellRenderer(centerRenderer);

            modelo2 = new DefaultTableModel(null, new String[]{"<html><b>Potential conflict-inducing tasks</b></html>"}) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false; // Tornar todas as células não editáveis
                }
            };
            startedTable.setModel(modelo2);

            refreshButton.addActionListener(e -> {
                changeJpanel(loading);
                refresh();
            });

            txtSearch.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    super.keyReleased(e);
                    TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modelo1);
                    unstartedTable.setRowSorter(sorter);
                    sorter.setRowFilter(RowFilter.regexFilter(txtSearch.getText()));

                    TableRowSorter<DefaultTableModel> sorter2 = new TableRowSorter<>(modelo2);
                    startedTable.setRowSorter(sorter2);
                    sorter2.setRowFilter(RowFilter.regexFilter(txtSearch.getText()));
                }
            });

            txtSearch.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    super.focusGained(e);
                    txtSearch.setText(null);
                    txtSearch.requestFocus();
                    //remove placeholder style
                    removePlaceHolderStyle(txtSearch);
                }
            });
            txtSearch.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    super.focusLost(e);
                    if (txtSearch.getText().length() == 0) {
                        addPlaceHolderStyle(txtSearch);
                        txtSearch.setText("Search by task name");
                    }
                }
            });

            /*
             * Este método pega a referência ao projeto atualmente aberto no IntelliJ
             * e cria um objeto TaitiDialog, responsável por mostrar a janela da aplicação.
             */
    //        addButton.addActionListener(e -> {
    //            TaitiDialog taitiDialog = new TaitiDialog(project, this);
    //            taitiDialog.show();
    //        });
            //TODO: renomear tabelas
            unstartedTable.addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int row = unstartedTable.rowAtPoint(e.getPoint());
                    int column = unstartedTable.columnAtPoint(e.getPoint());
                    if (row > -1 && column > -1) {
                        boolean hasScenarios = myUnstartedStoriesList.get(row).hasScenarios();
                        if (hasScenarios) {
                            unstartedTable.setToolTipText("<html>" +
                                    "Task Name: " + myUnstartedStoriesList.get(row).getStoryName() + "<br>" +
                                    "Conflict Rate: " + myUnstartedStoriesList.get(row).getConflictRate() + "%<br>" +
                                    "Owner: " + myUnstartedStoriesList.get(row).getPersonName() + "<br>" +
                                    "</html>");
                        }else{
                            unstartedTable.setToolTipText("<html>" +
                                    "Task Name: " + myUnstartedStoriesList.get(row).getStoryName() + "<br>" +
                                    "Owner: " + myUnstartedStoriesList.get(row).getPersonName() + "<br>" +
                                    "Please add tests to calculate the conflict rate." +
                                    "</html>");
                        }
                    } else {
                        unstartedTable.setToolTipText(null);
                    }

                }
            });

            startedTable.addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int row = startedTable.rowAtPoint(e.getPoint());
                    int column = startedTable.columnAtPoint(e.getPoint());
                    if (row > -1 && column > -1) {
                        startedTable.setToolTipText("<html>" + otherPendingStoriesList.get(row).getStoryName() +
                                "<br>TaskID: #" + otherPendingStoriesList.get(row).getId() +
                                "<br>Owner: " + otherPendingStoriesList.get(row).getPersonName() + "</html>");
                    } else {
                        startedTable.setToolTipText(null);
                    }
                }
            });

            unstartedTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) { // clique duplo
                        int index = unstartedTable.getSelectedRow();
                        if (index < 0 || index >= myUnstartedStoriesList.size()) {
                            return;
                        }
                        Task task = myUnstartedStoriesList.get(index);

                        if (!task.hasScenarios()) {
                            // Agora passamos a task selecionada para o TaitiDialog
                            TaitiDialog taitiDialog = new TaitiDialog(project, TaskBarGUI.this, task);
                            taitiDialog.show();
                        } else {
                            // Lógica pré-existente para exibir conflitos
                            ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
                            ToolWindow myToolWindow = toolWindowManager.getToolWindow("Conflicts");

                            String text = "Conflict table for task \"" + task.getName() + "\" which contains "
                                    + task.getConflictRate() + "% conflict rate.";

                            ConflictsGUI.setLabel(text);
                            ConflictsGUI.fillTable(task, conflictAnalyzer, getOtherPendingStoriesList());

                            if (myToolWindow != null) {
                                myToolWindow.show(null);
                            }
                        }
                    }
                }
            });

            unstartedTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    // Detectar o botão direito do mouse (clique simples no Windows é BUTTON3)
                    if (e.isPopupTrigger()) {
                        showPopup(e);
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    // Em alguns sistemas (como macOS), o popup é disparado no mouseReleased.
                    if (e.isPopupTrigger()) {
                        showPopup(e);
                    }
                }

                private void showPopup(MouseEvent e) {
                    int row = unstartedTable.rowAtPoint(e.getPoint());
                    if (row < 0 || row >= myUnstartedStoriesList.size()) {
                        return;
                    }

                    // Seleciona a linha clicada (para evidenciar visualmente)
                    unstartedTable.setRowSelectionInterval(row, row);

                    Task task = myUnstartedStoriesList.get(row);

                    // Verifica se a tarefa possui cenários
                    if (task.hasScenarios()) {
                        // Criar o menu de contexto
                        JPopupMenu popupMenu = new JPopupMenu();

                        JMenuItem editTestsItem = new JMenuItem("Edit tests");
                        editTestsItem.addActionListener(evt -> {
                            // Abre o TaitiDialog para edição da mesma forma que ao adicionar,
                            // porém agora carregando os cenários existentes.
                            TaitiDialog taitiDialog = new TaitiDialog(project, TaskBarGUI.this, task);

                            // Carrega os cenários existentes no painel principal
                            // Aqui assumimos que a Task possui um método para converter seus cenários
                            // em uma lista de ScenarioTestInformation.


                            taitiDialog.show();
                            taitiDialog.getMainPanel().loadExistingScenarios(task.toScenarioTestInformationList());
                        });
                        popupMenu.add(editTestsItem);

                        JMenuItem removeTestsItem = new JMenuItem("Remove tests");
                        removeTestsItem.addActionListener(evt -> {
                            TaitiSettingsState settings = TaitiSettingsState.getInstance(project);
                            settings.retrieveStoredCredentials(project);
                            PivotalTracker pivotalTracker = new PivotalTracker(settings.getToken(), settings.getPivotalURL(), project);
                            try {
                                pivotalTracker.deleteScenarios(String.valueOf(task.getId()));
                                refresh();
                            } catch (HttpException | InterruptedException | IOException e1) {
                                e1.printStackTrace();
                            }
                        });

                        popupMenu.add(removeTestsItem);

                        popupMenu.show(unstartedTable, e.getX(), e.getY());
                    }
                }
            });

            //configRefreshTask(); TODO: Desabilitado por questões de performance
            // Iniciar o processamento inicial em segundo plano
            configTaskList();
        }

        private void configRefreshTask() {
            /*
             * Essa parte é responsável pelo Refresh de tempo em tempo
             */
            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
            // Crie uma instância de Runnable que representa a tarefa a ser executada periodicamente
            Runnable refreshTask = () -> {
                changeJpanel(loading);
                refresh();
            };
            // Agende a tarefa para ser executada inicialmente após 15 minutos e repetidamente a cada 15 minutos
            executor.scheduleAtFixedRate(refreshTask, 15, 15, TimeUnit.MINUTES);
        }

        private void addPlaceHolderStyle(JTextField textField) {
            Font font = textField.getFont();
            font = font.deriveFont(Font.ITALIC);
            textField.setFont(font);
            textField.setForeground(JBColor.foreground()); //PlaceHolder font color
        }

        private void removePlaceHolderStyle(JTextField textField) {
            Font font = textField.getFont();
            font = font.deriveFont(Font.PLAIN);
            textField.setFont(font);
            textField.setForeground(JBColor.LIGHT_GRAY); //PlaceHolder font color
        }

        private void configTaskList() {
            TaitiSettingsState settings = TaitiSettingsState.getInstance(project);
            settings.retrieveStoredCredentials(project);
            PivotalTracker pivotalTracker = new PivotalTracker(settings.getToken(), settings.getPivotalURL(), project);

            if (pivotalTracker.checkStatus() == 200) {
                /**
                 * Primeiramente esvazio o array que contem as tasks para preenche-lo novamente com as informações mais recentes
                 */
                Stories plannedStories = new Stories(pivotalTracker, project, settings.getGithubURL());
                plannedStories.clearLists();
                ProgressManager.getInstance().run(new com.intellij.openapi.progress.Task.Backgroundable(project, "Carregando Stories", true) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        plannedStories.startList(indicator);
                    }

                    @Override
                    public void onFinished() {
                        limparListas();
                        ArrayList<PlannedTask> othersPlannedTaskArrayList = new ArrayList<>();
                        for (Task othersTask : plannedStories.getOtherPendingStories()) {
                            othersPlannedTaskArrayList.add(othersTask.getiTesk());
                        }
                        for (Task myUnstartedTask : plannedStories.getMyUnstartedStories()) {
                            double conflictRate = conflictAnalyzer.meanRelativeConflictRiskForTasks(myUnstartedTask.getiTesk(), othersPlannedTaskArrayList);
                            double formattedConflictRate = Math.round(conflictRate * 100.0);
                            myUnstartedTask.setConflictRate(formattedConflictRate);
                        }

                        // Add the unstarted stories to the main list first
                        List<Task> myUnstartedStoriesWithNoScenarios = plannedStories.getMyUnstartedStories();
                        myUnstartedStoriesWithNoScenarios.addAll(plannedStories.getNoScenarioTasks());


                        updateMyUnstartedStoriesList(myUnstartedStoriesWithNoScenarios, myUnstartedStoriesList, modelo1);
                        // Add the started stories to the main list
                        updateOtherPendingStoriesList(plannedStories.getOtherPendingStories(), otherPendingStoriesList, modelo2);

                        changeJpanel(TaskBar);

                        // Atualizar os modelos de tabela
                        modelo1.fireTableDataChanged();
                        modelo2.fireTableDataChanged();

                        // Revalidar e repintar as tabelas
                        unstartedTable.revalidate();
                        unstartedTable.repaint();
                        startedTable.revalidate();
                        startedTable.repaint();
                    }

                    @Override
                    public void onCancel() {
                        super.onCancel();
                        limparListas();

                        changeJpanel(TaskBar);
                        ApplicationManager.getApplication().invokeLater(() -> {
                            JOptionPane.showMessageDialog(content, "Processamento cancelado pelo usuario.");
                        });
                    }
                });
            }
        }

        private void limparListas() {
            myUnstartedStoriesList.clear();
            otherPendingStoriesList.clear();

            modelo1.setRowCount(0);
            modelo2.setRowCount(0);
        }

        public void changeJpanel(JPanel panel) {
            System.out.println("Change JPanel"+ panel.getName());
            if (content.getComponent(0) != panel && content != null) {
                for(Component c : content.getComponents()){
                    if(c != null){
                        content.remove(c);
                    }
                }
                content.add(panel, BorderLayout.CENTER); // Adiciona o novo painel
                // Revalida e redesenha o conteúdo
                content.revalidate();
                content.repaint();
            }
        }

        public void refresh() {
            configTaskList();
        }

        private void updateMyUnstartedStoriesList(List<Task> stories, ArrayList<Task> storiesList, DefaultTableModel model) {
            for (Task story : stories) {
                storiesList.add(story);
                String storyName = truncateStoryName(story.getStoryName());
                String conflictRateStr;

                if (story.hasScenarios()) {
                    conflictRateStr = story.getConflictRate() > 0 ? story.getConflictRate() + "%" : "0%";
                } else {
                    conflictRateStr = "Add tests";
                }

                model.addRow(new Object[]{storyName, conflictRateStr});
            }
        }

        private void updateOtherPendingStoriesList(List<Task> stories, ArrayList<Task> storiesList, DefaultTableModel model) {
            for (Task story : stories) {
                storiesList.add(story);
                String storyName = truncateStoryName(story.getStoryName());
                model.addRow(new Object[]{storyName});
            }
        }

        // Função para limitar o texto das tasks na TaskList
        private String truncateStoryName(String storyName) {
            if (storyName.length() > 50) {
                storyName = String.format("%s...", storyName.substring(0, 50));
            }
            return storyName;
        }

        public ArrayList<Task> getmyUnstartedStoriesList() {
            return myUnstartedStoriesList;
        }

        public ArrayList<Task> getOtherPendingStoriesList() {
            return otherPendingStoriesList;
        }

        public JPanel getContent() {
            return content;
        }

        public LoadingScreen getLoading() {
            return loading;
        }
    }
