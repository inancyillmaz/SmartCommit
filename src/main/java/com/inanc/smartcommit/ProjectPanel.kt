package com.inanc.smartcommit

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.vcs.commit.CommitWorkflowHandler
import java.io.File
import javax.swing.JComponent

class ProjectPanel : CheckinHandler() {
    override fun beforeCheckin(): ReturnResult {
        return super.beforeCheckin()
    }
}