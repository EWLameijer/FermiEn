package ui.loose_components

import Update
import eventhandling.BlackBoard
import java.awt.Dimension
import javax.swing.JCheckBox

import javax.swing.JLabel
import javax.swing.JPanel


class LabelledCheckbox(labelText: String, checked: Boolean) : JPanel() {
    private val label = JLabel(labelText)
    private val checkBox = JCheckBox().apply {
        preferredSize = Dimension(20, 20)
        isSelected = checked
        addActionListener { notifyCheckboxChangeListeners() }
    }

    private fun notifyCheckboxChangeListeners() = BlackBoard.post(Update(UpdateType.INPUTFIELD_CHANGED))

    init {
        add(label)
        add(checkBox)
    }

    fun contents(): Boolean = checkBox.isSelected

    fun setContents(checked: Boolean) {
        checkBox.isSelected = checked
    }
}
