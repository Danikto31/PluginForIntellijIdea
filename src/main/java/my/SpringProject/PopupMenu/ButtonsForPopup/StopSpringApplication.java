package my.SpringProject.PopupMenu.ButtonsForPopup;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class StopSpringApplication extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent ex) {
        Process process = StartSpringApplication.getProcess();
        long pid = process.pid();
        try {
            String cmd = "taskkill /F /T /PID " + pid;
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", cmd);
            Process p = pb.start();
            p.waitFor();
            Messages.showMessageDialog("Spring boot application successful finished","Spring-boot",Messages.getInformationIcon());
            System.out.println("Дерево по PID-"+pid+" завершено");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }
    public StopSpringApplication(){
        super("StopSpringApplication","Stop",null);
    }
    public void update(AnActionEvent e) {
    }
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
