package com.inanc.smartcommit

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.ui.content.ContentFactory

class SmartCommitWindowFactory : ToolWindowFactory, ToolWindowManagerListener {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(SmartCommitWindowPanel(project), "SmartCommit", false)
        toolWindow.contentManager.addContent(content)
    }
}
