import ui.MainWindow
import java.io.File

val version = getFermiEnVersion()

fun getFermiEnVersion() = File("versions.txt").readLines()[6].split(' ').first()

fun prettyPrint(s: StorageString) = s.toLines().forEach(::println)

object Settings {
    private const val statusFileName = "fermien_status.txt"
    private const val lastLoadedEncyKey = "last_loaded_ency"
    private var currentFile: String? = null

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

    fun setCurrentFile(selectedFile: File?) {
        currentFile = selectedFile!!.absolutePath
    }

    fun save() {
        File(statusFileName).writeText("$lastLoadedEncyKey: $currentFile")
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
    MainWindow()
}


