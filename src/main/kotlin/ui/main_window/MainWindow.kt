package ui.main_window

import Update
import UpdateType
import data.*
import data.utils.fileNamePart
import data.utils.pathPart
import data.utils.pluralize
import doNothing
import eventhandling.BlackBoard
import data.exportAsPrintable
import study_options.Analyzer
import study_options.ReviewManager
import ui.*
import java.awt.CardLayout
import java.awt.event.*
import java.io.File
import javax.imageio.ImageIO
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

    private var mainMode = correctStartingMode()

    private fun correctStartingMode() =
        if (Settings.studyOptions.otherSettings.startInStudyMode && reviewManager.hasNextCard())
            MainWindowMode.REVIEW else MainWindowMode.DISPLAY

    private val listPanel = ListPanel()

    private var reviewPanel = ReviewPanel()

    private val modesContainer = JPanel()

    private val informationPanel = InformationPanel(reviewManager)

    private val startReviewingMenuItem = createMenuItem("Start reviewing", 'r') { startReviewing() }

    private val goToEntryListMenuItem = createMenuItem("Go to list of entries", 'l') { goToEntryList() }

    private fun nameOfLastUsedEncyDirectory() = Settings.currentFile()!!.pathPart()

    private val fileMenu = JMenu("File")

    private var messageUpdater: Timer? = null

    private val entryMenu = JMenu("Entry").apply {
        addMenuItem("Add Entry", 'n') {
            if (mainMode == MainWindowMode.DISPLAY) listPanel.activateEntryPanel()
            else if (reviewState == ReviewingState.REVIEWING || reviewState == ReviewingState.REACTIVE) reviewPanel.newCard()
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
        val inputStream = javaClass.classLoader.getResourceAsStream("FermiEn.png")
        iconImage = ImageIcon(ImageIO.read(inputStream)).image

        isVisible = true
        listPanel.makeWidthCorrect()
    }

    private fun setupPanelContainer(reviewManager: ReviewManager) {
        reviewPanel = reviewManager.reviewPanel
        modesContainer.apply {
            layout = CardLayout()
            add(listPanel, displayId)
            add(reviewManager.reviewPanel, reviewingId)
            add(SummarizingPanel(reviewManager), summarizingId)
            add(informationPanel, informationalId)
        }
    }

    private fun inReviewingMode() = mainMode == MainWindowMode.REVIEW

    // Updates the title of the window, which contains information like the number of cards in the deck
    private fun updateWindowTitle() {
        val numReviewingPoints = EntryManager.reviewingPoints()
        val shortCutCode = getShortCutCode(Settings.getShortcutIdOfCurrentDeck())
        var title = "FermiEn ${Loader.version}: $shortCutCode ${Settings.currentFile()!!.fileNamePart()}"
        val entries = EntryManager.entries().size
        val toReview = EntryManager.reviewableEntries().size
        val sessionText = if (inReviewingMode() && reviewManager.reviewsLeftInThisSession() > 0) {
            "entry".pluralize(reviewManager.reviewsLeftInThisSession()) + " in this session, "
        } else ""
        title += ", ${"entry".pluralize(entries)} in deck, ${"entry".pluralize(toReview)} to review, " + sessionText +
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
            addMenuItem("Export Encyclopedia to printable txt", 'e', ::exportEncy)
            addMenuItem("Export Encyclopedia to randomly ordered printable txt", 'd', ::exportRandomEncy)
            addMenuItem("Add Entries from other ency", 'f', ::importEncyText)
            addMenuItem("Quit", 'q', ::saveAndQuit)
            addDeckLoadingMenuItems()
        }
    }

    private fun JMenu.addMenuItem(label: String, actionKey: Char, listener: () -> Unit) {
        add(createMenuItem(label, actionKey, listener))
    }

    private fun exportEncy() = exportEncy(List<Entry>::entrySorter)

    private fun exportRandomEncy() = exportEncy(List<Entry>::shuffled)

    private fun exportEncy(sorter: List<Entry>.() -> List<Entry>) {
        val printableFilename = Settings.currentFile()!!.removeSuffix(".txt") + "_printable.txt"
        val shuffledEntries = EntryManager.entries().sorter()
        exportAsPrintable(shuffledEntries, printableFilename)
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

    private fun importEncyText() = importText(::encyConverter)
    
    private fun importText(fileConverter: (String, String) -> Unit) {
        val chooser = JFileChooser(nameOfLastUsedEncyDirectory()).apply {
            fileFilter = FileNameExtensionFilter("Text files", "txt")
        }
        val result = chooser.showOpenDialog(this)
        if (result == JFileChooser.CANCEL_OPTION) {
            return
        } else {
            val selectedFile = chooser.selectedFile
            var initialTag = selectedFile.canonicalPath.dropLastWhile { it != '.' }.dropLast(1)
                .takeLastWhile { it != File.separatorChar }
            val desiredTag = JOptionPane.showInputDialog(
                null,
                "Optional: add tag to merged entries",
                initialTag
            )
            fileConverter(selectedFile.absolutePath, desiredTag)
        }
    }

    private fun encyConverter(filename: String, tag: String) {
        File(filename).readLines().forEach { EntryManager.addEntry(it.toEntryWithTag(tag)) }
    }

    private fun makeTxtFileName(fileName: String): String =
        if (fileName.endsWith(".txt")) fileName
        else fileName.replace('.', '_') + ".txt"

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

            if (selectedFile.exists()) EntryManager.loadEntriesFrom(selectedFilepath)
            else {
                // new file: check that it is a nice textfile
                val selectedFileName = selectedFile.name
                val txtFileName = makeTxtFileName(selectedFileName)
                val txtFilePath: String = selectedFilepath.removeSuffix(selectedFileName) + txtFileName
                File(txtFilePath).writeText("")
                EntryManager.loadEntriesFrom(txtFilePath)
            }
            listPanel.makeWidthCorrect()
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
        if (mainMode == MainWindowMode.DISPLAY && listPanel.isEditing()) {
            listPanel.saveState()
        }
        EntryManager.saveEntriesToFile()
        dispose()
        exitProcess(0)
    }

    private fun respondToUpdate(update: Update) = when (update.type) {
        UpdateType.ENCY_SWAPPED -> {
            informationPanel.updateMessageLabel()
            updateWindowTitle()
            mainMode = correctStartingMode()
            reviewPanel.clearEntry()
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



