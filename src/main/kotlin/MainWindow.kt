import java.awt.event.*
import java.io.File
import javax.swing.*
import javax.swing.table.DefaultTableModel
import kotlin.system.exitProcess


class MainWindow : JFrame() {
    class UnchangeableTableModel : DefaultTableModel() {
        override fun isCellEditable(row: Int, column: Int): Boolean {
            //all cells false
            return false
        }
    }

    private val table = JTable()

    private val scrollPane = JScrollPane(table)

    init {
        addMenu()
        val tableModel = UnchangeableTableModel()
        tableModel.addColumn("question")
        tableModel.addColumn("answer")
        entries.map { it.toHorizontalDisplay() }.sortedBy { it.first.lowercase() }.forEach {
            tableModel.addRow(arrayOf(it.first, it.second))
        }
        table.model = tableModel
        table.fillsViewportHeight = true
        table.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(mouseEvent: MouseEvent) {
                val table = mouseEvent.source as JTable
                val point = mouseEvent.point
                val row = table.rowAtPoint(point)
                if (mouseEvent.clickCount == 2 && table.selectedRow != -1) {
                    println("Clicked row $row")
                    val key = table.getValueAt(row,0) as String
                    showEntryByKey(key)
                }
            }
        })

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

    private fun showEntryByKey(key: String) {
        val selectedEntry = entries.find { it.toHorizontalDisplay().first == key }
        EntryEditingWindow(selectedEntry)
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