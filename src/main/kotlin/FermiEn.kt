import data.EntryManager
import study_options.ReviewManager
import ui.main_window.MainWindow
import ui.main_window.ReviewPanel
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.nio.charset.StandardCharsets
import javax.swing.JOptionPane
import kotlin.system.exitProcess


const val maxPriority = 10
const val feDefaultPriority = 1

const val indexOfLineWithMostRecentVersionNumber = 6

class Loader {
    companion object {
        val version = fermiEnVersion()

        private fun fermiEnVersion(): String {
            // resources loading from https://mkyong.com/java/java-read-a-file-from-resources-folder/
            val inputStream = Companion::class.java.classLoader.getResourceAsStream("versions.txt")
            try {
                InputStreamReader(inputStream, StandardCharsets.UTF_8).use { streamReader ->
                    BufferedReader(streamReader).use { reader ->
                        var counter = 0
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            if (counter == indexOfLineWithMostRecentVersionNumber) return line!!.split(' ').first()
                            counter++
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return ""
        }
    }
}

fun List<String>.getAt(key: String): String? =
    firstOrNull { it.startsWith(key) }?.split(DEFAULT_SEPARATOR)?.get(1)

fun main() {
    // Avoid multiple instances of FermiEn running at same time. From
    // http://stackoverflow.com/questions/19082265/how-to-ensure-only-one-instance-of-a-java-program-can-be-executed
    try {
        // create object of server socket and bind to some port number
        // ServerSocket(65000, 10, InetAddress.getLocalHost()) // using private port 65000
        val ss = ServerSocket(65000, 10, InetAddress.getLocalHost()) // using private port 65000
        // ServerSocket(14356, 10, InetAddress.getLocalHost())
        // do not put common port number like 80 etc. Because they are already used by system
        // If another instance exists, show message and terminates the current instance.
        // Otherwise starts application.
        println(ss.channel)

        startFermiEn()

    } catch (exc: IOException) {
        JOptionPane.showMessageDialog(
            null, "The application is already running.....",
            "Access Error", JOptionPane.ERROR_MESSAGE
        )
        exitProcess(0)
    }
}

fun startFermiEn() {
    val path = findBreadcrumb();
    println("Found path is $path")
    EntryManager.loadEntries()
    val reviewPanel = ReviewPanel()
    val reviewManager = ReviewManager(reviewPanel)
    reviewManager.initializeReviewSession()
    MainWindow(reviewManager)
}

fun findBreadcrumb(): String? {
    val paths = File.listRoots()
    paths.forEach(::println)
    for (path in paths) {
        println("Found path: $path")
        val found = searchDirectoryRecursively(path)
        if (found != null) return found;
    }
    return null;
}

fun searchDirectoryRecursively(inputPath: File): String? {
    //println("Searching ${inputPath.path}")
    val candidateBreadcrumb = inputPath.path + "\\fermien.breadcrumb";
    //println("test: $candidateBreadcrumb")
    val candidateBreadcrumbFile = File(candidateBreadcrumb);
    if (candidateBreadcrumbFile.exists()) return inputPath.absolutePath

    //println("Maindir: $inputPath / ${inputPath.path} / ${inputPath.name}")
    val directories = inputPath.list() ?: return null
    //for (d in directories) println("Found subdir: $d")
    for (directory in directories) {
        val targetDirectory = inputPath.path + "\\" + directory
        //println("Entering $targetDirectory")
        val found = searchDirectoryRecursively(File(targetDirectory))
        if (found != null) return found;
    }
    return null
}



