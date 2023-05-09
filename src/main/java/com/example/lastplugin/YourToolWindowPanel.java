package com.example.lastplugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vcs.changes.ui.CommitChangeListDialog;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
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
       CommitMessageGenerator tmp = new CommitMessageGenerator();
       tmp.getChangeDataFromChangeListManager(project);
       String prompt = tmp.createAIPromptFromChangeDataList(tmp.getChangeDataFromChangeListManager(project));
        LocalChangeList initialSelection = changeListManager.getDefaultChangeList();

        String body =  sentARequest(prompt);
        CommitChangeListDialog.commitChanges(project, changes, initialSelection, null, extractContent(body));
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

    private String sentARequest(String prompt) {

        URL url = null; // Replace with the URL you want to send the request to
        try {
            url = new URL("https://api.openai.com/v1/chat/completions");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6Ik1UaEVOVUpHTkVNMVFURTRNMEZCTWpkQ05UZzVNRFUxUlRVd1FVSkRNRU13UmtGRVFrRXpSZyJ9.eyJodHRwczovL2FwaS5vcGVuYWkuY29tL3Byb2ZpbGUiOnsiZW1haWwiOiJjb2RleGZsb3dzQGdtYWlsLmNvbSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlfSwiaHR0cHM6Ly9hcGkub3BlbmFpLmNvbS9hdXRoIjp7InVzZXJfaWQiOiJ1c2VyLWpQYXYzVHJua0hMeTJ4U0VEMTNKcW9MSSJ9LCJpc3MiOiJodHRwczovL2F1dGgwLm9wZW5haS5jb20vIiwic3ViIjoiZ29vZ2xlLW9hdXRoMnwxMDU2Mjg2NDMwMzMxNzY5MDM0MzgiLCJhdWQiOlsiaHR0cHM6Ly9hcGkub3BlbmFpLmNvbS92MSIsImh0dHBzOi8vb3BlbmFpLm9wZW5haS5hdXRoMGFwcC5jb20vdXNlcmluZm8iXSwiaWF0IjoxNjgyOTM5NTEyLCJleHAiOjE2ODQxNDkxMTIsImF6cCI6IlRkSkljYmUxNldvVEh0Tjk1bnl5d2g1RTR5T282SXRHIiwic2NvcGUiOiJvcGVuaWQgcHJvZmlsZSBlbWFpbCBtb2RlbC5yZWFkIG1vZGVsLnJlcXVlc3Qgb3JnYW5pemF0aW9uLnJlYWQgb2ZmbGluZV9hY2Nlc3MifQ.2hsT4BZsUFzENLWBFvkBveBLOZYrxtdjYmvgwD1ZTwAG4GiIhlVLakQJXQIW2BmsA249FdIph6KCGComJHalVB3qy1ZBKuX3FglEx2DBJtcC9scbL1eDZ15FrTyJBGYUML-PlUMv-9DQ5o_gjvrQOlHoXvVYao92vL08XCv_hvx9HkNvLH8iz34FajsU5YHV290QMgKns2hXkDeu00VKHx6O0GOBGi0TmM_lfpf_1PxCoVWWuYl19CZ9zGYWwWcW6bu8XQtHdHVdJN2AGMetdt2ue0oFGL4dVS5XgPtTfYoJH4AIvPKBncshiY86gLUhrBR7n4QzIYxT91O7OGNi4g");
        try {
            conn.setRequestMethod("POST");
        } catch (ProtocolException e) {
            throw new RuntimeException(e);
        }
        conn.setDoOutput(true);

// Set the request body
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "gpt-3.5-turbo");
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        JSONArray messagesArray = new JSONArray();
        messagesArray.add(message);
        requestBody.put("messages", messagesArray);

       OutputStream os = null;
        try {
            os = conn.getOutputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            os.write(requestBody.toString().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            os.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            os.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

// Read the response
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String inputLine;
        StringBuilder response = new StringBuilder();
        while (true) {
            try {
                if ((inputLine = in.readLine()) == null) break;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            response.append(inputLine);
        }
        try {
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

// Use the response
        return response.toString();


    }

    public String extractContent(String jsonString) {
        String contentKey = "\"content\":\"";
        int contentStartIndex = jsonString.indexOf(contentKey) + contentKey.length();
        int contentEndIndex = jsonString.indexOf("\"", contentStartIndex);

        if (contentStartIndex < contentKey.length() || contentEndIndex == -1) {
            return null; // content key not found or improperly formatted
        }

        return jsonString.substring(contentStartIndex, contentEndIndex);
    }

}