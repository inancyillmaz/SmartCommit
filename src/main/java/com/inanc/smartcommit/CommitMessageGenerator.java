package com.inanc.smartcommit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ContentRevision;
import org.jetbrains.annotations.NotNull;

public class CommitMessageGenerator  {

    public static class ChangeData {
        public String filePath;
        public List<String> addedLines;
        public List<String> modifiedLines;
        public List<String> removedLines;
    }

    public List<ChangeData> getChangeDataFromChangeListManager(@NotNull Project project) {
        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        Collection<Change> changes = changeListManager.getAllChanges();

        List<ChangeData> changeDataList = new ArrayList<>();
        DiffMatchPatch dmp = new DiffMatchPatch();

        for (Change change : changes) {
            ContentRevision beforeRevision = change.getBeforeRevision();
            ContentRevision afterRevision = change.getAfterRevision();

            if (beforeRevision == null || afterRevision == null) {
                continue;
            }

            String beforeContent = null;
            try {
                beforeContent = beforeRevision.getContent();
            } catch (VcsException e) {
                throw new RuntimeException(e);
            }
            String afterContent = null;
            try {
                afterContent = afterRevision.getContent();
            } catch (VcsException e) {
                throw new RuntimeException(e);
            }

            LinkedList<DiffMatchPatch.Diff> diffs = dmp.diff_main(beforeContent, afterContent);
            dmp.diff_cleanupSemantic(diffs);

            List<String> addedLines = new ArrayList<>();
            List<String> modifiedLines = new ArrayList<>();
            List<String> removedLines = new ArrayList<>();

            for (DiffMatchPatch.Diff diff : diffs) {
                if (diff.operation == DiffMatchPatch.Operation.DELETE) {
                    removedLines.add(diff.text);
                } else if (diff.operation == DiffMatchPatch.Operation.INSERT) {
                    addedLines.add(diff.text);
                } else {
                    modifiedLines.add(diff.text);
                }
            }

            ChangeData changeData = new ChangeData();
            changeData.filePath = afterRevision.getFile().getPath();
            changeData.addedLines = addedLines;
            changeData.modifiedLines = modifiedLines;
            changeData.removedLines = removedLines;

            changeDataList.add(changeData);
        }

        return changeDataList;
    }

    public String createAIPromptFromChangeDataList(List<ChangeData> changeDataList) {
        StringBuilder promptBuilder = new StringBuilder();

        promptBuilder.append("Given the following changes in the codebase:\n\n");

        for (ChangeData changeData : changeDataList) {
            promptBuilder.append("File: ").append(changeData.filePath).append("\n");
            promptBuilder.append("  Added lines:\n");
            for (String addedLine : changeData.addedLines) {
                promptBuilder.append("  ").append(addedLine).append("\n");
            }

            promptBuilder.append("  Modified lines:\n");
            for (String modifiedLine : changeData.modifiedLines) {
                promptBuilder.append("  ").append(modifiedLine).append("\n");
            }

            promptBuilder.append("  Removed lines:\n");
            for (String removedLine : changeData.removedLines) {
                promptBuilder.append("  ").append(removedLine).append("\n");
            }
            promptBuilder.append("\n");
        }

        promptBuilder.append("Please create a concise and descriptive commit message that summarizes the changes made.");

        return promptBuilder.toString();
    }


}