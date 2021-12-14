import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.WindowEvent
import javax.swing.*
import javax.swing.JComponent.WHEN_FOCUSED


class EntryEditingWindow(private var entry: Entry? = null) : JFrame() {
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
            cardFrontPane.text = entry!!.question.toDisplayString()
            cardBackPane.text = entry!!.answer.toDisplayString()
        }
        createKeyListener(KeyEvent.VK_ESCAPE) { clearOrExit() }

        addCardPanel()
        addButtonPanel()
        setSize(650, 400)
        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        isVisible = true
    }

    private fun clearOrExit() {
        if (cardFrontPane.text.isNotBlank() || cardBackPane.text.isNotBlank()) clear() else closeWindow()
    }

    private fun closeWindow() {
        dispatchEvent(WindowEvent(this, WindowEvent.WINDOW_CLOSING))
    }

    private fun clear() {
        entry = null
        cardFrontPane.text = ""
        cardBackPane.text = ""
    }

    private fun question() = cardFrontPane.text.toStorageString()

    private fun answer() = cardBackPane.text.toStorageString()

    private fun saveNewEntry() {
        if (entry != null) { // are you trying to replace the card/front?
            val originalQuestion = entry!!.question.toHorizontalString()
            val originalAnswer = entry!!.answer.toHorizontalString()
            val buttons = getFrontChangeButtons()
            JOptionPane.showOptionDialog(
                null,
                """Replace the card
                           '$originalQuestion' / '$originalAnswer' with
                           '${question().toHorizontalString()}' / '${answer().toHorizontalString()}'?""",
                "Are you sure you want to update the current card?", 0,
                JOptionPane.QUESTION_MESSAGE, null, buttons, null
            )
        } else {
            submitEntry()
        }
    }

    private fun submitEntry() {
        EntryManager.addEntry(Entry(question(), answer()))
        clear()
    }

    private fun getFrontChangeButtons(): Array<JButton> {
        val replaceButton = JButton("Replace card").apply {
            addActionListener {
                EntryManager.removeEntry(entry!!)
                submitEntry()
                closeOptionPane()
            }
        }
        val keepBothButton = JButton("Keep both cards").apply {
            addActionListener {
                submitEntry()
                closeOptionPane()
            }
        }
        val cancelCardSubmissionButton = JButton("Cancel this submission").apply {
            addActionListener {
                closeOptionPane()
            }
        }
        return arrayOf(replaceButton, keepBothButton, cancelCardSubmissionButton)
    }

    private fun closeOptionPane() = JOptionPane.getRootFrame().dispose()

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