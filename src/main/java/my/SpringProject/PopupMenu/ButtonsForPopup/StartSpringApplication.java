package my.SpringProject.PopupMenu.ButtonsForPopup;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class StartSpringApplication extends AnAction {
    public String[] TaskCleanSpring;
    public String[] TaskToRunSpring;
    public String PathToSpringProject;
    private static Process process;
    private static final Map<String,String> ListOfPaths = new HashMap<>();
    private static Project project;

    public void actionPerformed(AnActionEvent e) {
        project = e.getProject();
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(false, true, false, false, false, false);
        FileChooserDialog fileChooser = FileChooserFactory.getInstance().createFileChooser(fileChooserDescriptor, null, null);
        VirtualFile[] files = fileChooser.choose(e.getProject());
        if (files.length == 0) {
            Messages.showErrorDialog("No directory selected", "Error");
            return;
        }

        PathToSpringProject = files[0].getPath();
        recF(PathToSpringProject);


        if (PomCheckerElseGradle(ListOfPaths)) {
            TaskCleanSpring = new String[]{"cmd.exe", "/c", "mvn clean install"};
            TaskToRunSpring = new String[]{"cmd.exe", "/c", "mvn spring-boot:run"};
        } else {
            TaskCleanSpring = new String[]{"cmd.exe", "/c", "gradlew build"};
            TaskToRunSpring = new String[]{"cmd.exe", "/c", "gradlew bootRun"};
        }


        new Task.Backgroundable(project, "Run Spring Application") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    ProcessBuilder processBuilderClean = new ProcessBuilder(TaskCleanSpring);
                    processBuilderClean.directory(new File(PathToSpringProject));
                    process = processBuilderClean.start();

                    StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), System.out::println);
                    StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), System.err::println);

                    Executors.newSingleThreadExecutor().submit(outputGobbler);
                    Executors.newSingleThreadExecutor().submit(errorGobbler);

                    int exitCode = process.waitFor();
                    if (exitCode == 0) {
                        ProcessBuilder processBuilderRun = new ProcessBuilder(TaskToRunSpring);
                        processBuilderRun.directory(new File(PathToSpringProject));
                        process = processBuilderRun.start();

                        outputGobbler = new StreamGobbler(process.getInputStream(), System.out::println);
                        errorGobbler = new StreamGobbler(process.getErrorStream(), System.err::println);

                        Executors.newSingleThreadExecutor().submit(outputGobbler);
                        Executors.newSingleThreadExecutor().submit(errorGobbler);

                        System.out.println("Spring Boot application started.");
                        ApplicationManager.getApplication().invokeLater(() ->
                                Messages.showMessageDialog("Spring Boot application successfully started with PID: " + process.pid(), "Spring Boot", Messages.getInformationIcon())
                        );
                    } else {
                        System.err.println("Clean install failed.");
                        ApplicationManager.getApplication().invokeLater(() ->
                                Messages.showErrorDialog("Clean install failed.", "Error")
                        );
                    }
                } catch (IOException |
                         InterruptedException ex) {
                    throw new RuntimeException(ex);
                }

            }
        }.queue();






    }
    public static void recF(String Path){
        File[] file = new File(Path).listFiles();
        for(int i=0; i<file.length; ++i){
            if(!file[i].isDirectory()){
                ListOfPaths.put(file[i].getName(), file[i].getAbsolutePath());
            }else recF(file[i].getAbsolutePath());
        }
    }
    public static boolean PomCheckerElseGradle(Map<String,String> map){
        if(map.get("pom.xml")!=null)return true;
        else return false;
    }
    public void update(AnActionEvent e) {
    }

    public StartSpringApplication(){
        super("StartSpringApplication","StartHost",null);
    }


    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    public static Process getProcess() {
        return process;
    }

    private static class StreamGobbler implements Runnable {
        private final InputStream inputStream;
        private final Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer);
        }
    }
}
