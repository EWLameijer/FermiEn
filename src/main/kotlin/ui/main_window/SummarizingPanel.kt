package ui.main_window

import Update
import eventhandling.BlackBoard
import study_options.Review
import study_options.ReviewManager
import study_options.ReviewResult
import ui.createKeyPressSensitiveButton
import java.awt.CardLayout
import java.awt.Graphics
import java.awt.event.ComponentListener
import java.beans.EventHandler
import java.io.File
import javax.swing.*
import UpdateType.*
import ui.main_window.SummarizingState.*

enum class SummarizingState { REVIEWS_DONE, YET_REVIEWS_TO_DO }

class SummarizingPanel(private val reviewManager: ReviewManager) : JPanel() {
    private var report = JLabel()
    private var buttonPanel = JPanel()
    private var reviewsCompletedPanel = JPanel()
    private var stillReviewsToDoPanel = JPanel()
    private var summarizingState = REVIEWS_DONE

    private fun backToInformationMode() {
        //updateStudyIntervals()
        BlackBoard.post(Update(PROGRAMSTATE_CHANGED, ReviewingState.INFORMATIONAL.name))
    }

    /*private fun updateStudyIntervals() {
        DeckManager.currentDeck().updateRecommendedStudyIntervalDurations()
        Personalisation.updateTimeOfCurrentDeckReview()
    }*/

    private fun backToReviewingMode() {
        BlackBoard.post(Update(PROGRAMSTATE_CHANGED, ReviewingState.REACTIVE.name))
    }

    init {
        this.addComponentListener(
            EventHandler.create(
                ComponentListener::class.java, this,
                "requestFocusInWindow", null, "componentShown"
            )
        )

        reviewsCompletedPanel.apply {
            addComponentListener(
                EventHandler.create(
                    ComponentListener::class.java, this,
                    "requestFocusInWindow", null, "componentShown"
                )
            )
            add(
                createKeyPressSensitiveButton(
                    "Back to information screen",
                    "pressed ENTER"
                ) { toReactiveMode() })
        }

        stillReviewsToDoPanel.apply {
            addComponentListener(
                EventHandler.create(
                    ComponentListener::class.java, this,
                    "requestFocusInWindow", null, "componentShown"
                )
            )
            add(
                createKeyPressSensitiveButton(
                    "Go to next round of reviews",
                    'g'
                ) { backToReviewingMode() })
            add(
                createKeyPressSensitiveButton(
                    "Back to information screen",
                    'b'
                ) { backToInformationMode() })
        }

        buttonPanel.apply {
            layout = CardLayout()
            add(reviewsCompletedPanel, REVIEWS_DONE.name)
            add(stillReviewsToDoPanel, YET_REVIEWS_TO_DO.name)
        }
        add(report)
        add(buttonPanel)
    }


    private fun toReactiveMode() {
        //updateStudyIntervals()
        BlackBoard.post(Update(PROGRAMSTATE_CHANGED, ReviewingState.REACTIVE.name))
    }

    private fun successStatistics(reviews: List<Review>, text: String) = buildString {
        append("$text<br>")
        val totalNumberOfReviews = reviews.size
        append("total: $totalNumberOfReviews <br>")
        val (correctReviews, incorrectReviews) = reviews.partition { it.result == ReviewResult.SUCCESS }
        val numberOfCorrectReviews = correctReviews.size
        append("correctly answered: $numberOfCorrectReviews<br>")
        append("incorrectly answered: ${incorrectReviews.size}<br>")
        val percentageOfCorrectReviews = 100.0 * numberOfCorrectReviews / totalNumberOfReviews
        val percentageCorrectReviewsAsString = String.format("%.2f", percentageOfCorrectReviews)
        append("percentage of correct reviews: $percentageCorrectReviewsAsString%")
        append("<br><br>")
    }

    public override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        report.text = getReport()
        File("log.txt").writeText(report.text.replace("<br>", "\n").replace("<.*?>".toRegex(), ""))
        val cardLayout = buttonPanel.layout as CardLayout

        summarizingState =
            if (reviewManager.hasNextCard()) YET_REVIEWS_TO_DO else REVIEWS_DONE
        cardLayout.show(buttonPanel, summarizingState.name)
    }

    private fun getReport() = buildString {
        append("<html>")
        append("<b>Summary</b><br><br>")
        val allReviews = reviewManager.reviewResults()
        append(successStatistics(allReviews, "Total reviews"))

        val firstTimeReviews =
            reviewManager.getNewFirstReviews() // reviewedCards.filter { it.getReviews().size == it.getReviewsAfter(DeckManager.deckLoadTime()).size }
        val (previouslySucceededReviews, previouslyFailedReviews) = reviewManager.getNonFirstReviews()
        append(successStatistics(previouslySucceededReviews, "Previously succeeded cards"))
        append(successStatistics(previouslyFailedReviews, "Previously failed cards"))
        append(successStatistics(firstTimeReviews, "New cards"))
        append("</html>")
    }
}