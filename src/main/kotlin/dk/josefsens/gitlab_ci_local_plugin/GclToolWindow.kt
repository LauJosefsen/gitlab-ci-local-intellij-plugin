package dk.josefsens.gitlab_ci_local_plugin

import com.google.common.io.Resources.getResource
import com.intellij.execution.Executor
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.wm.ToolWindow
import org.jetbrains.annotations.NotNull
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.io.File
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath


class GclToolWindow(toolWindow: ToolWindow, project : Project) {
    private var scrollBar: JScrollBar? = null
    private var tree: JTree? = null
    private var refreshButton: JButton? = null
    private var panel: JPanel? = null
    private var project: Project? = null
    private var checkBox: JCheckBox? = null

    init {
        this.project = project
        refreshButton!!.addActionListener { Refresh() }
        Refresh()
    }

    private fun Refresh() {
        // set loading message
        refreshButton?.text = "Loading...";

        // run `gitlab-ci-local --list`
        var stdout = "";
        var stderr = "";

        // run `gitlab-ci-local --list` in project
        project.runCatching { ProcessBuilder("gitlab-ci-local", "--list").directory(File(project?.guessProjectDir()?.path
            ?: "")).start().apply {
            waitFor()
            stdout = inputStream.bufferedReader().readText()
            stderr = errorStream.bufferedReader().readText()
        } }


        val projectDir = project!!.guessProjectDir()!!.path
        try {
            val process =  ProcessBuilder("gitlab-ci-local", "--list").directory(File(projectDir)).start();
            stdout = process.inputStream.bufferedReader().readText();
            stderr = process.errorStream.bufferedReader().readText();
        }
        catch (e: Exception) {
            refreshButton?.text = "Error: " + e.message
            return
        }


        // parse output
        val gclJobs = GclJobFactory().parse(stdout)

        // group jobs by stage
        val stages = ArrayList<GclStage>();
        for (job in gclJobs) {
            var stage = stages.find { it.stage == job.stage }
            if (stage == null) {
                stage = GclStage(job.stage, ArrayList<GclJob>())
                stages.add(stage)
            }
            // add to list
            stage.jobs.add(job)
        }

        // create tree
        val root = DefaultMutableTreeNode(project!!.name)

        for (stage in stages) {
            val stageNode = DefaultMutableTreeNode(stage.stage)
            for (job in stage.jobs) {
                var treeNode = DefaultMutableTreeNode(job.name)
                stageNode.add(DefaultMutableTreeNode(job.name))
            }
            root.add(stageNode)
        }
        val imageIcon = ImageIcon(getResource("general/gearPlain_dark.png"))
        var folderIcon = ImageIcon(getResource("nodes/package.png"))
        val renderer = DefaultTreeCellRenderer()
        renderer.leafIcon = imageIcon
        renderer.openIcon = folderIcon
        renderer.closedIcon = folderIcon
        tree?.cellRenderer = renderer
        tree?.model = DefaultTreeModel(root)

        val ml: MouseListener = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                val selRow = tree!!.getRowForLocation(e.x, e.y)
                val selPath: TreePath? = tree!!.getPathForLocation(e.x, e.y)
                // if selPath is folder
                if (selPath != null && selPath.pathCount > 2) {

                    if (selRow != -1) {
                        if (e.clickCount == 1) {
                        } else if (e.clickCount == 2) {
                            println("Double-clicked")
                            println(selPath.path[2])

                            // create a gcl run configuration
//                            val jobName = selPath.path[2] as String
//                            val job = gclJobs.find { it.name == jobName }


                            var runner: RunnerAndConfigurationSettings = RunManager.getInstance(project!!).createConfiguration(selPath.path[2].toString(), GclRunConfigurationType::class.java);
                            RunManager.getInstance(project!!).addConfiguration(runner);
                            var executor: Executor = DefaultRunExecutor.getRunExecutorInstance();
                            ProgramRunnerUtil.executeConfiguration(runner, executor)
                        }
                    }
                }
            }
        }
        tree!!.addMouseListener(ml)

        // log result to console
        println(stdout);
        println(stderr);

        // update scrollbar
        scrollBar?.setValue(0)

        // update button
        refreshButton?.text = ""
    }
    val content: JComponent?
        get() = panel
}