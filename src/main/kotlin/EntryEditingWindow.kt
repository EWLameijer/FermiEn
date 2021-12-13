import java.awt.*
import javax.swing.*
import javax.swing.JComponent.WHEN_FOCUSED

class EntryEditingWindow(private val entry: Entry? = null) : JFrame() {
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
            cardFrontPane.text = StoredStringParser(entry.question).toDisplayString()
            cardBackPane.text = StoredStringParser(entry.answer).toDisplayString()
        }
        addCardPanel()
        addButtonPanel()
        setSize(650, 400)
        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        isVisible = true
    }

    private fun saveNewEntry() {
        val front = cardFrontPane.text.toStorageString()
        val back = cardBackPane.text.toStorageString()
        println("$front\t$back")
        if (entries.any { it.toHorizontalDisplay().first == front.toHorizontalString() }) {
            JOptionPane.showMessageDialog(null, "An entry with this question already exists",
                "Duplicate entry", JOptionPane.ERROR_MESSAGE)
        } else {
            entries += Entry(front, back)
            cardFrontPane.text = ""
            cardBackPane.text = ""
        }
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