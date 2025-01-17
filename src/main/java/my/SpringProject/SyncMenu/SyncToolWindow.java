package my.SpringProject.SyncMenu;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBList;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SyncToolWindow implements ToolWindowFactory, DumbAware {


    private Boolean runningStatus = false;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private List<String> list = new ArrayList<>();



    public void getKeys(ConcurrentHashMap<String, String> map){
        list.clear();
        Iterator<String> iterator = map.keys().asIterator();
        while(iterator.hasNext()){
            list.add(iterator.next());
        }
    }


    private void updateFileList(DefaultListModel<String> listModel, List<String> listData) {
        listModel.clear();
        for (String item : listData) {
            listModel.addElement(item);
        }
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

        Connecter syncService = ApplicationManager.getApplication().getService(Connecter.class);//Юзаем компонент из папки SyncMenu
//Создаём элементы панели
        JPanel panel = new JPanel();
        JButton syncButton = new JButton("Sync Data");
        JTextArea textArea = new JTextArea();
        JButton showButton = new JButton("ShowData");
        JButton saveButton = new JButton("Save");
        JButton AutoSyncButton = new JButton("AutoSync");

        // Добавляем JList для отображения названий файлов
        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> fileList = new JBList<>(listModel);
        JScrollPane listScrollPane = new JBScrollPane(fileList);

        // Добавляем JTextArea для отображения содержимого выбранного файла
        JTextArea fileContentTextArea = new JTextArea(10, 30);
        fileContentTextArea.setEditable(false);
        JScrollPane contentScrollPane = new JBScrollPane(fileContentTextArea);



        // Обработчик выбора элемента в JList
        fileList.addListSelectionListener(e -> {
            try {
                ConcurrentHashMap<String,String> map = syncService.getData();
                if (!e.getValueIsAdjusting()) {
                    String selectedFile = fileList.getSelectedValue();
                    if (selectedFile != null) {
                        String content = map.get(selectedFile);
                        fileContentTextArea.setText(content);
                    }
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
                });



        AutoSyncButton.addActionListener(e -> {
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            if (editor == null) {
                Messages.showErrorDialog(project, "No editor is currently selected.", "Error");
                return;
            }
            VirtualFile file = FileEditorManager.getInstance(project).getSelectedEditor().getFile();
            Document document = editor.getDocument();
            if (!runningStatus) {
                String intervalStr = Messages.showInputDialog(project, "Enter the sync interval in milliseconds:", "AutoSync Interval", Messages.getQuestionIcon());
                if (intervalStr != null && !intervalStr.isEmpty()) {
                    try {
                        long interval = Long.parseLong(intervalStr);
                        runningStatus = true;
                        scheduler.scheduleAtFixedRate(() -> {
                            if (runningStatus) {
                                try {
                                    String key = file.getName();
                                    String text = document.getText();
                                    syncService.updateData(key,text);

                                }catch (IOException ex) {
                                    throw new RuntimeException(ex);
                                }
                            }
                        }, 0, interval, TimeUnit.MILLISECONDS);
                    } catch (NumberFormatException ex) {
                        Messages.showErrorDialog(project, "Invalid interval entered. Please enter a valid number.", "Error");
                    }
                }
            } else {
                runningStatus = false;
                scheduler.shutdownNow();
                scheduler = Executors.newScheduledThreadPool(1);
            }
        });








        saveButton.addActionListener(e->{
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            VirtualFile file = FileEditorManager.getInstance(project).getSelectedEditor().getFile();
            if (editor != null) {
                Document document = editor.getDocument();

                new Task.Backgroundable(project, "Save data") {
                    @Override
                    public void run(@NotNull ProgressIndicator progressIndicator) {
                        try {
                            String key = file.getName();
                            String text = document.getText();
                                    syncService.updateData(key,text);
                            getKeys(syncService.getData());
                            ApplicationManager.getApplication().invokeLater(() -> updateFileList(listModel, list));

                        }catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }.queue();
            }
        });

        //Задаём параметры элементам
        syncButton.addActionListener(e -> {
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            if (editor == null) {
                Messages.showErrorDialog(project, "No editor is currently selected.", "Error");
                return;
            }
            new Task.Backgroundable(project, "Syncing Data") {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    try {
                        Map<String, String> data = syncService.getData();
                        ApplicationManager.getApplication().invokeLater(()->{
                            Document document = editor.getDocument();
                            WriteCommandAction.runWriteCommandAction(project,()->{
                                document.setText(data.get(fileList.getSelectedValue()));
                            });
                        });
                    } catch (IOException ex) {
                        ApplicationManager.getApplication().invokeLater(() ->
                                Messages.showErrorDialog(project, "Failed to sync data: " + ex.getMessage(), "Error")
                        );
                    }
                }
            }.queue();
        });


        showButton.addActionListener(e -> {
            new Task.Backgroundable(project, "Updating Data") {
                        @Override
                        public void run(@NotNull ProgressIndicator indicator) {
                            try {
                                Map<String, String> data = syncService.getData();
                                ApplicationManager.getApplication().invokeLater(() -> textArea.setText(data.toString()));
                                System.out.println(list);
                            } catch (IOException ex) {
                                ApplicationManager.getApplication().invokeLater(() ->
                                        Messages.showErrorDialog(project, "Failed to update data: " + ex.getMessage(), "Error")
                                );
                            }
                        }
                    }.queue();
        });


        //?Регистрируем элементы в панеле для idea
        panel.add(syncButton);
        panel.add(saveButton);
        panel.add(showButton);
        panel.add(AutoSyncButton);
        panel.add(new JScrollPane(textArea));
        panel.add(listScrollPane);
        panel.add(contentScrollPane);

        //Добавляем panel в toolWindow
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);
    }

}
