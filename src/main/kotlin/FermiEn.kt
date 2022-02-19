import data.EntryManager
import study_options.ReviewManager
import ui.main_window.MainWindow
import ui.main_window.ReviewPanel
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import javax.swing.JOptionPane
import kotlin.system.exitProcess

const val indexOfLineWithMostRecentVersionNumber = 6

fun fermiEnVersion(): String =
    with(File("versions.txt")) {
        if (isFile) readLines()[indexOfLineWithMostRecentVersionNumber].split(' ').first()
        else ""
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
    EntryManager.loadEntries()
    val reviewPanel = ReviewPanel()
    val reviewManager = ReviewManager(reviewPanel)
    reviewManager.initializeReviewSession()
    MainWindow(reviewManager)
}


