package ui


import Update
import data.Settings
import data.stringToDouble
import data.stringToInt
import data.toRegionalString
import doNothing
import eventhandling.BlackBoard
import study_options.IntervalSettings
import study_options.OtherSettings
import study_options.StudyOptions
import ui.loose_components.LabelledCheckbox
import ui.loose_components.LabelledTextField
import ui.loose_components.TimeInputElement
import ui.main_window.ReviewingState
import java.awt.BorderLayout
import java.awt.GridLayout
import java.awt.event.KeyEvent
import javax.imageio.ImageIO
import javax.swing.*

/**
 * The window in which the user can set how he/she wants to study; like which
 * time to take before the initial repetition, or what scheme repetitions will
 * have (every day, or with increasing intervals, or whatever).
 *
 * @author Eric-Wubbo Lameijer
 */
class StudyOptionsWindow : JFrame() {

    // Button that closes this window, not saving any changes made.
    private val cancelButton = JButton("Discard unsaved changes and close")

    // Button that restores the defaults to those of Eb.
    private val loadEbDefaultsButton = JButton("Load FermiEn's default values")

    // Button that reloads the current settings of the deck (undoing non-saved
    // changes).
    private val loadCurrentDeckSettingsButton = JButton("Load settings of current deck")

    // Button that sets the study settings of the deck to the values currently
    // displayed in this window.
    private val setToTheseValuesButton = JButton("Set study parameters of this deck to these values")

    // Input element that allows users to view and set the interval between the
    // creation of the card and the first time it is put up for review.
    private val initialIntervalBox: TimeInputElement

    private val sizeOfReview: LabelledTextField

    private val timeToWaitAfterCorrectReview: TimeInputElement

    private val lengtheningFactor: LabelledTextField

    private val targetedSuccessPercentage: LabelledTextField

    private val timeToWaitAfterIncorrectReview: TimeInputElement

    private val defaultPriority: LabelledTextField

    private val defaultStartMode : LabelledCheckbox

    private val textFields: List<LabelledTextField>

    init {
        val studyOptions = Settings.studyOptions
        initialIntervalBox = TimeInputElement(
            "Initial review after",
            studyOptions.intervalSettings.initialInterval
        )
        sizeOfReview = LabelledTextField(
            "Number of cards per reviewing session: ",
            studyOptions.otherSettings.reviewSessionSize.toString(), 3, 0
        )
        timeToWaitAfterCorrectReview = TimeInputElement(
            "Time to wait for re-reviewing remembered card:", studyOptions.intervalSettings.rememberedInterval
        )
        lengtheningFactor = LabelledTextField(
            "After each successful review, increase review time by a factor",
            toRegionalString(studyOptions.intervalSettings.lengtheningFactor.toString()), 5, 2
        )
        timeToWaitAfterIncorrectReview = TimeInputElement(
            "Time to wait for re-reviewing forgotten card:", studyOptions.intervalSettings.forgottenInterval
        )
        targetedSuccessPercentage = LabelledTextField(
            "Strive for this percentage successful reviews (between 80% and 90% likely best)",
            toRegionalString(studyOptions.otherSettings.idealSuccessPercentage.toString()), 5, 2
        )
        defaultPriority = LabelledTextField(
            "Default priority for new cards",
            studyOptions.otherSettings.defaultPriority.toString(), 3, 0
        )

        defaultStartMode = LabelledCheckbox("Start deck in review mode",
            studyOptions.otherSettings.startInStudyMode
        )

        textFields = listOf(sizeOfReview, lengtheningFactor, targetedSuccessPercentage)
    }

    // Updates the title of the frame in response to changes to indicate to the user whether there are unsaved changes.
    private fun updateFrame() {
        val guiStudyOptions = gatherUIDataIntoStudyOptionsObject()
        val deckStudyOptions = Settings.studyOptions
        val title = "Study Options" +
                if (guiStudyOptions == deckStudyOptions) " - no unsaved changes"
                else " - UNSAVED CHANGES"
        setTitle(title)
    }

    private fun loadSettings(settings: StudyOptions) {
        deactivateListeners()
        initialIntervalBox.interval = settings.intervalSettings.initialInterval
        sizeOfReview.setContents(settings.otherSettings.reviewSessionSize)
        timeToWaitAfterCorrectReview.interval = settings.intervalSettings.rememberedInterval
        lengtheningFactor.setContents(settings.intervalSettings.lengtheningFactor)
        timeToWaitAfterIncorrectReview.interval = settings.intervalSettings.forgottenInterval
        targetedSuccessPercentage.setContents(settings.otherSettings.idealSuccessPercentage)
        defaultPriority.setContents(settings.otherSettings.defaultPriority)
        defaultStartMode.setContents(settings.otherSettings.startInStudyMode)
        reactivateListeners()
        updateFrame()
    }

    private fun deactivateListeners() = textFields.forEach { it.deactivateListener() }

    private fun reactivateListeners() = textFields.forEach { it.activateListener() }

    private fun loadEbDefaults() = loadSettings(StudyOptions())

    private fun loadCurrentDeckSettings() = loadSettings(Settings.studyOptions)

    //  Collects the data from the GUI, and packages it nicely into a StudyOptions object.
    private fun gatherUIDataIntoStudyOptionsObject() = StudyOptions(
        IntervalSettings(
            initialIntervalBox.interval,
            timeToWaitAfterCorrectReview.interval,
            timeToWaitAfterIncorrectReview.interval,
            stringToDouble(lengtheningFactor.contents())!!
        ),
        OtherSettings(
            stringToInt(sizeOfReview.contents()),
            stringToDouble(targetedSuccessPercentage.contents())!!,
            stringToInt(defaultPriority.contents()),
            defaultStartMode.contents()
        )
    )

    private fun saveSettingsToDeck() {
        Settings.studyOptions = gatherUIDataIntoStudyOptionsObject()
        BlackBoard.post(Update(UpdateType.PROGRAMSTATE_CHANGED, ReviewingState.REACTIVE.name))
        updateFrame() // Should be set to 'no unsaved changes' again.
    }

    // Initializes the study options window, performing those actions which are only permissible (for a
    // nullness checker) after the window has been created.
    internal fun init() {
        layout = BorderLayout()
        createKeyListener(KeyEvent.VK_ESCAPE) { dispose() }
        bindActionsToButtons()
        BlackBoard.register(::respondToUpdate, UpdateType.INPUTFIELD_CHANGED)

        // Then create two panels: one for setting the correct values for the study
        // options, and one to contain the reset/confirm/reload etc. buttons.
        val settingsPane = initSettingsPane()
        add(settingsPane, BorderLayout.NORTH)
        val buttonsPane = initButtonsPane()
        add(buttonsPane, BorderLayout.SOUTH)

        setSize(700, 450)
        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        updateFrame()
        val inputStream = javaClass.classLoader.getResourceAsStream("FermiEn_neg.png")
        iconImage = ImageIcon(ImageIO.read(inputStream)).image
        isVisible = true
    }

    private fun bindActionsToButtons() {
        cancelButton.addActionListener { dispose() }
        loadCurrentDeckSettingsButton.addActionListener { loadCurrentDeckSettings() }
        loadEbDefaultsButton.addActionListener { loadEbDefaults() }
        setToTheseValuesButton.addActionListener { saveSettingsToDeck() }
    }

    private fun initSettingsPane(): JPanel {
        val settingsPane = JPanel().apply {
            layout = BorderLayout()
        }

        // now fill the panes
        val settingsBox = Box.createVerticalBox().apply {
            add(initialIntervalBox)
            add(sizeOfReview)
            add(timeToWaitAfterCorrectReview)
            add(lengtheningFactor)
            add(timeToWaitAfterIncorrectReview)
            add(targetedSuccessPercentage)
            add(defaultPriority)
            add(defaultStartMode)
        }
        settingsPane.add(settingsBox, BorderLayout.NORTH)
        return settingsPane
    }

    private fun initButtonsPane() = JPanel().apply {
        add(cancelButton)
        add(loadEbDefaultsButton)
        add(loadCurrentDeckSettingsButton)
        add(setToTheseValuesButton)

        layout = GridLayout(2, 2)
    }

    private fun respondToUpdate(update: Update) =
        if (update.type == UpdateType.INPUTFIELD_CHANGED) updateFrame()
        else doNothing

    companion object {
        // Displays the study options window. In order to pacify the nullness checker,
        // separates creation and display of the window.
        fun display() = StudyOptionsWindow().init()
    }
}