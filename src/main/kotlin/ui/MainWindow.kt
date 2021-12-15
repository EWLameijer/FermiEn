package ui

import DelegatingDocumentListener
import EntryManager
import Settings
import createKeyListener
import java.awt.CardLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.*
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.table.DefaultTableModel
import kotlin.system.exitProcess

enum class MainWindowState { LIST_ENTRIES, REVIEWING }

class MainWindow : JFrame() {
    private var mainState = MainWindowState.LIST_ENTRIES

    private val reviewPanel = ReviewPanel()

    private val entryPanel = JPanel()

    private val modesContainer = JPanel()

    private val startReviewingMenuItem = createMenuItem("Start reviewing", 'r') { startReviewing() }
    private val goToEntryListMenuItem = createMenuItem("Go to list of entries", 'l') { goToEntryList() }

    private val nameOfLastUsedEncyDirectory = ""

    class UnchangeableTableModel : DefaultTableModel() {
        override fun isCellEditable(row: Int, column: Int): Boolean {
            //all cells false
            return false
        }
    }

    private val table = JTable()

    private val scrollPane = JScrollPane(table)

    private val searchFieldListener = DelegatingDocumentListener { updateTable() }

    private val searchField = JTextField().apply {
        document.addDocumentListener(searchFieldListener)
    }

    private fun updateTable() {
        val tableModel = UnchangeableTableModel()
        tableModel.addColumn("question")
        tableModel.addColumn("answer")
        EntryManager.getHorizontalRepresentation().filter(::searchContentsInHorizontalEntry)
            .sortedBy { it.first.lowercase() }.forEach {
                tableModel.addRow(arrayOf(it.first, it.second))
            }
        table.model = tableModel
    }

    private fun searchContentsInHorizontalEntry(entry: Pair<String, String>): Boolean {
        val term = searchField.text.lowercase()
        return term in entry.first.lowercase() || term in entry.second.lowercase()
    }

    init {
        EntryManager.registerAsListener(::updateTable)
        modesContainer.layout = CardLayout()
        createKeyListener(KeyEvent.VK_ESCAPE) { searchField.text = "" }

        addMenu()
        val tableModel = UnchangeableTableModel()
        tableModel.addColumn("question")
        tableModel.addColumn("answer")
        EntryManager.getHorizontalRepresentation().sortedBy { it.first.lowercase() }.forEach {
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
                    val key = table.getValueAt(row, 0) as String
                    EntryManager.editEntryByKey(key)
                }
            }
        })
        entryPanel.layout = GridBagLayout()
        val searchBoxConstraints = GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            weightx = 1.0
            weighty = 1.0
            insets = Insets(0, 0, 0, 0)
            fill = GridBagConstraints.BOTH
        }
        val tableConstraints = GridBagConstraints().apply {
            gridx = 0
            gridy = 1
            weightx = 1.0
            weighty = 1000.0
            insets = Insets(0, 0, 0, 0)
            fill = GridBagConstraints.BOTH
        }

        entryPanel.add(searchField, searchBoxConstraints)
        entryPanel.add(scrollPane, tableConstraints)
        modesContainer.add(entryPanel, MainWindowState.LIST_ENTRIES.name)
        modesContainer.add(reviewPanel, MainWindowState.REVIEWING.name)
        add(modesContainer)
        setSize(1000, 700)
        defaultCloseOperation = EXIT_ON_CLOSE
        isVisible = true
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(windowEvent: WindowEvent?) {
                saveAndQuit()
            }
        })
        showCorrectPanel()
    }

    private fun showCorrectPanel() {
        val cardLayout = modesContainer.layout as CardLayout
        cardLayout.show(modesContainer, mainState.name)
        goToEntryListMenuItem.isEnabled = mainState == MainWindowState.REVIEWING
        startReviewingMenuItem.isEnabled = mainState == MainWindowState.LIST_ENTRIES
    }

    private fun addMenu() {
        jMenuBar = JMenuBar()
        val fileMenu = JMenu("File")
        fileMenu.add(createMenuItem("New Encyclopedia", 'o') { createEncyFile() })
        val modeMenu = JMenu("Mode")
        modeMenu.add(startReviewingMenuItem)
        modeMenu.add(goToEntryListMenuItem)
        val entryMenu = JMenu("Entry")
        entryMenu.add(createMenuItem("Add Entry", 'n') { EntryEditingWindow() })
        jMenuBar.add(fileMenu)
        jMenuBar.add(entryMenu)
        jMenuBar.add(modeMenu)
    }

    private fun createEncyFile() {
        val chooser = JFileChooser(nameOfLastUsedEncyDirectory).apply {
            fileFilter = FileNameExtensionFilter("Encyclopedia files", "txt")
        }
        val result = chooser.showOpenDialog(this)
        if (result == JFileChooser.CANCEL_OPTION) {
            return
        } else {
            EntryManager.saveEntriesToFile()
            EntryManager.clearEntries()
            val selectedFile = chooser.selectedFile
            Settings.setCurrentFile(selectedFile)
        }
    }

    private fun goToEntryList() {
        mainState = MainWindowState.LIST_ENTRIES
        showCorrectPanel()
    }

    private fun startReviewing() {
        mainState = MainWindowState.REVIEWING
        showCorrectPanel()
    }

    private fun createMenuItem(label: String, actionKey: Char, listener: () -> Unit) = JMenuItem(label).apply {
        accelerator =
            KeyStroke.getKeyStroke(KeyEvent.getExtendedKeyCodeForChar(actionKey.code), ActionEvent.CTRL_MASK)
        addActionListener { listener() }
    }

    private fun saveAndQuit() {
        EntryManager.saveEntriesToFile()
        dispose()
        exitProcess(0)
    }
}