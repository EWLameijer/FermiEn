import data.EntryManager
import data.baseDirectoryPath
import data.encyDirectory
import data.nameOfStatusFile
import study_options.ReviewManager
import ui.closeOptionPane
import ui.main_window.MainWindow
import ui.main_window.ReviewPanel
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import javax.swing.JButton
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.system.exitProcess

const val maxPriority = 10
const val feDefaultPriority = 1

const val indexOfLineWithMostRecentVersionNumber = 6

class Loader {
    companion object {
        val version = fermiEnVersion()

        private fun fermiEnVersion(): String {
            // resources loading from https://mkyong.com/java/java-read-a-file-from-resources-folder/
            val inputStream = Companion::class.java.classLoader.getResourceAsStream("versions.txt")!!
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
    setupIfNeeded()
    EntryManager.loadEntries()
    MainWindow()
}

// what should happen?
// If someone puts the programme on the desktop, a fermien_data folder should be made
// in that folder: settings and an ency-folder
// so what should happen:
// when running fermi_en
// check if there is a fermien_data folder at this location
// if so, read the settings from the fermien_data and continue
// if NOT:
// show a dialog, saying that this is a new location
// do they want to install FermiEn here or is there a previous installation?
// IF they want to install FermiEn here, create the directory with settings etc.
// IF there is a previous installation, let user go to that directory
// you still need to create a directory with settings
fun setupIfNeeded() {
    val directory = File(baseDirectoryPath)
    if (directory.exists() && directory.isDirectory) return
    val buttons = arrayOf(createYesButton(), createNoButton())
    JOptionPane.showOptionDialog(
        null,
        "Is there a previous installation of FermiEn?",
        "New installation or location", 0,
        JOptionPane.QUESTION_MESSAGE, null, buttons, null
    )
}

private fun createYesButton() =
    JButton("Yes").apply {
        addActionListener {
            var setupFile : File?
            do {
                setupFile = EmergencyFrame().findInstallation()
            } while (setupFile == null)
            setupNewWithStatus(setupFile.readText())
        }
    }

private fun createNoButton() =
    JButton("No").apply {
        addActionListener { setupNew() }
    }

fun setupNew() = setupNewWithStatus("")

fun setupNewWithStatus(status: String){
    val path = Paths.get(baseDirectoryPath)
    Files.createDirectory(path)
    val encyPath = Paths.get(encyDirectory)
    Files.createDirectory(encyPath)
    File("$baseDirectoryPath/$nameOfStatusFile").writeText(status)
    closeOptionPane()
}

class EmergencyFrame : JFrame() {
    fun findInstallation() : File? {
        val chooser = JFileChooser("").apply {
            fileFilter = FileNameExtensionFilter("FermiEn files", "txt")
        }
        chooser.dialogTitle = "Please select the current FermiEn settings file ($nameOfStatusFile)"
        val result = chooser.showOpenDialog(this)
        if (result == JFileChooser.CANCEL_OPTION) {
            return null
        } else {
            val selectedFile = chooser.selectedFile
            if (!selectedFile.endsWith("fermien_status.txt")) return null
            return selectedFile
        }
    }
}


