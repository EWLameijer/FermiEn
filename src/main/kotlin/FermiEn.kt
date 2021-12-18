import data.EntryManager
import data.StorageString
import study_options.Review
import study_options.ReviewManager
import study_options.StudyOptions
import ui.main_window.MainWindow
import ui.ReviewPanel
import java.io.File
import java.time.Duration

val version = fermiEnVersion()

fun fermiEnVersion() = File("versions.txt").readLines()[6].split(' ').first()

fun prettyPrint(s: StorageString) = s.toLines().forEach(::println)

object Settings {
    private const val statusFileName = "fermien_status.txt"
    private const val lastLoadedEncyKey = "last_loaded_ency"
    private var currentFile: String? = null
    var studyOptions = StudyOptions()

    fun currentFile(): String {
        if (currentFile == null) {
            val statusFile = File(statusFileName)
            if (statusFile.isFile) {
                val lines = File(statusFileName).readLines()
                currentFile = lines.getAt(lastLoadedEncyKey)
            }
            currentFile = currentFile ?: "notes.txt"
        }
        return currentFile!!
    }

    fun currentRepetitionsFile() : String {
        val entryFile = currentFile()
        return entryFile.replace(".txt", "_reps.txt")
    }

    fun setCurrentFile(selectedFile: File?) {
        currentFile = selectedFile!!.absolutePath
    }

    fun save() {
        File(statusFileName).writeText("$lastLoadedEncyKey: $currentFile")
    }

    fun intervalDurationFromUserSettings(reviews: List<Review>): Duration =
        studyOptions.intervalSettings.calculateNextIntervalDuration(reviews)

    fun currentSettingsFile(): String {
        val entryFile = currentFile()
        return entryFile.replace(".txt", "_settings.txt")
    }
}

fun List<String>.getAt(key: String): String? {
    val selectedLine = firstOrNull { it.startsWith(key) }
    return if (selectedLine != null) selectedLine.split(": ")[1]
    else null
}

fun main() {
    println("FermiEn version $version: Start\n")
    EntryManager.loadEntries()
    EntryManager.printEntries()
    val reviewPanel = ReviewPanel()
    val reviewManager = ReviewManager(reviewPanel)
    reviewManager.initializeReviewSession()
    MainWindow(reviewManager)
}


