package com.inanc.smartcommit

import com.inanc.smartcommit.domain.*
import com.inanc.smartcommit.presentation.*
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.ui.CommitChangeListDialog
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.Dimension
import java.awt.event.ItemEvent
import java.util.*
import javax.swing.*
import javax.swing.border.Border
import javax.swing.border.CompoundBorder

class SmartCommitWindowPanel(private val project: Project) : JPanel() {

    private val localPreferences by lazy { service<LocalPreferences>() }

    private val openAIService by lazy { service<OpenAIService>() }

    private val textArea: JTextArea by lazy {
        JTextArea()
    }

    private val smartCommitButton by lazy {
        JButton(PluginBundle.message("smartCommitButtonText"))
    }

    private val threadExecuteListener = object : ThreadExecutionListener {
        override fun onStart() {
            smartCommitButton.isEnabled = false
        }

        override fun onEnd() {
            smartCommitButton.isEnabled = true
        }

        override fun onError(error: Throwable) {
            smartCommitButton.isEnabled = true
            project.notifyNetworkErrorMessage(
                shouldInvokeLater = true,
                message = error.message
            )
        }
    }

    init {
        initMyUi()
    }

    private fun addEmptyLines() {
        add(Box.createRigidArea(Dimension(0, 10)))
    }

    private fun initMyUi() {
        val accessToken = localPreferences.getString(SHARED_PREF_ACCESS_TOKEN_KEY)
        val acceptTerms = localPreferences.getBoolean(SHARED_PREF_ACCEPT_TERMS_KEY)

        val padding: Border = BorderFactory.createEmptyBorder(10, 10, 10, 20)
        layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
        border = padding

     //   maximumSize = Dimension(300, 300)

        add(generateLabel(PluginBundle.message("logIntoOpenAi")))

        val lblLoginToOpenAi = createBrowsableLink(title = PluginBundle.message("htmlTitleLoginToOpenAI"),GPT_AUTH_LOGIN_URL) {
            project.notifyErrorMessage(
                displayId = PluginBundle.message("error"),
                title = it.orEmpty(),
                message = "",
                shouldInvokeLater = false
            )
        }
        addEmptyLines()
        add(lblLoginToOpenAi)
        addEmptyLines()

        add(
            JLabel(PluginBundle.message("accessTokenDescription"))
        )


        addEmptyLines()
        val lblAccessToken = createBrowsableLink(PluginBundle.message("htmlTitleGetAccessToken"),GPT_AUTH_SESSION_URL) {
            project.notifyErrorMessage(
                displayId = PluginBundle.message("error"),
                title = it.orEmpty(),
                message = "",
                shouldInvokeLater = false
            )
        }
        add(lblAccessToken)
        addEmptyLines()

        add(
            JLabel(PluginBundle.message("pasteAccessToken"))
        )
        addEmptyLines()

        val margin = JBUI.Borders.empty(5, 15)

        textArea.apply {
            lineWrap = true
            wrapStyleWord = true
            border = CompoundBorder(textArea.border, margin)
            alignmentX = Component.LEFT_ALIGNMENT
            text = accessToken
            maximumSize = Dimension(500, 250)
        }

        add(textArea)

        val cbTerms = JCheckBox(PluginBundle.message("termsAndConditions"))
        cbTerms.isSelected = acceptTerms
        addEmptyLines()
        add(cbTerms)
        addEmptyLines()

        val warningLabel = JLabel(PluginBundle.message("warningMessage"))
        warningLabel.foreground = JBColor.ORANGE
        add(warningLabel)
        addEmptyLines()

        val buttonBox: Box = Box.createHorizontalBox().apply {
            add(Box.createHorizontalGlue())
            add(smartCommitButton)
            add(Box.createHorizontalGlue())
        }
        add(buttonBox)

        smartCommitButton.addActionListener {
            val tmpText = textArea.text
            tmpText.extractAccessToken()?.let {
                localPreferences.saveString(SHARED_PREF_ACCESS_TOKEN_KEY, tmpText)
                openContent()
            } ?: run {
                project.notifyErrorMessage(
                    displayId = UUID.randomUUID().toString(),
                    message = PluginBundle.message("accessTokenInvalid"),
                    title = PluginBundle.message("error"),
                    shouldInvokeLater = false
                )
            }
        }

        smartCommitButton.isEnabled = cbTerms.isSelected
        cbTerms.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED) {
                localPreferences.saveBoolean(SHARED_PREF_ACCEPT_TERMS_KEY, true)
                smartCommitButton.isEnabled = true
            } else {
                smartCommitButton.isEnabled = false
                localPreferences.saveBoolean(SHARED_PREF_ACCEPT_TERMS_KEY, false)
            }
        }
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(PREFERRED_WIDTH, super.getPreferredSize().height)
    }

    private fun openContent() {
        val accessToken = localPreferences.getString(SHARED_PREF_ACCESS_TOKEN_KEY)
        val acceptTerms = localPreferences.getBoolean(SHARED_PREF_ACCEPT_TERMS_KEY)
        if (accessToken != null && acceptTerms) {
            openCommitChangesDialog()
        }
    }

    @Suppress("NestedBlockDepth")
    private fun openCommitChangesDialog() {
        val changeListManager = ChangeListManager.getInstance(project)
        val localChangeLists = changeListManager.changeLists

        val changes: MutableList<Change> = ArrayList()
        for (localChangeList in localChangeLists) {
            changes.addAll(localChangeList.changes)
        }

        if (changes.isEmpty()) {
            project.notifyErrorMessage(
                displayId = PluginBundle.message("noChangesDetekted"),
                title = PluginBundle.message("noChangesToCommit"),
                message = "",
                shouldInvokeLater = false
            )
            return
        }

        val previousVersion: ArrayList<String> = ArrayList()
        val currentVersions: ArrayList<String> = ArrayList()

        localChangeLists.forEach { localChangeList ->
            val changesX = localChangeList.changes

            changesX.forEach { change ->
                try {
                    change.beforeRevision?.let { previousVersion.add(it.content.orEmpty()) }
                    change.afterRevision?.let { currentVersions.add(it.content.orEmpty()) }
                } catch (e: VcsException) {
                    project.notifyErrorMessage(
                        displayId = "Commit Message Error",
                        title = "",
                        message = e.message,
                        shouldInvokeLater = false
                    )
                }
            }
        }

        val prompt = openAIService.createAIPromptFromLists(oldList = previousVersion, newList = currentVersions)
        val initialSelection = changeListManager.defaultChangeList
        project.executeOnPooledThread("Smart Commit Running", threadExecuteListener) {
            var wordCount = 0
            for (changeData in previousVersion) {
                wordCount += changeData.getWordsCount()
            }
            for (changeData in currentVersions) {
                wordCount += changeData.getWordsCount()
            }

            if (wordCount <= MAX_WORD_COUNT) {
                val body = openAIService.requestSmartCommitMessage(prompt) {
                    project.notifyNetworkErrorMessage(
                        message = it.message,
                        shouldInvokeLater = true
                    )
                }
                if (body != null) {
                    SwingUtilities.invokeLater {
                        CommitChangeListDialog.commitChanges(
                            /* project = */ project,
                            /* changes = */ changes,
                            /* initialSelection = */ initialSelection,
                            /* executor = */ null,
                            /* comment = */ body.extractContent()
                        )
                    }
                }
            } else {
                project.notifyErrorMessage(
                    displayId = "",
                    title = PluginBundle.message("error"),
                    message = PluginBundle.message("longChangeListTitleError"),
                    shouldInvokeLater = false,
                )
            }
        }
    }
}
