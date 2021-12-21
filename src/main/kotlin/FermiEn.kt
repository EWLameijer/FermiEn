import data.EntryManager
import study_options.ReviewManager
import ui.main_window.MainWindow
import ui.main_window.ReviewPanel
import java.io.File

val version = fermiEnVersion()

fun fermiEnVersion() = File("versions.txt").readLines()[6].split(' ').first()

fun List<String>.getAt(key: String): String? {
    val selectedLine = firstOrNull { it.startsWith(key) }
    return if (selectedLine != null) selectedLine.split(DEFAULT_SEPARATOR)[1]
    else null
}

fun main() {
    println("FermiEn version $version: Start\n")
    EntryManager.loadEntries()
    //EntryManager.printEntries()
    val reviewPanel = ReviewPanel()
    val reviewManager = ReviewManager(reviewPanel)
    reviewManager.initializeReviewSession()
    MainWindow(reviewManager)
}


