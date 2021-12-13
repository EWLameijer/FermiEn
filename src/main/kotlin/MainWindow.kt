import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.event.WindowListener
import java.beans.EventHandler
import java.io.File
import javax.swing.*
import kotlin.system.exitProcess

class MainWindow : JFrame() {
    private val cardFrontPane = JTextPane().apply {
        text = "front"
    }

    private val cardBackPane = JTextPane().apply {
        text = "back"
    }

    private val okButton = JButton("Ok").apply {
        addActionListener { saveNewEntry() }
    }

    private fun saveNewEntry() {
        val front = cardFrontPane.text.toStorageString()
        val back = cardBackPane.text.toStorageString()
        println("$front\t$back")
        entries += Entry(front, back)
    }

    // NOTE: init has to be below the cardFrontPane and cardBackPane definitions, else it doesn't work
    // (tested 2021-12-12)
    init {
        addCardPanel()
        addButtonPanel()
        setSize(1000, 700)
        defaultCloseOperation = EXIT_ON_CLOSE
        isVisible = true
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(windowEvent: WindowEvent?) {
                saveAndQuit()
            }
        })
    }

    private fun saveAndQuit() {
        File(inputFileName).writeText(entries.joinToString(separator = "\n") { "${it.question}\t${it.answer}" })
        dispose()
        exitProcess(0)
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