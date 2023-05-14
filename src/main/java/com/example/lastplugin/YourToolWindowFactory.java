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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class YourToolWindowFactory implements ToolWindowFactory, ToolWindowManagerListener {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        YourToolWindowPanel panel = new YourToolWindowPanel(project);
        ContentFactory contentFactory = project.getService(ContentFactory.class);
        Content content = contentFactory.createContent(panel, "", false);
        final AtomicInteger hasOpenedContent = new AtomicInteger(0);

        panel.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                /*
                String lastActiveToolWindowId = ToolWindowManager.getInstance(project).getLastActiveToolWindowId();
                if (lastActiveToolWindowId != null) {
                    ToolWindow lastActiveToolWindow = ToolWindowManager.getInstance(project).getToolWindow(lastActiveToolWindowId);
                    if (lastActiveToolWindow != null) {
                        lastActiveToolWindow.activate(null);
                    }
                }
                 */

                if(hasOpenedContent.get() <2){
                    hasOpenedContent.set(hasOpenedContent.get()+1);
                }else {
                    panel.openContent();
                }
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
