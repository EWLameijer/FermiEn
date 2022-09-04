package study_options

import Update
import data.Entry
import UpdateType
import data.EntryManager
import data.Settings
import eventhandling.BlackBoard
import ui.main_window.ReviewPanel
import ui.main_window.ReviewingState
import java.util.ArrayList

import java.time.Instant
import kotlin.math.min

/**
 * Manages the review session much like Deck manages the LogicalDeck: there can only be one review at a time
 *
 * @author Eric-Wubbo Lameijer
 */
class ReviewManager(var reviewPanel: ReviewPanel) {
    init {
        BlackBoard.register({ respondToEncyUpdate() }, UpdateType.ENCY_CHANGED)
        BlackBoard.register({ respondToEncySwap() }, UpdateType.ENCY_SWAPPED)
        reviewPanel.manager = this
    }

    private var entriesToBeReviewed = mutableListOf<Entry>()

    fun reviewsLeftInThisSession() = entriesToBeReviewed.size - counter

    // counter stores the index of the card in the cardsToBeReviewed list that should be reviewed next.
    private var counter: Int = 0

    // Should the answer (back of the card) be shown to the user? 'No'/false when the user is trying to recall the answer,
    // 'Yes'/true when the user needs to check the answer.
    private var initialized = false

    fun reviewResults(): List<Review> {
        return EntryManager.entries().flatMap { it.getReviewsAfter(EntryManager.encyLoadInstant()!!) }
    }

    private fun entriesReviewedInThisSession(): List<Entry> {
        return EntryManager.entries()
            .filter {
                val reviewsSoFar = it.reviews()
                reviewsSoFar.isNotEmpty() && reviewsSoFar.last().instant > EntryManager.encyLoadInstant()!!
            }
    }

    fun getNewFirstReviews(): List<Review> {
        return EntryManager.entries().mapNotNull { it.reviews().firstOrNull() }
            .filter { it.instant > EntryManager.encyLoadInstant() }
    }

    fun getNonFirstReviews(): Pair<List<Review>, List<Review>> {
        val previouslySucceeded = mutableListOf<Review>()
        val previouslyFailed = mutableListOf<Review>()
        entriesReviewedInThisSession().forEach { entry ->
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

    fun wasRemembered(reviewResult: ReviewResult) {
        currentEntry()!!.addReview(Review(Instant.now(), reviewResult))
        moveToNextReviewOrEnd()
    }

    private fun respondToEncySwap() = continueReviewSession()

    private fun respondToEncyUpdate() {
        val entriesReviewedSoFar = counter
        val newState = if (entriesReviewedSoFar == 0) ReviewingState.REACTIVE
        else ReviewingState.SUMMARIZING
        BlackBoard.post(Update(UpdateType.PROGRAMSTATE_CHANGED, newState.name))
    }

    fun initializeReviewSession() {
        continueReviewSession()
    }

    private fun List<Entry>.sortOnPriorityAndRipeness(): List<Entry> {
        val now = Instant.now()
        return sortedWith(compareByDescending<Entry> { it.importance }.thenByDescending { it.getRipenessFactor(now) })
    }

    private fun continueReviewSession() {
        initialized = true
        val maxNumReviews = Settings.studyOptions.otherSettings.reviewSessionSize
        val reviewableEntries = EntryManager.reviewableEntries()
        val numberOfReviewableEntries = reviewableEntries.size
        val numCardsToBeReviewed =
            if (maxNumReviews == null) numberOfReviewableEntries
            else min(maxNumReviews, numberOfReviewableEntries)
        if (numCardsToBeReviewed == 0) return
        // now, for best effect, those cards which have expired more recently should
        // be rehearsed first, as other cards probably need to be relearned anyway,
        // and we should try to contain the damage.
        val (newlyReviewedEntries, repeatReviewedEntries) = reviewableEntries.partition { it.numReviews() == 0 }
        val sortedReviewedEntries = repeatReviewedEntries.sortOnPriorityAndRipeness()
        val sortedNewCards = newlyReviewedEntries.sortOnPriorityAndRipeness()
        val prioritizedReviewList = sortedReviewedEntries + sortedNewCards

        // get the first n for the review
        entriesToBeReviewed = ArrayList(prioritizedReviewList.subList(0, numCardsToBeReviewed))
        entriesToBeReviewed.shuffle()

        counter = 0
        startCardReview()
    }

    private fun startCardReview() {
        reviewPanel.display(currentEntry()!!)
    }

    private fun moveToNextReviewOrEnd() {
        counter++
        if (hasNextCard()) {
            startCardReview()
        } else {
            initialized = false
            BlackBoard.post(Update(UpdateType.PROGRAMSTATE_CHANGED, ReviewingState.SUMMARIZING.name))
        }
    }

    // is there a next card to study?
    fun hasNextCard() = counter <= entriesToBeReviewed.lastIndex
}
