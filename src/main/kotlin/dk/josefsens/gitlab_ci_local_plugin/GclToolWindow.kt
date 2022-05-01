package dk.josefsens.gitlab_ci_local_plugin

import com.intellij.openapi.wm.ToolWindow
import java.awt.event.ActionEvent
import javax.swing.*

class GclToolWindow(toolWindow: ToolWindow) {
    private var scrollBar: JScrollBar? = null
    private var list: JList<String>? = null
    private var refreshButton: JButton? = null
    private var panel: JPanel? = null

    init {
        refreshButton!!.addActionListener { Refresh() }
        Refresh()
    }

    private fun Refresh() {
        // set loading message
        refreshButton?.text = "Loading...";

        // run `gitlab-ci-local --list`
        var result = ""
        try {
            result = ProcessBuilder("gitlab-ci-local", "--list").start().inputStream.bufferedReader().readText()
        }
        catch (e: Exception) {
            refreshButton?.text = "Error: " + e.message
            return
        }

        // update list
        list?.setListData(result.split("\n").toTypedArray())

        // update scrollbar
        scrollBar?.setValue(0)

        // update button
        refreshButton?.text = ""
    }
    val content: JComponent?
        get() = panel
}