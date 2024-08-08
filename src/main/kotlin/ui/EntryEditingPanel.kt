package ui

import data.*
import data.utils.StorageString
import data.utils.linesOfMaxLength
import data.utils.toHorizontalString
import data.utils.toStorageString
import maxPriority
import ui.main_window.ListPanel
import java.awt.*
import javax.swing.*

class EntryEditingPanel(private val parentWindow: ListPanel, private var entry: Entry? = null) : JPanel() {
    private val priorityLabel = JLabel(priorityText())

    private val deleteButton = UnfocusableButton("Delete") {
        EntryManager.removeEntry(entry!!)
        clear()
    }

    private val changePriorityButton = UnfocusableButton("Change priority") { changePriority() }

    private fun priorityText() = if (entry == null) "" else "Priority ${entry!!.importance}"

    private fun changePriority() {
        val newPriorityAsString =
            JOptionPane.showInputDialog(this, "Enter new priority (1-$maxPriority)", entry!!.importance)
        val newPriority = newPriorityAsString.toIntOrNull()
        if (newPriority == null || newPriority < 1 || newPriority > maxPriority) JOptionPane.showMessageDialog(
            this,
            "'$newPriorityAsString' is an invalid value - enter a value between 1 and $maxPriority"
        ) else {
            entry!!.importance = newPriority
            priorityLabel.text = "Priority $newPriority"
        }
    }

    private val cardFrontPane = JTextPane().apply {
        makeTabTransferFocus(this)
        document.addDocumentListener(parentWindow.searchFieldListener)
        //document.addDocumentListener(DelegatingDocumentListener { println("Listening") })
    }

    fun frontText(): String = cardFrontPane.text

    private fun makeTabTransferFocus(component: Component) {
        var strokes: HashSet<KeyStroke> = HashSet(listOf(KeyStroke.getKeyStroke("pressed TAB")))
        component.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, strokes)
        strokes = HashSet(listOf(KeyStroke.getKeyStroke("shift pressed TAB")))
        component.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, strokes)
    }

    private val cardBackPane = JTextPane().apply {
        makeTabTransferFocus(this)
    }

    private val okButton = JButton("Ok").apply {
        addActionListener { saveNewEntry() }
    }

    // NOTE: init has to be below the cardFrontPane and cardBackPane definitions, else it doesn't work
    // (tested 2021-12-12)
    init {
        val enterKeyStroke = KeyStroke.getKeyStroke("pressed ENTER")
        val action = "Enter"

        okButton.getInputMap(WHEN_FOCUSED).put(enterKeyStroke, action)
        okButton.actionMap.put(action, ProgrammableAction(::saveNewEntry))
        if (entry != null) {
            cardFrontPane.text = entry!!.question.toPanelDisplayString()
            cardBackPane.text = entry!!.answer.toPanelDisplayString()
        }

        addCardPanel()
        addButtonPanel()
        setSize(650, 400)
        isVisible = true
    }

    private fun clear() {
        entry = null
        cardFrontPane.apply {
            text = ""
            requestFocusInWindow()
        }
        cardBackPane.text = ""
    }

    fun question() = cardFrontPane.text.toStorageString()

    private fun answer() = cardBackPane.text.toStorageString()

    private fun originalQuestion(): StorageString? = entry?.question

    fun saveNewEntry() {
        if (question().toHorizontalString().isBlank()) {
            JOptionPane.showMessageDialog(this, "Cannot add a card with a blank front")
            return
        }
        if (entry != null) { // are you trying to replace the card/front?
            val originalAnswer = entry!!.answer
            val buttons = getFrontChangeButtons()
            JOptionPane.showOptionDialog(
                null,
                stringCompareResult(originalAnswer),
                "Are you sure you want to update the current card?", 0,
                JOptionPane.QUESTION_MESSAGE, null, buttons, null
            )

        } else {
            submitEntry()
        }
    }

    private fun stringCompareResult(originalAnswer: StorageString): String {
        val (origQ, origA, newQ, newA) = listOf(
            originalQuestion(),
            originalAnswer,
            question(),
            answer()
        ).map { it!!.toHorizontalString() }
        val qComparison =
            if (newQ != origQ) "Change the question<br>'${summarize(origQ)}' to<br>'${difference(origQ, newQ)}'" else ""
        var aComparison =
            if (newA != origA) "Change the answer<br>'${summarize(origA)}' to<br>'${difference(origA, newA)}'" else ""
        val separator = if (qComparison.isNotEmpty() && aComparison.isNotEmpty()) "<br>and<br>" else ""
        if (separator.isNotEmpty()) aComparison = aComparison.replaceFirst('C', 'c')
        return "<html>$qComparison$separator$aComparison</html>"
    }

    private fun summarize(text: String): String {
        val words = text.split(' ')
        return if (words.size <= 6) text
        else (words.take(3) + "..." + words.takeLast(3)).joinToString(" ")
    }

    private fun difference(originalText: String, newText: String): String {
        val commonPrefix = originalText.commonPrefixWith(newText)
        val commonSuffix = originalText.commonSuffixWith(newText)
        val result = newText.removePrefix(commonPrefix).removeSuffix(commonSuffix)
        return ("${summarize(commonPrefix)}<i>$result</i>${summarize(commonSuffix)}").linesOfMaxLength(50)
    }

    private fun submitEntry() {
        val newEntry = Entry(question(), answer(), entry?.importance)
        EntryManager.addEntry(newEntry)
    }

    private fun getFrontChangeButtons(): Array<JButton> {
        val replaceButton = createKeyPressSensitiveButton("Replace card", 'r') {
            EntryManager.removeEntry(entry!!)
            submitEntry()
            closeOptionPane()
        }
        val keepBothButton = createKeyPressSensitiveButton("Keep both cards", 'k') {
            submitEntry()
            closeOptionPane()
        }
        val cancelCardSubmissionButton = createKeyPressSensitiveButton("Cancel this submission", 'c') {
            closeOptionPane()
        }
        //createKeyListener(KeyEvent.VK_ESCAPE) { closeOptionPane() }
        val buttons = mutableListOf(replaceButton, cancelCardSubmissionButton)
        if (!question().flattenedEquals(originalQuestion()!!)) buttons.add(1, keepBothButton)
        return buttons.toTypedArray()
    }


    private fun addCardPanel() {
        val upperPanel = JSplitPane(JSplitPane.VERTICAL_SPLIT, JScrollPane(cardFrontPane), JScrollPane(cardBackPane))
        upperPanel.resizeWeight = 0.5
        layout = GridBagLayout()
        val frontConstraints = GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            weightx = 1.0
            weighty = 1.0
            insets = Insets(0, 0, 5, 0)
            fill = GridBagConstraints.BOTH
        }
        add(upperPanel, frontConstraints)
    }

    private fun addButtonPanel() {
        val buttonPane = JPanel().apply {
            add(priorityLabel)
            if (entry != null) {
                add(changePriorityButton)
                add(deleteButton)
            }
            add(okButton)
        }
        val buttonPaneConstraints = GridBagConstraints().apply {
            gridx = 0
            gridy = 1
            weightx = 0.0
            weighty = 0.0
            insets = Insets(10, 10, 10, 10)
        }
        add(buttonPane, buttonPaneConstraints)
    }

    fun setQuestion(question: String?) {
        entry = null
        cardFrontPane.text = question
        cardBackPane.text = ""
    }

    fun focusOnQuestion() {
        cardFrontPane.requestFocus()
    }
}


