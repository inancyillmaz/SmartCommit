package com.inanc.smartcommit.presentation

import com.inanc.smartcommit.domain.openWebURL
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import java.awt.Cursor
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JLabel

fun createBrowsableLink(title: String, url: String, onError: (String?) -> Unit): JLabel {
    val linkLabel1 = JLabel("<html><u>$title</u></html>")
    linkLabel1.foreground = JBColor.cyan
    linkLabel1.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    linkLabel1.addMouseListener(object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
            url.openWebURL {
                onError(it)
            }
        }
    })
    return linkLabel1
}

fun generateLabel(text: String): JBLabel {
    val html = "<html>$text</html>"
    return JBLabel(html).apply {
        val size: Dimension = preferredSize
        size.width = 200
    }
}
