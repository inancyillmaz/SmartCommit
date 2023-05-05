package com.example.lastplugin;

import com.intellij.openapi.project.Project;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class YourToolWindowPanel extends JPanel {
    private Project project;

    public YourToolWindowPanel(Project project) {
        this.project = project;
        initUI();
    }

    private void initUI() {
        JButton changeCommitMessageButton = new JButton("Change Commit Message");
        changeCommitMessageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Your logic to change the commit message without interacting with Git directly
            }});
        this.add(changeCommitMessageButton);
    }
}