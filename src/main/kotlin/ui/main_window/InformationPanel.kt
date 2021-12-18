package ui.main_window

import Update
import createKeyPressSensitiveButton
import data.EntryManager
import data.durationToString
import eventhandling.BlackBoard
import study_options.ReviewManager
import ui.MainWindowState
import java.awt.BorderLayout
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.JLabel
import javax.swing.JPanel
import data.pluralize
import javax.swing.Timer

class InformationPanel(private val reviewManager: ReviewManager) : JPanel() {
    private val messageLabel = JLabel()

    private var messageUpdater: Timer? = null

    // button the user can press to start reviewing. Only visible if the user for some reason decides to not review
    // cards yet (usually by having one rounds of review, and then stopping the reviewing)
    private val startReviewingButton =
        createKeyPressSensitiveButton("Review now", 'r', ::startReviewing).apply {
            isVisible = false
        }

    private fun startReviewing() {
        //reviewManager.resetTimers()
        BlackBoard.post(Update(UpdateType.PROGRAMSTATE_CHANGED, MainWindowState.REACTIVE.name))
    }

    init {
        layout = BorderLayout()
        add(messageLabel, BorderLayout.CENTER)
        add(startReviewingButton, BorderLayout.SOUTH)
        messageUpdater = Timer(100) { updateMessageLabel() }
        messageUpdater!!.start()
    }


    private fun deckSizeMessage(): String {
        val numEntries = EntryManager.entries().size
        val plural = pluralize("entry", numEntries)
        return "The current encyclopedia contains $numEntries $plural."
    }

    /*private fun totalReviewTimeMessage(): String {
        val currentDeck = DeckManager.currentDeck()
        val totalStudyTime = durationToString(currentDeck.totalStudyTime())
        val totalMemoryTime = durationToString(currentDeck.totalMemoryTime())
        return "Reviewing has taken a total time of $totalStudyTime, the memorized worth is $totalMemoryTime"
    }*/

    //Returns text indicating how long it will be to the next review
    private fun timeToNextReviewMessage() = buildString {
        val numCards = EntryManager.entries().size
        if (numCards > 0) {
            append("Time till next review: ")
            val timeUntilNextReviewAsDuration = EntryManager.timeUntilNextReview()!!
            val timeUntilNextReviewAsText = durationToString(timeUntilNextReviewAsDuration)
            append(timeUntilNextReviewAsText)
            val nextReviewInstant = LocalDateTime.now() + timeUntilNextReviewAsDuration
            val formattedNextReviewInstant = formatReviewDate(nextReviewInstant)
            append(formattedNextReviewInstant)
            append("<br>")
            startReviewingButton.isVisible = timeUntilNextReviewAsDuration.isNegative
        } else {
            startReviewingButton.isVisible = false
        }
    }

    private fun formatReviewDate(nextReviewInstant: LocalDateTime): String {
        val nowDayOfYear = LocalDateTime.now().dayOfYear
        val nextReviewDayOfYear = nextReviewInstant.dayOfYear
        val isClose = nextReviewDayOfYear <= nowDayOfYear + 1
        val closeString = if (nextReviewDayOfYear == nowDayOfYear) "today" else "tomorrow"
        val generalFormatter = DateTimeFormatter.ofPattern(" (yyyy-MM-dd HH:mm)")
        val todayFormatter = DateTimeFormatter.ofPattern(" HH:mm)")
        return if (isClose) " ($closeString " + nextReviewInstant.format(todayFormatter)
        else nextReviewInstant.format(generalFormatter)
    }

    // Updates the message label (the information inside the main window, like time to next review)
    fun updateMessageLabel() {
        messageLabel.text = buildString {
            append("<html>")
            append(deckSizeMessage() + "<br>")
            //append(totalReviewTimeMessage() + "<br>")
            append(timeToNextReviewMessage())
            append("$uiCommands<br>")
            //append(Personalisation.deckShortcuts() + "<br><br>")
            //append(Personalisation.toStudy())
            append("</html>")
        }
    }

    //Returns the commands of the user interface as a string, which can be used to instruct the user on Eb's use.
    private val uiCommands = """<br>
            Ctrl+N to add an entry.<br>
            Ctrl+Q to quit.<br>
            Ctrl+L to show the list of entries.<br>
            Ctrl+O to create a new encyclopedia.<br>
            Ctrl+T to view/edit the study options.<br>""".trimIndent()
}