package com.inanc.smartcommit

import com.inanc.smartcommit.data.exceptions.ApiExceptions
import com.inanc.smartcommit.domain.*
import com.inanc.smartcommit.domain.exceptions.TooLongChangeError
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
import java.awt.FlowLayout
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

        override fun onApiError(apiExceptions: ApiExceptions) {
            when (apiExceptions) {
                ApiExceptions.ApiExceptions401,
                ApiExceptions.ApiExceptions429 -> {
                    GPT_BILLING_URL.openWebURL {
                        project.notifyNetworkErrorMessage(
                            shouldInvokeLater = true
                        )
                    }
                }

                ApiExceptions.ApiExceptionsUnknown -> {
                    project.notifyNetworkErrorMessage(
                        shouldInvokeLater = true
                    )
                }
            }
        }
    }

    init {
        initUi()
    }

    private fun addEmptyLines() {
        add(Box.createRigidArea(Dimension(0, 10)))
    }

    private fun initUi() {
        val accessToken = localPreferences.getString(SHARED_PREF_ACCESS_TOKEN_KEY)
        val acceptTerms = localPreferences.getBoolean(SHARED_PREF_ACCEPT_TERMS_KEY)

        val padding: Border = BorderFactory.createEmptyBorder(10, 10, 10, 20)
        layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
        border = padding

        val lblLogin = generateLabel(PluginBundle.message("logIntoOpenAi"))
        add(lblLogin)

        val lblLoginToOpenAi = createBrowsableLink(
            title = PluginBundle.message("htmlTitleLoginToOpenAI"),
            url = GPT_AUTH_LOGIN_URL
        ) {
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

        val lblAccessTokenTitle = generateLabel(PluginBundle.message("accessTokenDescription"))
        add(lblAccessTokenTitle)

        addEmptyLines()
        val lblAccessToken =
            createBrowsableLink(PluginBundle.message("htmlTitleGetAccessToken"), GPT_AUTH_SESSION_URL) {
                project.notifyErrorMessage(
                    displayId = PluginBundle.message("error"),
                    title = it.orEmpty(),
                    message = "",
                    shouldInvokeLater = false
                )
            }
        add(lblAccessToken)
        addEmptyLines()

        val lblPasteAccessToken = generateLabel(PluginBundle.message("pasteAccessToken"))
        add(lblPasteAccessToken)
        addEmptyLines()

        val margin = JBUI.Borders.empty(5, 15)


        textArea.apply {
            lineWrap = true
            wrapStyleWord = true
            border = CompoundBorder(textArea.border, margin)
            alignmentX = Component.LEFT_ALIGNMENT
            text = accessToken
        }

// This will ensure that initially textArea takes a reasonable amount of vertical space.
        val initialHeight = 100 // You can adjust this value as needed
        textArea.preferredSize = Dimension(500, initialHeight)

// This will ensure that textArea does not grow vertically beyond a maximum height.
        val maximumHeight = 300 // You can adjust this value as needed
        textArea.maximumSize = Dimension(Integer.MAX_VALUE, maximumHeight)

        add(textArea)

        val cbTerms = JCheckBox(PluginBundle.message("termsAndConditions"))
        cbTerms.isSelected = acceptTerms
        addEmptyLines()
        add(cbTerms)
        addEmptyLines()

        val warningLabel = generateLabel(PluginBundle.message("warningMessage"))
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
        project.executeOnPooledThread(PluginBundle.message("smartCommitRunning"), threadExecuteListener) {
            var wordCount = 0
            for (changeData in previousVersion) {
                wordCount += changeData.getWordsCount()
            }
            for (changeData in currentVersions) {
                wordCount += changeData.getWordsCount()
            }

            if (wordCount <= MAX_WORD_COUNT) {
                val body = openAIService.requestSmartCommitMessage(prompt) {
                    threadExecuteListener.onApiError(it)
                }
                if (body != null) {
                    SwingUtilities.invokeLater {
                        CommitChangeListDialog.commitChanges(
                            /* project = */ project,
                            /* changes = */ changes,
                            /* initialSelection = */ initialSelection,
                            /* executor = */ null,
                            /* comment = */ body.extractContent(project)
                        )
                    }
                }
            } else {
                threadExecuteListener.onError(
                    TooLongChangeError(
                        message = PluginBundle.message("longChangeListTitleError"),
                    )
                )
            }
        }
    }
}
