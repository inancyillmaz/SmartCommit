package com.inanc.smartcommit;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.util.concurrent.atomic.AtomicInteger;

public class YourToolWindowFactory implements ToolWindowFactory, ToolWindowManagerListener {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        YourToolWindowPanel panel = new YourToolWindowPanel(project);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "", false);
        final AtomicInteger hasOpenedContent = new AtomicInteger(0);

        panel.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
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
