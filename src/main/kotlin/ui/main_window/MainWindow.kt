package ui.main_window

import Update
import UpdateType
import data.*
import doNothing
import eventhandling.BlackBoard
import fermiEnVersion
import study_options.Analyzer
import study_options.ReviewManager
import ui.*
import java.awt.CardLayout
import java.awt.event.*
import java.io.File
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter
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

class MainWindow(private val reviewManager: ReviewManager) : JFrame() {
    private var reviewState = ReviewingState.REACTIVE

    private var mainMode = if (reviewManager.hasNextCard()) MainWindowMode.REVIEW else MainWindowMode.DISPLAY

    private val listPanel = ListPanel()

    private val modesContainer = JPanel()

    private val informationPanel = InformationPanel(reviewManager)

    private val startReviewingMenuItem = createMenuItem("Start reviewing", 'r') { startReviewing() }

    private val goToEntryListMenuItem = createMenuItem("Go to list of entries", 'l') { goToEntryList() }

    private fun nameOfLastUsedEncyDirectory() = Settings.currentFile()!!.pathPart()

    private val fileMenu = JMenu("File")

    private var messageUpdater: Timer? = null

    private val entryMenu = JMenu("Entry").apply {
        addMenuItem("Add Entry", 'n') {
            listPanel.activateEntryPanel()
        }
    }

    private val encyMenu = JMenu("Encyclopedia Settings").apply {
        addMenuItem("Study Settings", 't') { StudyOptionsWindow.display() }
        addMenuItem("Analyze Ency", 'z') { Analyzer.run() }
    }

    private val modeMenu = JMenu("Mode").apply {
        add(startReviewingMenuItem)
        add(goToEntryListMenuItem)
    }

    private fun goToEntryList() {
        mainMode = MainWindowMode.DISPLAY
        showCorrectPanel()
    }

    init {
        BlackBoard.register(::respondToUpdate, UpdateType.PROGRAMSTATE_CHANGED)
        BlackBoard.register(::respondToUpdate, UpdateType.ENCY_SWAPPED)

        addMenu()
        listPanel.setup()

        // note: container.add needs string, so .name here (or "$"), despite it seeming overkill
        setupPanelContainer(reviewManager)
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

    private fun setupPanelContainer(reviewManager: ReviewManager) {
        modesContainer.apply {
            layout = CardLayout()
            add(listPanel, displayId)
            add(reviewManager.reviewPanel, reviewingId)
            add(SummarizingPanel(reviewManager), summarizingId)
            add(informationPanel, informationalId)
        }
    }

    // Updates the title of the window, which contains information like the number of cards in the deck
    private fun updateWindowTitle() {
        val numReviewingPoints = EntryManager.reviewingPoints()
        val shortCutCode = getShortCutCode(Settings.getShortcutIdOfCurrentDeck())
        var title = "FermiEn ${fermiEnVersion()}: $shortCutCode ${Settings.currentFile()!!.fileNamePart()}"
        val entries = EntryManager.entries().size
        val toReview = EntryManager.reviewableEntries().size
        title += ", ${"entry".pluralize(entries)} in deck, $toReview to review, " +
                "point".pluralize(numReviewingPoints) + "."
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
        if (mainMode == MainWindowMode.DISPLAY) listPanel.resetPanel()
        else reviewManager.initializeReviewSession()
    }

    private fun updateFileMenu() {
        fileMenu.apply {
            removeAll()
            addMenuItem("Create or Load Encyclopedia", 'o', ::createEncyFile)
            addMenuItem("Add Entries (Eb format)", 'e', ::importEbText)
            addMenuItem("Add Entries (FermiEn format)", 'f', ::importEncyText)
            addMenuItem("Quit", 'q', ::saveAndQuit)
            addDeckLoadingMenuItems()
        }
    }

    private fun JMenu.addMenuItem(label: String, actionKey: Char, listener: () -> Unit) {
        add(createMenuItem(label, actionKey, listener))
    }


    private fun addMenu() {
        jMenuBar = JMenuBar().apply {
            updateFileMenu()
            add(fileMenu)
            add(encyMenu)
            add(entryMenu)
            add(modeMenu)
        }
    }


    private fun manageDeckShortcuts() {
        DeckShortcutsPopup().updateShortcuts()
        updateFileMenu()
    }

    private fun addDeckLoadingMenuItems() {
        fileMenu.addSeparator()
        fileMenu.addMenuItem("Manage deck-shortcuts", '0', ::manageDeckShortcuts)
        (1..9).filter { Settings.shortcuts[it] != null }.forEach { digit ->
            val encyFileName = Settings.shortcuts[digit]!!
            fileMenu.addMenuItem(
                "Load deck '${encyFileName.fileNamePart()}'",
                digit.digitToChar()
            ) { EntryManager.loadEntriesFrom(encyFileName) }
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
        accelerator = KeyStroke.getKeyStroke(KeyEvent.getExtendedKeyCodeForChar(actionKey.code), ActionEvent.ALT_MASK)
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
            informationPanel.updateMessageLabel()
            updateWindowTitle()
            showCorrectPanel()
        }
        UpdateType.PROGRAMSTATE_CHANGED -> {
            reviewState = ReviewingState.valueOf(update.contents)
            updateOnScreenInformation()
            showCorrectPanel()
        }
        else -> doNothing
    }

    // Gives the message label its correct (possibly updated) value.
    private fun updateOnScreenInformation() {
        informationPanel.updateMessageLabel()
    }
}

