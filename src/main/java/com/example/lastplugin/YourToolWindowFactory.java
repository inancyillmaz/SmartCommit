package com.example.lastplugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

public class YourToolWindowFactory implements ToolWindowFactory, ToolWindowManagerListener {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        YourToolWindowPanel panel = new YourToolWindowPanel(project);
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(panel, "", false);

        panel.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                String lastActiveToolWindowId = ToolWindowManager.getInstance(project).getLastActiveToolWindowId();
                if (lastActiveToolWindowId != null) {
                    ToolWindow lastActiveToolWindow = ToolWindowManager.getInstance(project).getToolWindow(lastActiveToolWindowId);
                    if (lastActiveToolWindow != null) {
                        lastActiveToolWindow.activate(null);
                    }
                }
                panel.openContent();
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {

            }

            @Override
            public void ancestorMoved(AncestorEvent event) {

            }
        });

        toolWindow.getContentManager().addContent(content);

    }
}
