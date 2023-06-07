package com.inanc.smartcommit

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener

class SmartCommitWindowFactory : ToolWindowFactory, ToolWindowManagerListener {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = SmartCommitWindowPanel(project)
        val contentFactory = ContentFactory.getInstance()
        val content: Content = contentFactory.createContent(panel, "", false)
        val hasOpenedContent = AtomicInteger(0)

        panel.addAncestorListener(object : AncestorListener {
            override fun ancestorAdded(event: AncestorEvent?) {
                if (hasOpenedContent.get() < 2) {
                    hasOpenedContent.incrementAndGet()
                } else {
                    panel.openContent()
                }
            }

            override fun ancestorRemoved(event: AncestorEvent?) {
                // Not implemented
            }

            override fun ancestorMoved(event: AncestorEvent?) {
                // Not implemented
            }
        })

        toolWindow.contentManager.addContent(content)
    }
}