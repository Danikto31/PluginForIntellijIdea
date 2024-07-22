package my.SpringProject.PopupMenu;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import my.SpringProject.PopupMenu.ButtonsForPopup.StartSpringApplication;
import my.SpringProject.PopupMenu.ButtonsForPopup.StopSpringApplication;
import org.jetbrains.annotations.NotNull;

public class PopupMenuController extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        DefaultActionGroup group = new DefaultActionGroup("Popup Group",true);
        group.add(new StartSpringApplication());
        group.add(new StopSpringApplication());

  JBPopupFactory.getInstance().createActionGroupPopup("PopupMenu",group,e.getDataContext(), JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,true).showInBestPositionFor(e.getDataContext());

    }
}
