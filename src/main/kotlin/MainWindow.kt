import java.awt.event.*
import java.io.File
import javax.swing.*
import kotlin.system.exitProcess


class MainWindow : JFrame() {

    private val table: JTable = JTable(toTableContents(entries), arrayOf("question", "answer"))

    private fun toTableContents(entries: List<Entry>): Array<Array<String>> =
        entries.map { it.toHorizontalDisplay() }.map { arrayOf(it.first, it.second) }.toTypedArray()

    private val scrollPane = JScrollPane(table);


    init {
        addMenu()
        table.fillsViewportHeight = true;
        add(scrollPane)
        setSize(1000, 700)
        defaultCloseOperation = EXIT_ON_CLOSE
        isVisible = true
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(windowEvent: WindowEvent?) {
                saveAndQuit()
            }
        })
    }

    private fun addMenu() {
        jMenuBar = JMenuBar()
        val entryMenu = JMenu("Entry")
        entryMenu.add(createMenuItem("Add Entry", 'n') { EntryEditingWindow() })
        jMenuBar.add(entryMenu)
    }

    private fun createMenuItem(label: String, actionKey: Char, listener: () -> Unit) = JMenuItem(label).apply {
        accelerator =
            KeyStroke.getKeyStroke(KeyEvent.getExtendedKeyCodeForChar(actionKey.code), ActionEvent.CTRL_MASK)
        addActionListener { listener() }
    }

    private fun saveAndQuit() {
        File(inputFileName).writeText(entries.joinToString(separator = "\n") { "${it.question}\t${it.answer}" })
        dispose()
        exitProcess(0)
    }
}