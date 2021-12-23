package ui.main_window

import EMPTY_STRING
import data.Entry
import data.EntryManager
import study_options.ReviewManager
import study_options.ReviewResult
import ui.CardPanel
import ui.EntryEditingWindow
import ui.createKeyPressSensitiveButton
import java.awt.*
import java.awt.event.ComponentListener
import java.beans.EventHandler
import javax.swing.JPanel
import ui.main_window.ReviewState.*

enum class ReviewState { ANSWER_HIDDEN, ANSWER_SHOWN }

class ReviewPanel : JPanel() {

    private val frontOfCardPanel = CardPanel()
    private val backOfCardPanel = CardPanel()
    private val situationalButtonPanel = JPanel()
    private val fixedButtonPanel = JPanel()
    //private val reviewHistoryArea = JTextArea("test")
    private var reviewingState = ANSWER_HIDDEN
    private var entry: Entry? = null

    var manager: ReviewManager? = null

    init {
        EntryManager.registerAsListener { respondToDeckChange() }
        this.isFocusable = true
        addComponentListener(
            EventHandler.create(
                ComponentListener::class.java, this,
                "requestFocusInWindow", null, "componentShown"
            )
        )

        layout = GridBagLayout()

        initFrontOfCardPanel()
        initBackOfCardPanel()
        initSituationalPanel() // Show answer (if only front is shown), or Remembered/Forgotten (if back is shown too, so user can check his learning)
        initFixedButtonPanel()
        initSidePanel()
    }

    private fun initFixedButtonPanel() {
        // the fixed button panel contains buttons that need to be visible always
        val fixedButtonPanelConstraints = GridBagConstraints().apply {
            gridx = 3
            gridy = 4
            gridwidth = 1
            gridheight = 1
            weightx = 1.0
            weighty = 1.0
            fill = GridBagConstraints.BOTH
        }
        fixedButtonPanel.add(createKeyPressSensitiveButton("Edit entry", 'e') { editCard() })
        fixedButtonPanel.add(createKeyPressSensitiveButton("Delete entry", 'd') {
            EntryManager.removeEntry(entry!!)
        })
        //fixedButtonPanel.add(createKeyPressSensitiveButton("View score", 'v', ::showScore))
        add(fixedButtonPanel, fixedButtonPanelConstraints)
    }

    private fun initSituationalPanel() {
        // for buttons that depend on the situation, like when the back of the card
        // is shown or when it is not yet shown.
        val buttonPanelForHiddenBack = JPanel().apply {
            layout = FlowLayout()
            add(createKeyPressSensitiveButton("Show Answer", 's', ::showAnswer))
        }

        val buttonPanelForShownBack = JPanel().apply {
            add(createKeyPressSensitiveButton("Remembered", 'r') { registerAnswer(ReviewResult.SUCCESS) })
            add(createKeyPressSensitiveButton("Forgotten", 'f') { registerAnswer(ReviewResult.FAILURE) })
        }
        val situationalButtonPanelConstraints = GridBagConstraints().apply {
            gridx = 0
            gridy = 4
            gridwidth = 3
            gridheight = 1
            weightx = 3.0
            weighty = 1.0
            fill = GridBagConstraints.BOTH
        }
        situationalButtonPanel.layout = CardLayout()
        situationalButtonPanel.add(buttonPanelForHiddenBack, ANSWER_HIDDEN.name)
        situationalButtonPanel.add(buttonPanelForShownBack, ANSWER_SHOWN.name)
        situationalButtonPanel.background = Color.GREEN
        add(situationalButtonPanel, situationalButtonPanelConstraints)
    }

    private fun initBackOfCardPanel() {
        val backOfCardConstraints = GridBagConstraints().apply {
            gridx = 0
            gridy = 2
            gridwidth = 4
            gridheight = 2
            weightx = 4.0
            weighty = 2.0
            fill = GridBagConstraints.BOTH
        }
        backOfCardPanel.background = Color.YELLOW
        add(backOfCardPanel, backOfCardConstraints)
    }

    private fun initFrontOfCardPanel() {
        val frontOfCardConstraints = GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            gridwidth = 4
            gridheight = 2
            weightx = 4.0
            weighty = 2.0
            fill = GridBagConstraints.BOTH
        }
        frontOfCardPanel.background = Color.PINK
        add(frontOfCardPanel, frontOfCardConstraints)
    }

    private fun initSidePanel() {
        // panel, to be used in future to show successful/unsuccessful cards
        // for now hidden?
        val sidePanelConstraints = GridBagConstraints().apply {
            gridx = 4
            gridy = 0
            gridwidth = 1
            gridheight = 5
            fill = GridBagConstraints.BOTH
            weightx = 1.0
            weighty = 5.0
        }
        val sidePanel = JPanel()

        //sidePanel.add(reviewHistoryArea)
        sidePanel.background = Color.RED
        add(sidePanel, sidePanelConstraints)
    }

    private fun editCard() = EntryEditingWindow(entry)

    private fun registerAnswer(wasRemembered: ReviewResult) {
        manager!!.wasRemembered(wasRemembered)
        repaint()
    }

    private fun showAnswer() {
        reviewingState = ANSWER_SHOWN
        showPanel()
        repaint()
    }

    private fun showPanel() {
        val cardLayout = situationalButtonPanel.layout as CardLayout
        cardLayout.show(situationalButtonPanel, reviewingState.name)
        val backText =
            if (reviewingState == ANSWER_SHOWN) entry!!.answer.toPanelDisplayString() else EMPTY_STRING
        backOfCardPanel.setText(backText)
    }

    public override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
    }

    private fun refresh() = repaint()

    fun display(currentEntry: Entry) {
        reviewingState = ANSWER_HIDDEN
        entry = currentEntry
        val frontText = currentEntry.question.toPanelDisplayString()
        frontOfCardPanel.setText(frontText)
        backOfCardPanel.setText(EMPTY_STRING)
        showPanel()
        refresh()
    }
}