package eb.mainwindow.reviewing

import EMPTY_STRING
import Entry
import Review
import ReviewResult
import UpdateType
import doNothing
import log
import ui.ReviewPanel
import java.time.Duration
import java.util.ArrayList

import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.time.Instant
import javax.swing.Timer
import kotlin.math.min

/**
 * Manages the review session much like Deck manages the LogicalDeck: there can only be one review at a time
 *
 * @author Eric-Wubbo Lameijer
 */
class ReviewManager(var reviewPanel: ReviewPanel) {
    /*init {
        BlackBoard.register(this, UpdateType.DECK_SWAPPED)
        BlackBoard.register(this, UpdateType.CARD_CHANGED)
        BlackBoard.register(this, UpdateType.DECK_CHANGED)
    }*/

    private var entriesToBeReviewed = mutableListOf<Entry>()
    //private var cardsReviewed = mutableSetOf<Card>()

    // private val evalStatusListener: ActionListener = ActionListener { e: ActionEvent -> evaluateStatus() }

    // counter stores the index of the card in the cardsToBeReviewed list that should be reviewed next.
    private var counter: Int = 0

    // Should the answer (back of the card) be shown to the user? 'No'/false when the user is trying to recall the answer,
    // 'Yes'/true when the user needs to check the answer.
    private var showAnswer: Boolean = false

    /* fun reviewResults(): List<Review> {
        ensureReviewSessionIsValid()
        return cardsReviewed.flatMap { it.getReviewsAfter(DeckManager.deckLoadTime()) }
    }*/


    /*fun reviewedCards(): List<Card> {
        ensureReviewSessionIsValid()
        return cardsReviewed.toList()
    }*/

    /* fun getNewFirstReviews(): List<Review> {
        ensureReviewSessionIsValid()
        return cardsReviewed.map { it.getReviews().first() }.filter { it.instant > DeckManager.deckLoadTime() }
    }

    fun getNonFirstReviews(): Pair<List<Review>, List<Review>> {
        ensureReviewSessionIsValid()
        val previouslySucceeded = mutableListOf<Review>()
        val previouslyFailed = mutableListOf<Review>()
        cardsReviewed.forEach { card ->
            val reversedReviews = card.getReviews().reversed()
            for (index in 0 until reversedReviews.lastIndex) { // for each review EXCEPT the 'first review'
                val review = reversedReviews[index]
                if (review.instant > DeckManager.deckLoadTime()) {
                    if (reversedReviews[index + 1].wasSuccess) previouslySucceeded += review else previouslyFailed += review
                } else break
            }
        }
        return previouslySucceeded to previouslyFailed
    } */


    fun currentEntry(): Entry? =
        if (entriesToBeReviewed.isEmpty() || counter >= entriesToBeReviewed.size) null
        else entriesToBeReviewed[counter]

    fun currentFront(): String {
        ensureReviewSessionIsValid()
        return currentEntry()?.question?.toPanelDisplayString() ?: ""
    }

    private fun currentBack() = currentEntry()?.answer.toPanelDisplayString() ?: ""


    private fun ensureReviewSessionIsValid() {
        initializeReviewSession()
    }

    fun wasRemembered(reviewResult: ReviewResult) {
        ensureReviewSessionIsValid()
        currentEntry()!!.addReview(Review(Instant.now(), reviewResult))
        moveToNextReviewOrEnd()
    }

    fun respondToUpdate(update: UpdateType) = when (update) {
        UpdateType.ENTRY_CHANGED -> updatePanels()
        UpdateType.ENCY_CHANGED -> updateCollection() // It can be that the current card (or another) has been deleted
        UpdateType.ENCY_SWAPPED -> initializeReviewSession()
        else -> doNothing
    }

    fun showAnswer() {
        ensureReviewSessionIsValid()
        showAnswer = true
        updatePanels()
    }

    private fun updatePanels() {
        if (activeEntryExists()) {
            val currentBack = if (showAnswer) currentBack() else EMPTY_STRING
            reviewPanel!!.updatePanels(currentFront(), currentBack, showAnswer)
        }
    }

    private fun initializeReviewSession() {
        //cardsReviewed = mutableSetOf() // don't carry old reviews with you.
        continueReviewSession()
    }

    private fun continueReviewSession() {
        val maxNumReviews = 20 // currentDeck.studyOptions.otherSettings?.reviewSessionSize
        val reviewableEntries = EntryManager.reviewableEntries()
        val numberOfReviewableEntries = reviewableEntries.size
        log("Number of reviewable cards is $numberOfReviewableEntries")
        val numCardsToBeReviewed =
            if (maxNumReviews == null) totalNumberOfReviewableCards
            else min(maxNumReviews, totalNumberOfReviewableCards)
        // now, for best effect, those cards which have expired more recently should
        // be rehearsed first, as other cards probably need to be relearned anyway,
        // and we should try to contain the damage.
        val (newlyReviewedCards, repeatReviewedCards) = reviewableCards.partition { it.getReviews().isEmpty() }
        val sortedReviewedCards = repeatReviewedCards.sortedByDescending { currentDeck.getRipenessFactor(it) }
        val sortedNewCards = newlyReviewedCards.sortedByDescending { currentDeck.getRipenessFactor(it) }
        val prioritizedReviewList = sortedReviewedCards + sortedNewCards

        // get the first n for the review
        cardsToBeReviewed = ArrayList(prioritizedReviewList.subList(0, numCardsToBeReviewed))
        cardsToBeReviewed.shuffle()

        counter = 0
        stopTimer.reset()
        startCardReview()
    }

    private fun startCardReview() {
        showAnswer = false
        startTimer.press()
        stopTimer.reset()
        if (activeCardExists() && timerSettings().limitReviewTime && frontTimer == null) {
            frontTimer = Timer(100) { evaluateStatus() }
            frontTimer!!.start()
        } else if (frontTimer != null && !timerSettings().limitReviewTime) {
            frontTimer!!.stop()
            frontTimer = null
            reviewPanel!!.resetButtonTexts()
        }
        updatePanels()
    }

    private fun evaluateStatus() {
        if (!startTimer.isEmpty() && stopTimer.isEmpty()) {
            updateStatusInFrontCardMode()
        } else if (!stopTimer.isEmpty() && !startTimer.isEmpty()) {
            updateStatusInWholeCardMode()
        }
    }

    private fun updateStatusInWholeCardMode() {
        val wholeTimeLimit = timerSettings()!!.wholeStudyTimeLimit.asDuration()
        val backInspectionTimePassed = Duration.between(stopTimer.instant(), Instant.now())
        if (backInspectionTimePassed > wholeTimeLimit) {
            wasRemembered(false)
        } else {
            val timeRemaining = (wholeTimeLimit - backInspectionTimePassed).seconds
            reviewPanel!!.updateForgottenButton(timeRemaining)
        }
    }

    private fun updateStatusInFrontCardMode() {
        val frontTimeLimit = timerSettings()!!.frontStudyTimeLimit.asDuration()
        val timePassed = Duration.between(startTimer.instant(), Instant.now())
        if (timePassed > frontTimeLimit) {
            showAnswer()
        } else {
            val timeRemaining = (frontTimeLimit - timePassed).seconds
            reviewPanel!!.updateShowButton(timeRemaining)
        }
    }

    private fun activeEntryExists() = counter < entriesToBeReviewed.size

    private fun moveToNextReviewOrEnd() {
        if (hasNextCard()) {
            counter++
            startCardReview()
        } else {
            BlackBoard.post(Update(UpdateType.PROGRAMSTATE_CHANGED, MainWindowState.SUMMARIZING.name))
        }
    }

    // is there a next card to study?
    private fun hasNextCard() = counter < cardsToBeReviewed.lastIndex

    // The number of cards that still need to be reviewed in this session
    fun cardsToGoYet(): Int {
        ensureReviewSessionIsValid()
        return cardsToBeReviewed.size - counter
    }

    // If cards are added to (or, more importantly, removed from) the deck, ensure
    // that the card also disappears from the list of cards to be reviewed
    private fun updateCollection() {
        if (cardsToBeReviewed.isEmpty()) {
            updatePanels()
            return
        }
        val deletingCurrentCard = !deckContainsCardWithThisFront(cardsToBeReviewed[counter].front)
        val deletedIndices =
            cardsToBeReviewed.withIndex().filter { !deckContainsCardWithThisFront(cardsToBeReviewed[it.index].front) }
                .map { it.index }
        cardsToBeReviewed = cardsToBeReviewed.filter { deckContainsCardWithThisFront(it.front) }.toMutableList()
        deletedIndices.forEach { if (it <= counter) counter-- }

        if (deletingCurrentCard) {
            moveToNextReviewOrEnd()
        } else {
            updatePanels()
        }
    }

    // Returns whether this deck contains a card with this front. This sounds a lot like asking whether the deck
    // contains a card, but since by definition each card in a deck has a unique front,
    // just checking fronts simplifies things.
    private fun deckContainsCardWithThisFront(front: Hint) =
        DeckManager.currentDeck().cardCollection.getCardWithFront(front) != null

    // Allows the GUI to initialize the panel that displays the reviews
    fun setPanel(inputReviewPanel: ReviewPanel) {
        reviewPanel = inputReviewPanel
    }
}
