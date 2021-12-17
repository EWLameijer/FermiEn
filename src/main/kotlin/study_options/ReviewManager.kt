package study_options

import EMPTY_STRING
import Update
import data.Entry
import UpdateType
import data.EntryManager
import data.toHorizontalString
import doNothing
import eventhandling.BlackBoard
import log
import ui.MainWindowState
import ui.ReviewPanel
import java.util.ArrayList

import java.time.Instant
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
    init {
        reviewPanel.manager = this
    }

    private var entriesToBeReviewed = mutableListOf<Entry>()
    //private var cardsReviewed = mutableSetOf<Card>()

    // private val evalStatusListener: ActionListener = ActionListener { e: ActionEvent -> evaluateStatus() }

    // counter stores the index of the card in the cardsToBeReviewed list that should be reviewed next.
    private var counter: Int = 0

    // Should the answer (back of the card) be shown to the user? 'No'/false when the user is trying to recall the answer,
    // 'Yes'/true when the user needs to check the answer.
    private var initialized = false

    fun reviewResults(): List<Review> {
        ensureReviewSessionIsValid()
        return EntryManager.entries().flatMap { it.getReviewsAfter(EntryManager.encyLoadInstant()!!) }
    }

    fun reviewedEntries(): List<Entry> {
        ensureReviewSessionIsValid()
        return EntryManager.entries().filter { it.reviews().last().instant > EntryManager.encyLoadInstant()!! }
    }


    /*fun reviewedCards(): List<Card> {
        ensureReviewSessionIsValid()
        return cardsReviewed.toList()
    }*/

    fun getNewFirstReviews(): List<Review> {
        ensureReviewSessionIsValid()
        return EntryManager.entries().map { it.reviews().first() }
            .filter { it.instant > EntryManager.encyLoadInstant() }
    }

    fun getNonFirstReviews(): Pair<List<Review>, List<Review>> {
        ensureReviewSessionIsValid()
        val previouslySucceeded = mutableListOf<Review>()
        val previouslyFailed = mutableListOf<Review>()
        reviewedEntries().forEach { entry ->
            val reversedReviews = entry.reviews().reversed()
            for (index in 0 until reversedReviews.lastIndex) { // for each review EXCEPT the 'first review'
                val review = reversedReviews[index]
                if (review.instant > EntryManager.encyLoadInstant()) {
                    if (reversedReviews[index + 1].result == ReviewResult.SUCCESS) previouslySucceeded += review else previouslyFailed += review
                } else break
            }
        }
        return previouslySucceeded to previouslyFailed
    }

    private fun currentEntry(): Entry? =
        if (entriesToBeReviewed.isEmpty() || counter >= entriesToBeReviewed.size) null
        else entriesToBeReviewed[counter]

    private fun currentFront() = currentEntry()?.question?.toPanelDisplayString() ?: ""

    private fun currentBack() = currentEntry()?.answer?.toPanelDisplayString() ?: ""


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

    private fun updatePanels() {
        if (activeEntryExists()) {
            reviewPanel!!.display(currentEntry()!!)
        }
    }

    fun initializeReviewSession() {
        //cardsReviewed = mutableSetOf() // don't carry old reviews with you.
        if (!initialized) continueReviewSession()
    }

    private fun continueReviewSession() {
        initialized = true
        val maxNumReviews = Settings.studyOptions.otherSettings.reviewSessionSize
        val reviewableEntries = EntryManager.reviewableEntries()
        val numberOfReviewableEntries = reviewableEntries.size
        log("Number of reviewable cards is $numberOfReviewableEntries")
        val numCardsToBeReviewed =
            if (maxNumReviews == null) numberOfReviewableEntries
            else min(maxNumReviews, numberOfReviewableEntries)
        // now, for best effect, those cards which have expired more recently should
        // be rehearsed first, as other cards probably need to be relearned anyway,
        // and we should try to contain the damage.
        val (newlyReviewedEntries, repeatReviewedEntries) = reviewableEntries.partition { it.numReviews() == 0 }
        val sortedReviewedEntries = repeatReviewedEntries.sortedByDescending { it.getRipenessFactor() }
        val sortedNewCards = newlyReviewedEntries.sortedByDescending { it.getRipenessFactor() }
        val prioritizedReviewList = sortedReviewedEntries + sortedNewCards

        // get the first n for the review
        entriesToBeReviewed = ArrayList(prioritizedReviewList.subList(0, numCardsToBeReviewed))
        entriesToBeReviewed.shuffle()

        counter = 0
        startCardReview()
    }

    private fun startCardReview() {
        updatePanels()
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
    fun hasNextCard() = counter < entriesToBeReviewed.lastIndex

    // The number of cards that still need to be reviewed in this session
    fun cardsToGoYet(): Int {
        ensureReviewSessionIsValid()
        return entriesToBeReviewed.size - counter
    }

    // If cards are added to (or, more importantly, removed from) the deck, ensure
    // that the card also disappears from the list of cards to be reviewed
    private fun updateCollection() {
        if (entriesToBeReviewed.isEmpty()) {
            updatePanels()
            return
        }
        val deletingCurrentCard =
            !EntryManager.containsEntryWithQuestion(entriesToBeReviewed[counter].question.toHorizontalString())
        val deletedIndices =
            entriesToBeReviewed.withIndex()
                .filter { !EntryManager.containsEntryWithQuestion(entriesToBeReviewed[it.index].question.toHorizontalString()) }
                .map { it.index }
        entriesToBeReviewed =
            entriesToBeReviewed.filter { EntryManager.containsEntryWithQuestion(it.question.toHorizontalString()) }
                .toMutableList()
        deletedIndices.forEach { if (it <= counter) counter-- }

        if (deletingCurrentCard) {
            moveToNextReviewOrEnd()
        } else {
            updatePanels()
        }
    }

    // Allows the GUI to initialize the panel that displays the reviews
    fun setPanel(inputReviewPanel: ReviewPanel) {
        reviewPanel = inputReviewPanel
    }
}