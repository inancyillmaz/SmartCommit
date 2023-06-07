package com.inanc.smartcommit

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.VcsNotifier
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.LocalChangeList
import com.intellij.openapi.vcs.changes.ui.CommitChangeListDialog
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import net.minidev.json.JSONArray
import net.minidev.json.JSONObject
import javax.swing.*
import javax.swing.border.Border
import javax.swing.border.CompoundBorder
import java.awt.*
import java.awt.event.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.*
import java.util.ArrayList
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future


class SmartCommitWindowPanel(private val project: Project) : JPanel() {
    private var linkLabel: JLabel? = null
    private var textArea: JTextArea? = null

    init {
        initMyUi()
    }

    fun addEmptyLines() {
        add(Box.createRigidArea(Dimension(0, 10)))
    }

    fun initMyUi() {
        val myLocal = LocalStorage()
        val accessToken = myLocal.loadValue("myKey")
        val acceptTerms = myLocal.loadValueAcceptTerms("acceptTerms")

        val padding: Border = BorderFactory.createEmptyBorder(10, 10, 10, 20)
        layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
        border = padding

        val linkLabel1 = JLabel("<html><u>Log in to OpenAI</u></html>")
        linkLabel1.foreground = JBColor.cyan
        linkLabel1.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        linkLabel1.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                openWebURL("https://chat.openai.com/auth/login")
            }
        })
        add(JLabel("Please start by logging into OpenAI. You can do this by clicking the following link:"))
        addEmptyLines()
        add(linkLabel1)
        addEmptyLines()

        add(JLabel("Next, you'll need to obtain an access token. Simply follow this link, then copy the token you're provided with:"))
        addEmptyLines()
        val linkLabel2 = JLabel("<html><u>Get Your Access Token</u></html>")
        linkLabel2.foreground = JBColor.cyan
        linkLabel2.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        linkLabel2.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                openWebURL("https://chat.openai.com/api/auth/session")
            }
        })
        add(linkLabel2)
        addEmptyLines()

        add(JLabel("Once you've obtained your access token, please paste all the result into the provided field below."))
        addEmptyLines()

        textArea = JTextArea()
        textArea!!.lineWrap = true
        textArea!!.wrapStyleWord = true
        val maxDimension = Dimension(Int.MAX_VALUE, 80)
        val margin = JBUI.Borders.empty(5, 15)
        textArea!!.border = CompoundBorder(textArea!!.border, margin)
        textArea!!.maximumSize = maxDimension
        textArea!!.alignmentX = Component.LEFT_ALIGNMENT
        textArea!!.text = accessToken
        add(textArea)

        val checkBox1 = JCheckBox("By proceeding, I acknowledge and agree that any changes I make to the code lines or classes may be communicated to OpenAI.")
        checkBox1.isSelected = acceptTerms
        addEmptyLines()
        add(checkBox1)
        addEmptyLines()

        val warningLabel = JLabel("In order to utilize this plugin, you might be required to provide your billing information to OpenAI.")
        warningLabel.foreground = JBColor.ORANGE
        add(warningLabel)
        addEmptyLines()
        val button = JButton("Smart Commit")
        val buttonBox: Box = Box.createHorizontalBox()
        buttonBox.add(Box.createHorizontalGlue())
        buttonBox.add(button)
        buttonBox.add(Box.createHorizontalGlue())
        add(buttonBox)

        button.addActionListener {
            var chosenAccessToken: String? = null
            if (accessToken == null || textArea!!.text != null && accessToken != textArea!!.text) {
                if (textArea!!.text != null) {
                    chosenAccessToken = textArea!!.text
                    myLocal.saveValue("myKey", chosenAccessToken)
                }
            }
            chosenAccessToken = myLocal.loadValue("myKey")
            if (chosenAccessToken != null && checkBox1.isSelected) {
                openContent()
            }
        }

        button.isEnabled = checkBox1.isSelected
        checkBox1.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED) {
                myLocal.saveTerms("acceptTerms", true)
                button.isEnabled = true
            } else {
                button.isEnabled = false
                myLocal.saveTerms("acceptTerms", false)
            }
        }
    }

    private fun openWebURL(url: String) {
        try {
            Desktop.getDesktop().browse(URI(url))
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(400, super.getPreferredSize().height)
    }

    fun openContent() {
        val myLocal = LocalStorage()
        val accessToken = myLocal.loadValue("myKey")
        val acceptTerms = myLocal.loadValueAcceptTerms("acceptTerms")
        if (accessToken != null && acceptTerms) {
            VcsNotifier.getInstance(project).notifyImportantInfo("Commit Message Calculating", "We are generating a commit message for you", "")
            openCommitChangesDialog(accessToken)
        }
    }

    private fun openCommitChangesDialog(accessToken: String) {
        val changeListManager = ChangeListManager.getInstance(project)
        val localChangeLists = changeListManager.changeLists

        val changes: MutableList<Change> = ArrayList()
        for (localChangeList in localChangeLists) {
            changes.addAll(localChangeList.changes)
        }

        if (changes.isEmpty()) {
            VcsNotifier.getInstance(project).notifyError("No Changes Detected", "There are no changes to commit.", "")
            return
        }

        val previousVersion: ArrayList<String> = ArrayList()
        val currentVersions: ArrayList<String> = ArrayList()

        for (localChangeList in localChangeLists) {
            val changesX = localChangeList.changes

            for (change in changesX) {
                try {
                    change.beforeRevision?.let { previousVersion.add(it.content!!) }
                    change.afterRevision?.let { currentVersions.add(it.content!!) }
                } catch (e: VcsException) {
                    throw RuntimeException(e)
                }
            }
        }

        val prompt = createAIPromptFromMyLists(previousVersion, currentVersions)
        val initialSelection = changeListManager.defaultChangeList

        val executorService = Executors.newSingleThreadExecutor()
        val future = executorService.submit(Callable {
            var wordCount = 0
            for (changeData in previousVersion) {
                wordCount += countWords(changeData)
            }
            for (changeData in currentVersions) {
                wordCount += countWords(changeData)
            }

            if (wordCount <= 4500) {
                return@Callable sentARequest(prompt, accessToken)
            } else {
                SwingUtilities.invokeLater {
                    VcsNotifier.getInstance(project).notifyError("Commit Message Error", "The changes made to the classes are very extensive", "")
                }
                return@Callable null
            }
        })

        try {
            val body = future.get()  //get result of computation
            if (body != null) {
                SwingUtilities.invokeLater {
                    CommitChangeListDialog.commitChanges(project, changes, initialSelection, null, extractContent(body))
                }
            }
        } catch (e: Exception) {
            println(e.toString())
            VcsNotifier.getInstance(project).notifyError("Error", e.toString(), e.toString())

        } finally {
            executorService.shutdown()
        }
    }

    fun createAIPromptFromMyLists(oldList: ArrayList<String>, newList: ArrayList<String>): String {
        val promptBuilder = StringBuilder()

        promptBuilder.append("Forget all the conversation and please create a concise and descriptive commit message that summarizes the changes made.And commit message couldn't involves words like this;Refactored, etc. And try to be spesific; with given the following changes in the codebase:\n\n")

        for (changeData in oldList) {
            promptBuilder.append("\nThe old version of a class:\n").append(changeData)
        }
        for (changeData in newList) {
            promptBuilder.append("\nThe new version of a class:\n").append(changeData)
        }

        return promptBuilder.toString()
    }

    companion object {
        fun countWords(str: String): Int {
            if (str.isEmpty()) {
                return 0
            }
            val words = str.trim { it <= ' ' }.split("\\s+".toRegex()).toTypedArray()
            return words.size
        }
    }

    private fun sentARequest(prompt: String, accessToken: String): String {

        var url: URL? = null
        try {
            url = URL("https://api.openai.com/v1/chat/completions")
        } catch (e: MalformedURLException) {
            throw RuntimeException(e)
        }
        var conn: HttpURLConnection? = null
        try {
            conn = url.openConnection() as HttpURLConnection
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer $accessToken")
        try {
            conn.requestMethod = "POST"
        } catch (e: ProtocolException) {
            throw RuntimeException(e)
        }
        conn.doOutput = true

        val requestBody = JSONObject()
        requestBody.put("model", "gpt-3.5-turbo")
        val message = JSONObject()
        message.put("role", "user")
        message.put("content", prompt)
        val messagesArray = JSONArray()
        messagesArray.add(message)
        requestBody.put("messages", messagesArray)

        var os: OutputStream? = null
        try {
            os = conn.outputStream
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        try {
            os.write(requestBody.toString().toByteArray())
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        try {
            os.flush()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        try {
            os.close()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        var br: BufferedReader? = null
        try {
            br = BufferedReader(InputStreamReader(conn.inputStream, "utf-8"))
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        var line: String? = null
        val sb = StringBuilder()

        try {
            while (br.readLine().also { line = it } != null) {
                sb.append(line).append("\n")
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        return sb.toString()
    }

    fun extractContent(jsonString: String): String? {
        val contentKey = "\"content\":\""
        val contentStartIndex = jsonString.indexOf(contentKey) + contentKey.length
        val contentEndIndex = jsonString.indexOf("\"", contentStartIndex)

        return if (contentStartIndex < contentKey.length || contentEndIndex == -1) {
            null // content key not found or improperly formatted
        } else {
            jsonString.substring(contentStartIndex, contentEndIndex)
        }
    }
}
