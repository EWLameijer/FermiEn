package ui.main_window

import Update
import UpdateType
import data.*
import doNothing
import eventhandling.BlackBoard
import eventhandling.DelegatingDocumentListener
import fermiEnVersion
import study_options.Analyzer
import study_options.ReviewManager
import ui.*
import java.awt.CardLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.*
import java.io.File
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.table.DefaultTableModel
import kotlin.system.exitProcess


// MainWindowMode is what is in the main window. Is either Display or Reviewing
// ReviewingState is either INFORMATIONAL, SUMMARIZING, REACTIVE or REVIEWING
// together (WITH whether there are cards to review now) they indicate one of four possible panels.

enum class MainWindowMode { DISPLAY, REVIEW }
enum class ReviewingState { INFORMATIONAL, REACTIVE, REVIEWING, SUMMARIZING }

const val displayId = "DISPLAY"
const val reviewingId = "REVIEWING"
const val informationalId = "INFORMATIONAL"
const val summarizingId = "SUMMARIZING"

class MainWindow(reviewManager: ReviewManager) : JFrame() {
    private var reviewState = ReviewingState.REACTIVE

    private var mainMode = if (reviewManager.hasNextCard()) MainWindowMode.REVIEW else MainWindowMode.DISPLAY

    private val entryPanel = JPanel()

    private lateinit var entryEditingPanel : EntryEditingPanel

    private val modesContainer = JPanel()

    private val summarizingPanel = SummarizingPanel(reviewManager)

    private val informationPanel = InformationPanel(reviewManager)

    private val startReviewingMenuItem = createMenuItem("Start reviewing", 'r') { startReviewing() }

    private val goToEntryListMenuItem = createMenuItem("Go to list of entries", 'l') { goToEntryList() }

    private fun nameOfLastUsedEncyDirectory() = Settings.currentFile()!!.pathPart()

    private var messageUpdater: Timer? = null

    private val fileMenu = JMenu("File")

    class UnchangeableTableModel : DefaultTableModel() {
        override fun isCellEditable(row: Int, column: Int): Boolean {
            //all cells false
            return false
        }
    }

    // from https://stackoverflow.com/questions/9467093/how-to-add-a-tooltip-to-a-cell-in-a-jtable
    private val table = object : JTable() {
        override fun getToolTipText(e: MouseEvent): String? {
            var tip: String? = null
            val p = e.point
            val rowIndex = rowAtPoint(p)
            val colIndex = columnAtPoint(p)
            try {
                tip = getValueAt(rowIndex, colIndex).toString().linesOfMaxLength(100).inHtml()
            } catch (_: RuntimeException) {
                //catch null pointer exception if mouse is over an empty line
            }
            return tip
        }
    }

    private val scrollPane = JScrollPane(table)



    private val searchField = JTextField().apply {
        document.addDocumentListener(searchFieldListener)
    }

    private fun updateTable() {
        println("Updating table!")
        val tableModel = UnchangeableTableModel()
        tableModel.addColumn("question")
        tableModel.addColumn("answer")
        EntryManager.getHorizontalRepresentation().filter(::searchContentsInHorizontalEntry)
            .sortedBy { it.first.lowercase() }.forEach {
                tableModel.addRow(arrayOf(it.first, it.second))
            }
        table.model = tableModel
    }

    val searchFieldListener = DelegatingDocumentListener { updateTable() }

    private fun searchContentsInHorizontalEntry(entry: Pair<String, String>): Boolean {
        // TODO: should also look at
        val editWindowTerms =
            if (entryEditingPanel.isVisible) entryEditingPanel.question().toHorizontalString() + " " else ""
        val searchFieldTerms = searchField.text
        val allTerms = (editWindowTerms + searchFieldTerms).lowercase().split(' ')
        return allTerms.all { it in entry.first.lowercase() || it in entry.second.lowercase() }
    }

    init {
        BlackBoard.register({ updateTable() }, UpdateType.ENCY_CHANGED, UpdateType.ENCY_SWAPPED)
        modesContainer.layout = CardLayout()
        createKeyListener(KeyEvent.VK_ESCAPE) {
            with(searchField) {
                text = ""
                requestFocusInWindow()
            }
            entryEditingPanel.isVisible = false
        }
        BlackBoard.register(::respondToUpdate, UpdateType.PROGRAMSTATE_CHANGED)
        BlackBoard.register(::respondToUpdate, UpdateType.ENCY_SWAPPED)

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
                    val key = table.getValueAt(row, 0) as String
                    EntryManager.editEntryByQuestion(key)
                }
            }
        })
        entryEditingPanel = EntryEditingPanel(this)
        setupEntryPanel()

        // note: container.add needs string, so .name here (or "$"), despite it seeming overkill
        modesContainer.add(entryPanel, displayId)
        modesContainer.add(reviewManager.reviewPanel, reviewingId)
        modesContainer.add(summarizingPanel, summarizingId)
        modesContainer.add(informationPanel, informationalId)
        add(modesContainer)
        setSize(1000, 700)
        defaultCloseOperation = EXIT_ON_CLOSE
        isVisible = true
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(windowEvent: WindowEvent?) {
                saveAndQuit()
            }
        })

        updateOnScreenInformation()
        showCorrectPanel()
        messageUpdater = Timer(100) {
            updateWindowTitle()
            informationPanel.updateMessageLabel()
        }
        messageUpdater!!.start()
        iconImage = ImageIcon("resources/FermiEn.png").image

    }

    private fun setupEntryPanel() {
        val editCardConstraints = GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            weightx = 1.0
            weighty = 1.0
            insets = Insets(0, 0, 0, 0)
            fill = GridBagConstraints.BOTH
            gridheight = 2
        }
        val searchBoxConstraints = GridBagConstraints().apply {
            gridx = 1
            gridy = 0
            weightx = 1.0
            weighty = 1.0
            insets = Insets(0, 0, 0, 0)
            fill = GridBagConstraints.BOTH
        }
        val tableConstraints = GridBagConstraints().apply {
            gridx = 1
            gridy = 1
            weightx = 1.0
            weighty = 1000.0
            insets = Insets(0, 0, 0, 0)
            fill = GridBagConstraints.BOTH
        }
        entryPanel.apply {
            layout = GridBagLayout()
            add(entryEditingPanel, editCardConstraints)
            entryEditingPanel.isVisible = false;
            add(searchField, searchBoxConstraints)
            add(scrollPane, tableConstraints)
        }
    }

    // Updates the title of the window, which contains information like the number of cards in the deck
    private fun updateWindowTitle() {
        val numReviewingPoints = EntryManager.reviewingPoints()
        val shortCutCode = getShortCutCode(Settings.getShortcutIdOfCurrentDeck())
        var title = "FermiEn ${fermiEnVersion()}: $shortCutCode ${Settings.currentFile()!!.fileNamePart()}"
        val entries = EntryManager.entries().size
        title += ", ${"entry".pluralize(entries)} in deck, ${"point".pluralize(numReviewingPoints)}"
        this.title = title
    }

    private fun getShortCutCode(id: Int?) =
        if (id == null) ""
        else {
            val (command, realIndex) = if (id > 9) 'A' to id - 10 else 'C' to id
            "[$command$realIndex]"
        }

    private fun getCorrectPanelId() =
        if (mainMode == MainWindowMode.DISPLAY) displayId
        else when (reviewState) {
            ReviewingState.INFORMATIONAL -> informationalId
            ReviewingState.REVIEWING -> reviewingId
            ReviewingState.SUMMARIZING -> summarizingId
            ReviewingState.REACTIVE -> if (EntryManager.reviewableEntries()
                    .isNotEmpty()
            ) reviewingId else informationalId
        }

    private fun showCorrectPanel() {
        val cardLayout = modesContainer.layout as CardLayout
        cardLayout.show(modesContainer, getCorrectPanelId())
        goToEntryListMenuItem.isEnabled = mainMode != MainWindowMode.DISPLAY
        startReviewingMenuItem.isEnabled = mainMode == MainWindowMode.DISPLAY
    }

    private fun rebuildFileMenu() {
        fileMenu.removeAll()
        fileMenu.add(createMenuItem("Create or Load Encyclopedia", 'o', ::createEncyFile))
        fileMenu.add(createMenuItem("Add Entries (Eb format)", 'e', ::importEbText))
        fileMenu.add(createMenuItem("Add Entries (FermiEn format)", 'f', ::importEncyText))
        fileMenu.add(createMenuItem("Quit", 'q', ::saveAndQuit))
        addDeckLoadingMenuItems(fileMenu)
    }

    private fun addMenu() {
        jMenuBar = JMenuBar()
        rebuildFileMenu()
        val encyMenu = JMenu("Encyclopedia Settings")
        encyMenu.add(createMenuItem("Study Settings", 't') { StudyOptionsWindow.display() })
        encyMenu.add(createMenuItem("Analyze Ency", 'z') { Analyzer.run() })
        val modeMenu = JMenu("Mode")
        modeMenu.add(startReviewingMenuItem)
        modeMenu.add(goToEntryListMenuItem)
        val entryMenu = JMenu("Entry")
        entryMenu.add(createMenuItem("Add Entry", 'n') {
            entryEditingPanel.isVisible = true
        })
        jMenuBar.add(fileMenu)
        jMenuBar.add(encyMenu)
        jMenuBar.add(entryMenu)
        jMenuBar.add(modeMenu)
    }

    private fun manageDeckShortcuts() {
        DeckShortcutsPopup().updateShortcuts()
        rebuildFileMenu()
    }

    private fun addDeckLoadingMenuItems(fileMenu: JMenu) {
        fileMenu.addSeparator()
        fileMenu.add(createMenuItem("Manage deck-shortcuts", '0', ::manageDeckShortcuts))
        (1..9).filter { Settings.shortcuts[it] != null }.forEach { digit ->
            val encyFileName = Settings.shortcuts[digit]!!
            fileMenu.add(
                createMenuItem(
                    "Load deck '${encyFileName.fileNamePart()}'",
                    digit.digitToChar()
                ) { EntryManager.loadEntriesFrom(encyFileName) })
        }
        (10..Settings.maxNumShortcuts).filter { Settings.shortcuts[it] != null }
            .forEach { rawIndex ->
                val encyFileName = Settings.shortcuts[rawIndex]!!
                val digit = rawIndex - 10 // deck 11 becomes Alt+1 etc.
                fileMenu.add(
                    createAltMenuItem(
                        "Load deck '${encyFileName.fileNamePart()}'",
                        digit.digitToChar()
                    ) { EntryManager.loadEntriesFrom(encyFileName) })
            }
    }

    private fun createAltMenuItem(label: String, actionKey: Char, listener: () -> Unit) = JMenuItem(label).apply {
        accelerator =
            KeyStroke.getKeyStroke(KeyEvent.getExtendedKeyCodeForChar(actionKey.code), ActionEvent.ALT_MASK)
        addActionListener { listener() }
    }

    private fun importEbText() = importText(::ebConverter)

    private fun importEncyText() = importText(::encyConverter)

    private fun importText(fileConverter: (String) -> Unit) {
        val chooser = JFileChooser(nameOfLastUsedEncyDirectory()).apply {
            fileFilter = FileNameExtensionFilter("Text files", "txt")
        }
        val result = chooser.showOpenDialog(this)
        if (result == JFileChooser.CANCEL_OPTION) {
            return
        } else {
            val selectedFile = chooser.selectedFile
            fileConverter(selectedFile.absolutePath)
        }
    }

    private fun ebConverter(filename: String) {
        File(filename).readLines().forEach { EntryManager.addEntry(doubleTabToEntry(it)) }
    }

    private fun encyConverter(filename: String) {
        File(filename).readLines().forEach { EntryManager.addEntry(it.toEntry()) }
    }

    private fun doubleTabToEntry(line: String): Entry {
        val (question, answer) = line.split("\t\t")
        return Entry(question.toStorageString(), answer.toStorageString())
    }

    private fun createEncyFile() {
        val chooser = JFileChooser(nameOfLastUsedEncyDirectory()).apply {
            fileFilter = FileNameExtensionFilter("Encyclopedia files", "txt")
        }
        val result = chooser.showOpenDialog(this)
        if (result == JFileChooser.CANCEL_OPTION) {
            return
        } else {
            val selectedFile = chooser.selectedFile
            val selectedFilepath = selectedFile.absolutePath
            if (!selectedFile.exists()) {
                File(selectedFilepath).writeText("")
            }
            EntryManager.loadEntriesFrom(selectedFilepath)
        }
    }

    private fun goToEntryList() {
        mainMode = MainWindowMode.DISPLAY
        showCorrectPanel()
        searchField.requestFocusInWindow()
    }

    private fun startReviewing() {
        mainMode = MainWindowMode.REVIEW
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

    private fun respondToUpdate(update: Update) = when (update.type) {
        UpdateType.ENCY_SWAPPED -> {
            //Personalisation.updateTimeOfCurrentDeckReview()
            informationPanel.updateMessageLabel()
            updateWindowTitle()
            showCorrectPanel()
        }
        UpdateType.PROGRAMSTATE_CHANGED -> {
            reviewState = ReviewingState.valueOf(update.contents)
            //reviewPanel.refresh() // there may be new cards to refresh
            updateOnScreenInformation()
            showCorrectPanel()
        }
        /*UpdateType.DECK_SWAPPED -> {
            val newState =
                if (mustReviewNow()) MainWindowState.REVIEWING
                else MainWindowState.REACTIVE
            BlackBoard.post(Update(UpdateType.PROGRAMSTATE_CHANGED, newState.name))
        }*/
        else -> doNothing
    }

    // Gives the message label its correct (possibly updated) value.
    private fun updateOnScreenInformation() {
        //updateMenuIfNeeded()
        informationPanel.updateMessageLabel()
        //updateWindowTitle()
    }
}

