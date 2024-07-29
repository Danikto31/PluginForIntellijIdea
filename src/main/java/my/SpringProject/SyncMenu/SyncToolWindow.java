package my.SpringProject.SyncMenu;

import ch.qos.logback.core.util.FileUtil;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBList;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.components.JBScrollPane;
import my.SpringProject.Controllers.PathController;
import my.SpringProject.DiffOfStringFiles.DifFinder;
import org.jetbrains.annotations.NotNull;
import my.SpringProject.ZipDownloadUnpack.ApacheZipper;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

public class SyncToolWindow implements ToolWindowFactory, DumbAware {
    private String ProjectDirectoryPath;
    private String ProjectName;
    private ConcurrentHashMap<String,String> PathMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String,String> DataMap = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Boolean runningStatus = false;
    private Editor Actualeditor;
    private VirtualFile Actualfile;

    private void setProjectDirectoryPath(String ProjectDirectoryPath) {
        this.ProjectDirectoryPath = ProjectDirectoryPath;
    }
    private void setProjectName(String ProjectName) {
        this.ProjectName = ProjectName;
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
        JButton AutoSaveButton = new JButton("AutoSave");
        JButton AutoSyncButton = new JButton("AutoSync");
        JButton AutoMegaFuckingButton = new JButton("Auto");
        JButton DownloadProject = new JButton("DownloadAndOpenProject");

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

        DownloadProject.addActionListener(e ->{
            String PathToGitProject = Messages.showInputDialog(project,"Enter the path to zipFile to download","Download Project",Messages.getInformationIcon());
            FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(false, true, false, false, false, false);
            FileChooserDialog fileChooser = FileChooserFactory.getInstance().createFileChooser(fileChooserDescriptor, null, null);
            VirtualFile[] files = fileChooser.choose(project);
            new Task.Backgroundable(project,"Downloading zipFiles"){
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    try {
                        File zipFile = ApacheZipper.DownloadZip(PathToGitProject,files);
                        ApacheZipper.unzipFile(zipFile,files);

                        VirtualFile virtualProjectFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(files[0].getPath());
                        if(virtualProjectFile!=null){
                        ProjectUtil.openOrImport(virtualProjectFile.getPath()+"\\"+ApacheZipper.GetFileName(),project,false);
                        setProjectDirectoryPath(virtualProjectFile.getPath()+"/"+ApacheZipper.GetFileName()+"/");
                        setProjectName(ApacheZipper.GetFileName());
                        PathController.setAllPaths(ProjectDirectoryPath);
                        PathMap = PathController.getAllPathMap();
                            ApplicationManager.getApplication().invokeLater(() ->
                                    Messages.showInfoMessage("Successfully download", ApacheZipper.GetFileName()));

                        }else {ApplicationManager.getApplication().invokeLater(() ->
                                Messages.showInfoMessage("Cant find project by the path", "Error"));}
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                }.queue();
        });

        AutoMegaFuckingButton.addActionListener(e -> {
            if (!runningStatus) {
                String intervalStr = Messages.showInputDialog(project, "Enter the sync interval in milliseconds:", "AutoSave Interval", Messages.getQuestionIcon());
                if (intervalStr != null && !intervalStr.isEmpty()) {
                    try {
                        long interval = Long.parseLong(intervalStr);
                        runningStatus = true;
                        scheduler.scheduleAtFixedRate(() -> {
                            if (runningStatus) {
                                PathController.clearMap();
                                PathMap.clear();
                                Actualeditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
                                if (ProjectDirectoryPath == null) {
                                    ProjectDirectoryPath = project.getBasePath();
                                    if(ProjectDirectoryPath!=null){
                                    ProjectName = ProjectDirectoryPath.split("/")[ProjectDirectoryPath.split("/").length-1];}
                                }
                                PathController.setAllPaths(ProjectDirectoryPath);
                                PathMap = PathController.getAllPathMap();
                                System.out.println(PathMap);

                                try {
                                    if (syncService.getPaths().isEmpty() && PathMap != null) {
                                        PathMap.forEach((K, V) -> {
                                            try {
                                                syncService.updatePathData(K, V);
                                            } catch (IOException ex) {
                                                throw new RuntimeException(ex);
                                            }
                                        });
                                    }else {
                                        assert PathMap != null;
                                        if(!PathMap.equals(syncService.getPaths())) {
                                                ConcurrentHashMap<String,String> Map = syncService.getPaths();
                                                PathMap.forEach((K, V) -> {
                                                    if(Map.get(K)!=null){
                                                        Map.remove(K);
                                                    }
                                                });
                                            System.out.println(Map);
                                                if(!Map.isEmpty()){
                                                    Map.forEach((K,V)->{//Проверка всех директорий и создание их(если у тебя файлов меньше, чем на серваке
                                                        if(K.contains("Directory:")){
                                                            String path = ProjectDirectoryPath+Map.get(K).split(ProjectName)[1].replaceAll("\\\\"+K.replace("Directory:",""),"");
                                                            File file = new File(path,K.replace("Directory:",""));
                                                                file.mkdir();
                                                                Map.remove(K);
                                                        }
                                                    });
                                                    Map.forEach((K,V)->{//Создание файлов после директорий
                                                        String path = ProjectDirectoryPath+Map.get(K).split(ProjectName)[1].replaceAll("\\\\"+K,"");
                                                        System.out.println(path);
                                                        System.out.println("pizda");
                                                        File file = new File(path,K);
                                                        try {
                                                           if(file.createNewFile()){
                                                               FileWriter writer = new FileWriter(file);//Создаёт файл, но не сохраняет
                                                           }
                                                        } catch (IOException ex) {
                                                            throw new RuntimeException(ex);
                                                        }

                                                    });
                                                }
                                        }
                                    }
                                } catch (IOException ex) {
                                    throw new RuntimeException(ex);
                                }

                                try {
                                    System.out.println(syncService.getPaths());
                                } catch (IOException ex) {
                                    throw new RuntimeException(ex);
                                }
                                if (Actualeditor == null) {
                                    ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog(project, "No editor is currently selected.", "Error"));
                                    runningStatus = false;
                                    scheduler.shutdownNow();
                                    scheduler = Executors.newScheduledThreadPool(1);
                                    return;
                                }

                                Document document = Actualeditor.getDocument();
                                Actualfile = Objects.requireNonNull(FileEditorManager.getInstance(project).getSelectedEditor()).getFile();

                                String key = Actualfile.getName();
                                String text = document.getText();


                                try {
                                    DataMap = syncService.getData();
                                    if (DataMap.isEmpty()) {
                                        syncService.updateData(key, text);
                                        DataMap = syncService.getData();
                                    }
                                } catch (IOException ex) {
                                    throw new RuntimeException(ex);
                                }


                                String DataText = DataMap.get(key);
                                if (DataText != null) {
                                    if (DifFinder.FileTextNull()) {
                                        DifFinder.setFileText(text);
                                    }
                                    System.out.println(DataText + "//" + DifFinder.getFinaleText());

                                    if (!DataText.equals(DifFinder.getFinaleText())) {
                                        ApplicationManager.getApplication().invokeLater(() -> {
                                            WriteCommandAction.runWriteCommandAction(project, () -> {
                                                try {
                                                    document.setText(syncService.getData().get(key));
                                                } catch (IOException ex) {
                                                    throw new RuntimeException(ex);
                                                }
                                            });
                                        });
                                    }

                                    if (DifFinder.Diff(text)) {
                                        try {
                                            syncService.updateData(key, text);
                                            CountDownLatch latch = new CountDownLatch(1);
                                            ApplicationManager.getApplication().invokeLater(() -> {
                                                WriteCommandAction.runWriteCommandAction(project, () -> {
                                                    try {
                                                        document.setText(syncService.getData().get(key));
                                                        latch.countDown();
                                                    } catch (IOException ex) {
                                                        throw new RuntimeException(ex);
                                                    }
                                                });
                                            });
                                            latch.await();
                                            DifFinder.setFileText(text);
                                        } catch (IOException | InterruptedException ex) {
                                            throw new RuntimeException(ex);
                                        }
                                    } else {
                                        try {
                                            syncService.updateData(key, text);
                                        } catch (IOException ex) {
                                            throw new RuntimeException(ex);
                                        }
                                    }
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

        showButton.addActionListener(e->{
            new Task.Backgroundable(project,"Showing Data"){
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        try {
                            textArea.setText(syncService.getPaths().toString());
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
                }
            }.queue();

        });



        //?Регистрируем элементы в панеле для idea
        panel.add(syncButton);
        panel.add(saveButton);
        panel.add(showButton);
        panel.add(AutoSaveButton);
        panel.add(new JScrollPane(textArea));
        panel.add(listScrollPane);
        panel.add(contentScrollPane);
        panel.add(AutoSyncButton);
        panel.add(AutoMegaFuckingButton);
        panel.add(DownloadProject);
        //Добавляем panel в toolWindow
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);
    }

}
