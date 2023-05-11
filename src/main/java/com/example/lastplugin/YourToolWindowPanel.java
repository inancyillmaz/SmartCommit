package com.example.lastplugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vcs.changes.ui.CommitChangeListDialog;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class YourToolWindowPanel extends JPanel {
    private Project project;
    private JLabel linkLabel;
    private JTextField textField;

    public YourToolWindowPanel(Project project) {
        this.project = project;
        initMyUi();
    }

    public void addEmptyLines() {
        add(Box.createRigidArea(new Dimension(0, 10)));
    }

    public void initMyUi() {
        Border padding = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setBorder(padding);

// First link
        JLabel linkLabel1 = new JLabel("<html><u>Log in to OpenAI</u></html>");
        linkLabel1.setForeground(JBColor.cyan);
        linkLabel1.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        linkLabel1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openWebURL("https://chat.openai.com/auth/login");
            }
        });
        add(new JLabel("Please start by logging into OpenAI. You can do this by clicking the following link:"));
        addEmptyLines();
        add(linkLabel1);
        addEmptyLines();

// Second link
        add(new JLabel("Next, you'll need to obtain an access token. Simply follow this link, then copy the token you're provided with:"));
        JLabel linkLabel2 = new JLabel("<html><u>Get Your Access Token</u></html>");
        linkLabel2.setForeground(JBColor.cyan);
        linkLabel2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        linkLabel2.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openWebURL("https://chat.openai.com/api/auth/session");
            }
        });
        add(linkLabel2);
        addEmptyLines();

// Note
        add(new JLabel("Once you've obtained your access token, please paste it into the provided field below."));
        addEmptyLines();

// Text field
        JTextField textField = new JTextField();
        Dimension maxDimension = new Dimension(Integer.MAX_VALUE, 60); // Width is MAX_VALUE, height is 30
        Border margin = JBUI.Borders.empty(5, 15);
        textField.setBorder(new CompoundBorder(textField.getBorder(), margin));
        textField.setMaximumSize(maxDimension);
        textField.setAlignmentX(Component.LEFT_ALIGNMENT);  // add this line
        add(textField);

// Checkboxes
        JCheckBox checkBox1 = new JCheckBox("By proceeding, I acknowledge and agree that any changes I make to the code lines or classes may be communicated to OpenAI.");

        addEmptyLines();
        add(checkBox1);
        addEmptyLines();

// Button
        JLabel warningLabel = new JLabel("In order to utilize this plugin, you might be required to provide your billing information to OpenAI.");
        warningLabel.setForeground(JBColor.ORANGE); // Set the text color to orange
        add(warningLabel);

        addEmptyLines();
        JButton button = new JButton("Start");
        add(button);

    }

    private void openWebURL(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void setText(String text) {
        repaint(); // Repaint the panel to update the displayed text
    }


    private void initUI() {
        JButton changeCommitMessageButton = new JButton("Smart Commit");
        changeCommitMessageButton.addActionListener(e -> openCommitChangesDialog());
        this.add(changeCommitMessageButton);
    }

    public void openContent() {
        VcsNotifier.getInstance(project).notifyImportantInfo("Commit Message Calculating", "Commit message calculating", "Commit message calculating");
    //    openCommitChangesDialog();
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
            VcsNotifier.getInstance(project).notifyError("No Changes Detected", "There are no changes to commit.", "There are no changes to commit.");
            return;
        }
        processChangedLines();


        ArrayList<String> previousVersion = new ArrayList<>();
        ArrayList<String> currentVersions = new ArrayList<>();

        for (LocalChangeList localChangeList : localChangeLists) {
            Collection<Change> changesX = localChangeList.getChanges();

            for (Change change : changesX) {
                try {
                    if (change.getBeforeRevision() != null) {
                        previousVersion.add(change.getBeforeRevision().getContent());
                    }
                    if (change.getAfterRevision() != null) {
                        currentVersions.add(change.getAfterRevision().getContent());
                    }
                } catch (VcsException e) {
                    throw new RuntimeException(e);
                }
            }
        }


        CommitMessageGenerator tmp = new CommitMessageGenerator();
        tmp.getChangeDataFromChangeListManager(project);
        String prompt = createAIPromptFromMyLists(previousVersion, currentVersions);
        LocalChangeList initialSelection = changeListManager.getDefaultChangeList();

        new Thread(new Runnable() {
            @Override
            public void run() {
                // Perform the network request off the EDT
                final String body = sentARequest(prompt);

                // Switch back to the EDT to update the UI
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        CommitChangeListDialog.commitChanges(project, changes, initialSelection, null, extractContent(body));
                    }
                });
            }
        }).start();

    }

    public String createAIPromptFromMyLists(ArrayList<String> oldList, ArrayList<String> newList) {
        StringBuilder promptBuilder = new StringBuilder();

        promptBuilder.append("Forget all the conversation and start new conversation with given the following changes in the codebase:\n\n");

        for (String changeData : oldList) {
            promptBuilder.append("The old version of classes: \n").append(changeData);
        }

        for (String changeData : newList) {
            promptBuilder.append("\nThe new version of classes: \n").append(changeData);
        }

        promptBuilder.append("\nPlease create a concise and descriptive commit message that summarizes the changes made. And commit message couldn't involves words like this; \n Refactored, etc. And try to be spesific");
        return promptBuilder.toString();
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
                    if (beforeContent == null || afterContent == null) {
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