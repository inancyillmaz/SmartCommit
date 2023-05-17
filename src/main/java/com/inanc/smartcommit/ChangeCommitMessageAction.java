package com.inanc.smartcommit;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

public class ChangeCommitMessageAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // Get the current project
        Project project = e.getProject();

        // Show a dialog to get the new commit message
        String newCommitMessage = Messages.showInputDialog(project, "Enter new commit message:", "Change Commit Message", Messages.getQuestionIcon());

        if (newCommitMessage == null || newCommitMessage.trim().isEmpty()) {
            return;
        }

        // Your logic to change the commit message without interacting with Git directly
        // This may include using the IntelliJ VCS API to fetch commit history and update the commit message
    }
}