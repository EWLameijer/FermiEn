package ui.main_window

import data.EntryManager
import Settings
import Update
import ui.createKeyListener
import data.Entry
import data.pluralize
import data.toStorageString
import doNothing
import eventhandling.BlackBoard
import eventhandling.DelegatingDocumentListener
import fermiEnVersion
import study_options.Analyzer
import study_options.ReviewManager
import ui.EntryEditingWindow
import ui.StudyOptionsWindow
import java.awt.*
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
enum class ReviewingState { INFORMATIONAL, REACTIVE, REVIEWING, SUMMARIZING  }

const val displayId = "DISPLAY"
const val reviewingId = "REVIEWING"
const val informationalId = "INFORMATIONAL"
const val summarizingId = "SUMMARIZING"

class MainWindow(private val reviewManager: ReviewManager) : JFrame() {
    private var reviewState = ReviewingState.REVIEWING
    private var mainMode = if (reviewManager.hasNextCard()) MainWindowMode.REVIEW else MainWindowMode.DISPLAY

    private val entryPanel = JPanel()

    private val modesContainer = JPanel()

    private val summarizingPanel = SummarizingPanel(reviewManager)

    private val informationPanel = InformationPanel(reviewManager)

    private val startReviewingMenuItem = createMenuItem("Start reviewing", 'r') { startReviewing() }

    private val goToEntryListMenuItem = createMenuItem("Go to list of entries", 'l') { goToEntryList() }

    private val nameOfLastUsedEncyDirectory = ""

    private var messageUpdater: Timer? = null

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
        createKeyListener(KeyEvent.VK_ESCAPE) {
            with(searchField) {
                text = ""
                requestFocusInWindow()
            }
        }
        BlackBoard.register(::respondToUpdate, UpdateType.PROGRAMSTATE_CHANGED)

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
        iconImage = ImageIcon("FermiEn.png").image
    }

    // Updates the title of the window, which contains information like the number of cards in the deck
    private fun updateWindowTitle() {
        val numReviewingPoints = EntryManager.reviewingPoints()

        //val shortCutCode = getShortCutCode(currentDeck.name)
        var title =
            "FermiEn ${fermiEnVersion()}: ${Settings.currentFile().takeLastWhile { it != '\\' }.removeSuffix(".txt")}"
        /*if (state == MainWindowState.REVIEWING) {
    title += (", ${"card".pluralize(ReviewManager.cardsToGoYet())} yet to be reviewed in the current session")
}*/
        val entries = EntryManager.entries().size
        title += ", ${"entry".pluralize(entries)} in deck, ${"point".pluralize(numReviewingPoints)}"

        this.title = title
    }

    fun getCorrectPanelId() = if (mainMode == MainWindowMode.DISPLAY) displayId
    else when(reviewState) {
        ReviewingState.INFORMATIONAL -> informationalId
        ReviewingState.REVIEWING -> reviewingId
        ReviewingState.SUMMARIZING -> summarizingId
        ReviewingState.REACTIVE -> if (EntryManager.reviewableEntries().isNotEmpty()) reviewingId else informationalId
    }

    private fun showCorrectPanel() {
        val cardLayout = modesContainer.layout as CardLayout
        cardLayout.show(modesContainer, getCorrectPanelId())
        goToEntryListMenuItem.isEnabled = mainMode != MainWindowMode.DISPLAY
        startReviewingMenuItem.isEnabled = mainMode == MainWindowMode.DISPLAY
    }

    private fun addMenu() {
        jMenuBar = JMenuBar()
        val fileMenu = JMenu("File")
        fileMenu.add(createMenuItem("New Encyclopedia", 'o', ::createEncyFile))
        fileMenu.add(createMenuItem("Import Text", 'i', ::importText))
        fileMenu.add(createMenuItem("Quit", 'q', ::saveAndQuit))
        val encyMenu = JMenu("Encyclopedia Settings")
        encyMenu.add(createMenuItem("Study Settings", 't') { StudyOptionsWindow.display() })
        encyMenu.add(createMenuItem("Analyze Ency", 'z') { Analyzer.run() })
        val modeMenu = JMenu("Mode")
        modeMenu.add(startReviewingMenuItem)
        modeMenu.add(goToEntryListMenuItem)
        val entryMenu = JMenu("Entry")
        entryMenu.add(createMenuItem("Add Entry", 'n') { EntryEditingWindow() })
        jMenuBar.add(fileMenu)
        jMenuBar.add(encyMenu)
        jMenuBar.add(entryMenu)
        jMenuBar.add(modeMenu)
    }

    private fun importText() {
        val chooser = JFileChooser(nameOfLastUsedEncyDirectory).apply {
            fileFilter = FileNameExtensionFilter("Text files", "txt")
        }
        val result = chooser.showOpenDialog(this)
        if (result == JFileChooser.CANCEL_OPTION) {
            return
        } else {
            val selectedFile = chooser.selectedFile
            File(selectedFile.absolutePath).readLines().forEach { EntryManager.addEntry(doubleTabToEntry(it)) }
        }
    }

    private fun doubleTabToEntry(line: String): Entry {
        val (question, answer) = line.split("\t\t")
        return Entry(question.toStorageString(), answer.toStorageString())
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
        mainMode = MainWindowMode.DISPLAY
        showCorrectPanel()
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

        /*UpdateType.DECK_CHANGED -> {
            Personalisation.updateTimeOfCurrentDeckReview()
            showCorrectPanel()
        }*/
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
