package dk.josefsens.gitlab_ci_local_plugin

import com.google.common.io.Resources.getResource
import com.intellij.execution.Executor
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessOutput
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import dk.josefsens.gitlab_ci_local_plugin.dk.josefsens.gitlab_ci_local_plugin.WslUtils
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.io.File
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath


class GclToolWindow(project : Project) {
    private var tree: JTree? = null
    private var refreshButton: JButton? = null
    private var panel: JPanel? = null
    private var project: Project? = null
    private var checkBox: JCheckBox? = null
    private var textArea: JTextArea? = null

    init {
        this.project = project
        refreshButton!!.addActionListener { Refresh() }
        Refresh()
    }
    private fun Refresh() {
        // set loading message
        refreshButton?.text = "Loading..."
        println(project?.basePath)

        val task: Task.Backgroundable = object : Task.Backgroundable(project, "Loading...", true) {
            override fun run(indicator: ProgressIndicator) {
                val output = GclList()
                refreshButton?.text = ""
                if(output.exitCode != 0){
                    textArea?.text = output.stderr
                    return
                }
                textArea?.text = ""

                // parse output
                val gclJobs = GclJobFactory().parse(output.stdout)

                // display output
                showGclJobs(gclJobs)
            }
        }
        ProgressManager.getInstance().run(task)
    }

    private fun GclList(): ProcessOutput {
        val runConfiguration = GeneralCommandLine(WslUtils.rewriteToWslExec(project!!.basePath!!, listOf("gitlab-ci-local", "--list")))
            .withRedirectErrorStream(true)
        runConfiguration.workDirectory = File(project!!.basePath!!)
        return ExecUtil.execAndGetOutput(runConfiguration);
    }

    fun showGclJobs(jobs: List<GclJob>) {
        // group jobs by stage
        val stages = ArrayList<GclStage>();
        for (job in jobs) {
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

        // add mouse listener
        addMouseListener()
    }

    fun addMouseListener() {
        val ml: MouseListener = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                val selRow = tree!!.getRowForLocation(e.x, e.y)
                val selPath: TreePath? = tree!!.getPathForLocation(e.x, e.y)
                if (selPath != null && selPath.pathCount > 2 && selRow != -1 && e.clickCount == 2) {
                    val script = selPath.path[2].toString() + " " + (if(checkBox!!.isSelected) "--needs" else "--no-needs")
                    val runner: RunnerAndConfigurationSettings = RunManager.getInstance(project!!)
                        .createConfiguration(script, GclRunConfigurationType::class.java);
                    RunManager.getInstance(project!!).addConfiguration(runner);
                    val executor: Executor = DefaultRunExecutor.getRunExecutorInstance();
                    ProgramRunnerUtil.executeConfiguration(runner, executor)
                }
            }
        }
        tree!!.addMouseListener(ml)
    }


    val content: JComponent?
        get() = panel
}