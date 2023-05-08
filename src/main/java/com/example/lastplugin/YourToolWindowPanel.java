package com.example.lastplugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vcs.changes.ui.CommitChangeListDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class YourToolWindowPanel extends JPanel {
    private Project project;

    public YourToolWindowPanel(Project project) {
        this.project = project;
       // openCommitChangesDialog();
      //  initUI();
    }



    private void initUI() {
        JButton changeCommitMessageButton = new JButton("Smart Commit");
        changeCommitMessageButton.addActionListener(e -> openCommitChangesDialog());
        this.add(changeCommitMessageButton);
    }

    public void openContent(){
        openCommitChangesDialog();
    }


    private void openCommitChangesDialog() {
        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        List<LocalChangeList> localChangeLists = changeListManager.getChangeListsCopy();
        DiffMatchPatch dmp = new DiffMatchPatch();

        // Extract changes from LocalChangeLists
        Collection<Change> changes = new ArrayList<>();
        for (LocalChangeList localChangeList : localChangeLists) {
            changes.addAll(localChangeList.getChanges());
        }

        if (changes.isEmpty()) {
            VcsNotifier.getInstance(project).notifyError("No Changes Detected", "There are no changes to commit.","There are no changes to commit.");
            return;
        }
        processChangedLines();

        LocalChangeList initialSelection = changeListManager.getDefaultChangeList();
        CommitChangeListDialog.commitChanges(project, changes, initialSelection, null, "Commit Changes Noluyo Olum");
    }

    private void processChangedLines() {
        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        List<LocalChangeList> localChangeLists = changeListManager.getChangeListsCopy();
        DiffMatchPatch dmp = new DiffMatchPatch();

        for (LocalChangeList localChangeList : localChangeLists) {
            Collection<Change> changes = localChangeList.getChanges();

            for (Change change : changes) {
                ContentRevision beforeRevision = change.getBeforeRevision();
                ContentRevision afterRevision = change.getAfterRevision();

                if (beforeRevision != null && afterRevision != null) {
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

                    // Compute the differences between the two content strings
                    if(beforeContent == null || afterContent == null){
                        return;
                    }
                    LinkedList<DiffMatchPatch.Diff> diffs = dmp.diff_main(beforeContent, afterContent);
                    dmp.diff_cleanupSemantic(diffs);

                    // Process the differences to identify modified lines and their content
                    int lineNumberBefore = 1;
                    int lineNumberAfter = 1;

                    for (DiffMatchPatch.Diff diff : diffs) {
                        String[] lines = diff.text.split("\n");

                        if (diff.operation == DiffMatchPatch.Operation.DELETE) {
                            for (String line : lines) {
                                if (!line.isEmpty()) {
                                    System.out.printf("Line %d removed: %s%n", lineNumberBefore, line);
                                }
                                lineNumberBefore++;
                            }
                        } else if (diff.operation == DiffMatchPatch.Operation.INSERT) {
                            for (String line : lines) {
                                if (!line.isEmpty()) {
                                    System.out.printf("Line %d added: %s%n", lineNumberAfter, line);
                                }
                                lineNumberAfter++;
                            }
                        } else {
                            lineNumberBefore += lines.length - 1;
                            lineNumberAfter += lines.length - 1;
                        }
                    }
                }
            }
        }
    }
}