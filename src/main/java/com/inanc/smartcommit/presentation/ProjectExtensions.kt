package com.inanc.smartcommit.presentation

import com.inanc.smartcommit.domain.ThreadExecutionListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsNotifier
import javax.swing.SwingUtilities

@Suppress("TooGenericExceptionCaught")
fun Project.executeOnPooledThread(
    title: String,
    threadExecutionListener: ThreadExecutionListener,
    process: () -> Unit
) {
    ApplicationManager.getApplication().executeOnPooledThread {
        threadExecutionListener.onStart()
        val progressManager = ProgressManager.getInstance()
        val task = object : Task.Backgroundable(this, title, true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                indicator.text = title
                indicator.fraction = 0.0

                try {
                    process()
                } catch (e: Exception) {
                    threadExecutionListener.onError(e)
                } finally {
                    threadExecutionListener.onEnd()
                }
            }
        }
        progressManager.run(task)
    }
}

fun Project.notifyErrorMessage(displayId: String, title: String, message: String, shouldInvokeLater: Boolean) {
    if (shouldInvokeLater) {
        SwingUtilities.invokeLater {
            VcsNotifier.getInstance(this)
                .notifyError(displayId, title, message)
        }
        return
    }
    VcsNotifier.getInstance(this)
        .notifyError(displayId, title, message)
}

fun Project.notifyNetworkErrorMessage(shouldInvokeLater: Boolean, message: String?) {
    notifyErrorMessage(
        displayId = "Network Error",
        title = "Network Error",
        message = message ?: "Opps there is a problem with network, Please try again...",
        shouldInvokeLater = shouldInvokeLater
    )
}
