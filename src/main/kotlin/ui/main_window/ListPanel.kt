package ui.main_window

import data.EntryManager
import data.utils.inHtml
import data.utils.linesOfMaxLength
import eventhandling.BlackBoard
import eventhandling.DelegatingDocumentListener
import ui.EntryEditingPanel
import ui.createKeyListener
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.table.DefaultTableModel

class ListPanel : JPanel() {

    val searchFieldListener = DelegatingDocumentListener { updateTable() }

    private lateinit var entryEditingPanel: EntryEditingPanel

    fun isEditing() = entryEditingPanel.isVisible

    fun saveState() = entryEditingPanel.saveNewEntry()

    private val searchField = JTextField().apply {
        document.addDocumentListener(searchFieldListener)
    }

    // from https://stackoverflow.com/questions/9467093/how-to-add-a-tooltip-to-a-cell-in-a-jtable
    private val table = object : JTable() {
        override fun getToolTipText(e: MouseEvent): String? {
            var tip: String? = null
            val p = e.point
            val rowIndex = rowAtPoint(p)
            val colIndex = columnAtPoint(p)
            try {
                tip = getValueAt(rowIndex, colIndex).toString().linesOfMaxLength(100).inHtml()
            } catch (_: RuntimeException) {
                //catch null pointer exception if mouse is over an empty line
            }
            return tip
        }
    }

    private val scrollPane = JScrollPane(table)

    private fun updateTable() {
        val tableModel = UnchangeableTableModel()
        tableModel.addColumn("question")
        tableModel.addColumn("answer")
        EntryManager.getHorizontalRepresentation().filter(::searchContentsInHorizontalEntry)
            .sortedBy { it.first.lowercase() }.forEach {
                tableModel.addRow(arrayOf(it.first, it.second))
            }
        table.model = tableModel
    }

    private fun searchContentsInHorizontalEntry(entry: Pair<String, String>): Boolean {
        val searchString = if (entryEditingPanel.isVisible) entryEditingPanel.frontText()
        else searchField.text
        val searchTerms = searchString.lowercase().split(' ')
        return searchTerms.all { it in entry.first.lowercase() || it in entry.second.lowercase() }
    }

    class UnchangeableTableModel : DefaultTableModel() {
        override fun isCellEditable(row: Int, column: Int): Boolean {
            //all cells false
            return false
        }
    }

    fun activateEntryPanel() {
        if (entryEditingPanel.isVisible) return // do nothing
        entryEditingPanel.isVisible = true
        entryEditingPanel.setQuestion(searchField.text)
        searchField.text = ""
        searchField.isVisible = false
    }

    fun setup() {
        BlackBoard.register({ updateTable() }, UpdateType.ENCY_CHANGED, UpdateType.ENCY_SWAPPED)
        createKeyListener(KeyEvent.VK_ESCAPE) {
            resetPanel()
        }
        entryEditingPanel = EntryEditingPanel(this)
        layout = GridBagLayout()
        add(entryEditingPanel, editCardConstraints)
        entryEditingPanel.isVisible = false
        add(searchField, searchBoxConstraints)
        add(scrollPane, tableConstraints)

        initializeEntriesList()
    }

    private val tableConstraints = GridBagConstraints().apply {
        gridx = 1
        gridy = 1
        weightx = 1.0
        weighty = 1000.0
        insets = Insets(0, 0, 0, 0)
        fill = GridBagConstraints.BOTH
    }

    private val searchBoxConstraints = GridBagConstraints().apply {
        gridx = 1
        gridy = 0
        weightx = 1.0
        weighty = 1.0
        insets = Insets(0, 0, 0, 0)
        fill = GridBagConstraints.BOTH
    }

    private val editCardConstraints = GridBagConstraints().apply {
        gridx = 0
        gridy = 0
        weightx = 1.0
        weighty = 1.0
        insets = Insets(0, 0, 0, 0)
        fill = GridBagConstraints.BOTH
        gridheight = 2
    }

    private val tableModel = UnchangeableTableModel().apply {
        addColumn("question")
        addColumn("answer")
        EntryManager.getHorizontalRepresentation().sortedBy { it.first.lowercase() }.forEach {
            addRow(arrayOf(it.first, it.second))
        }
    }

    private fun initializeEntriesList() {
        table.model = tableModel
        table.fillsViewportHeight = true
        table.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(mouseEvent: MouseEvent) {
                val table = mouseEvent.source as JTable
                val point = mouseEvent.point
                val row = table.rowAtPoint(point)
                if (mouseEvent.clickCount == 2 && table.selectedRow != -1) {
                    val key = table.getValueAt(row, 0) as String
                    EntryManager.editEntryByQuestion(key)
                }
            }
        })
    }

    fun resetPanel() {
        with(searchField) {
            isVisible = true
            text = ""
            requestFocusInWindow()
        }
        entryEditingPanel.isVisible = false
        updateTable()
    }
}