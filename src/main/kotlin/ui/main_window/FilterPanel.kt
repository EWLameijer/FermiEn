package ui.main_window

import eventhandling.DelegatingDocumentListener
import java.awt.Component
import java.awt.Dimension
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class FilterPanel(private val searchFieldListener: DelegatingDocumentListener) : JPanel() {
    private val searchLabel = JLabel("Search:", JLabel.CENTER)

    private val tagLabel = JLabel("Tag:", JLabel.CENTER)

    private val searchField = JTextField().apply {
        document.addDocumentListener(searchFieldListener)
    }

    fun resetAfterDeckChanged() {
        resetAfterCardAdded()
        tagField.text = ""
    }

    fun resetAfterCardAdded() {
        isVisible = true
        searchField.text = ""
        searchField.requestFocusInWindow()
    }

    fun getQuery(): String = searchField.text

    private val tagField = JTextField().apply {
        document.addDocumentListener(searchFieldListener)
    }

    fun getTag(): String = tagField.text

    private val items: List<Component> = listOf(searchLabel, searchField, tagLabel, tagField)
    private val maxHeight = items.maxOf { it.preferredSize.height }

    init {
        layout = null
        add(searchLabel)
        add(searchField)
        add(tagLabel)
        add(tagField)
        isVisible = true
        this.preferredSize = Dimension(100, maxHeight)
    }

    fun shiftContents(width: Int) {
        val searchLabelSize = searchLabel.preferredSize
        val targetSearchLabelWidth = searchLabelSize.width + 50
        searchLabel.setBounds(
            insets.left, insets.top,
            targetSearchLabelWidth, maxHeight
        )
        searchField.setBounds(
            insets.left + targetSearchLabelWidth, insets.top,
            width / 2 - (insets.left + searchLabelSize.width), maxHeight
        )
        searchField.setBounds(
            insets.left + targetSearchLabelWidth, insets.top,
            width / 2 - (insets.left + targetSearchLabelWidth), maxHeight
        )

        val targetTagLabelWidth = tagLabel.preferredSize.width + 50
        tagLabel.setBounds(
            width / 2, insets.top,
            targetTagLabelWidth, maxHeight
        )
        tagField.setBounds(
            width / 2 + targetTagLabelWidth, insets.top,
            width - (width / 2 + targetTagLabelWidth), maxHeight
        )

    }
}