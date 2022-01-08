package ui

import data.*
import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.WindowEvent
import javax.swing.*
import javax.swing.JComponent.WHEN_FOCUSED

class EntryEditingWindow(private var entry: Entry? = null) : JFrame() {
    private val priorityLabel = JLabel(priorityText())

    private val deleteButton = UnfocusableButton("Delete") {
        EntryManager.removeEntry(entry!!)
        clear()
    }

    private val changePriorityButton = UnfocusableButton("Change priority") { changePriority() }

    private fun priorityText() = if (entry == null) "" else "Priority ${entry!!.importance}"

    private fun changePriority() {
        val newPriorityAsString = JOptionPane.showInputDialog(this, "Enter new priority (1-10)", entry!!.importance)
        val newPriority = newPriorityAsString.toIntOrNull()
        if (newPriority == null || newPriority < 1 || newPriority > 10) JOptionPane.showMessageDialog(
            this,
            "'$newPriorityAsString' is an invalid value - enter a value between 1 and 10"
        ) else {
            entry!!.importance = newPriority
            priorityLabel.text = "Priority $newPriority"
        }
    }

    private val cardFrontPane = JTextPane().apply {
        makeTabTransferFocus(this)
    }

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
        createKeyListener(KeyEvent.VK_ESCAPE) { clearOrExit() }

        addCardPanel()
        addButtonPanel()
        setSize(650, 400)
        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        isVisible = true
        iconImage = ImageIcon("resources/FermiEn_neg.png").image
        updateTitle()
    }

    private fun updateTitle() {
        title = "${Settings.currentFile()!!.fileNamePart()}: " + if (entry == null) "add entry" else "edit entry"
    }

    private fun clearOrExit() {
        if (entry != null) closeWindow()
        if (cardFrontPane.text.isNotBlank() || cardBackPane.text.isNotBlank()) clear() else closeWindow()
    }

    private fun closeWindow() {
        dispatchEvent(WindowEvent(this, WindowEvent.WINDOW_CLOSING))
    }

    private fun clear() {
        entry = null
        cardFrontPane.apply {
            text = ""
            requestFocusInWindow()
        }
        cardBackPane.text = ""
        updateTitle()
    }

    private fun question() = cardFrontPane.text.toStorageString()

    private fun answer() = cardBackPane.text.toStorageString()

    private fun originalQuestion(): StorageString? = entry?.question

    private fun saveNewEntry() {
        if (question().toHorizontalString().isBlank()) {
            JOptionPane.showMessageDialog(this, "Cannot add a card with a blank front")
            return
        }
        if (entry != null) { // are you trying to replace the card/front?
            val originalQuestion = originalQuestion()!!
            val originalAnswer = entry!!.answer
            if (question().flattenedEquals(originalQuestion) && answer().flattenedEquals(originalAnswer)) closeWindow()
            else { //because closeWindow does not stop the process...
                val buttons = getFrontChangeButtons()
                JOptionPane.showOptionDialog(
                    null,
                    stringCompareResult(originalAnswer),
                    "Are you sure you want to update the current card?", 0,
                    JOptionPane.QUESTION_MESSAGE, null, buttons, null
                )
            }
        } else {
            submitEntry()
        }
        createKeyListener(KeyEvent.VK_ESCAPE) { clearOrExit() } // set escape back
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
        clearOrExit() // enable adding new cards, but modifying an existing card should not result in a 'card chain'
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
        createKeyListener(KeyEvent.VK_ESCAPE) { closeOptionPane() }
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
}


