package sample

import javafx.application.Application
import javafx.beans.property.SimpleStringProperty
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.TreeCell
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeTableView
import javafx.stage.Stage
import jdk.nashorn.api.tree.Tree
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.scene.control.TreeTableColumn
import javafx.util.Callback
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import java.time.Instant
import java.util.*
import kotlin.collections.HashMap


class Main : Application() {
    override fun start(primaryStage: Stage) {
        primaryStage.minWidth = 400.0
        primaryStage.minHeight = 300.0

        val root = FXMLLoader.load<Parent>(javaClass.getResource("sample.fxml"))

        bindTreeGrid(root)

        primaryStage.title = "Hello World"
        primaryStage.scene = Scene(root)
        primaryStage.show()
    }

    private fun bindTreeGrid(root: Parent) {
        val repo = FileRepositoryBuilder().apply {
            isMustExist = true
            gitDir = File("C:\\Source\\github.com\\git\\git\\.git")
        }.build()

        fun walkBreadthFirst(commit: RevCommit, root: TreeItem<RevCommit>) {
            val seen = HashSet<ObjectId>()
            val queue = ArrayDeque<CommitToProcess>()
            queue.push(CommitToProcess(root, commit))
            while(queue.any()) {
                val next = queue.removeFirst()
                if (!seen.add(next.commit.id)) {
                    continue
                }
                val ti = TreeItem<RevCommit>(next.commit)
                next.parent.children.add(ti)
                if (next.commit.parentCount == 0) {
                    continue
                }
                queue.addFirst(CommitToProcess(next.parent, repo.parseCommit(next.commit.getParent(0))))
                for (i in 1 until next.commit.parentCount) {
                    queue.addLast(CommitToProcess(ti, repo.parseCommit(next.commit.getParent(i))))
                }
            }
        }

        val ttv = root.lookup("#goose") as TreeTableView<RevCommit>
        ttv.isShowRoot = false
        val rootItem = TreeItem<RevCommit>()
        val column = ttv.columns[0]
        column.cellValueFactory = Callback { ReadOnlyStringWrapper(it.value.value?.id?.name) }

        val descColumn = ttv.columns[1]
        descColumn.cellValueFactory = Callback { ReadOnlyStringWrapper(it.value.value?.shortMessage) }

        var next = repo.parseCommit(repo.resolve("HEAD"))
        println(Instant.now())

        walkBreadthFirst(next, rootItem)

        println(Instant.now())

        ttv.root = rootItem
    }

    data class CommitToProcess(val parent: TreeItem<RevCommit>, val commit: RevCommit)
}

fun main(args: Array<String>) {
    Application.launch(Main::class.java, *args)
}