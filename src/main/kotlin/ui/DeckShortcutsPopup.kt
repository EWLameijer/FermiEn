package ui

import data.Settings
import javax.swing.JButton
import javax.swing.JOptionPane

class DeckShortcutsPopup {
    private val shortcutsWithDeckData = Settings.shortcuts

    fun updateShortcuts() {
        val currentDeckName = Settings.currentFile()!!
        val currentDeckIndices = shortcutsWithDeckData.filterValues { it == currentDeckName }.keys
        val currentDeckIndex = if (currentDeckIndices.isEmpty()) null else currentDeckIndices.first()

        val cancelButton = createCancelButton()
        val removeShortcutButton = createRemoveShortcutButton(currentDeckIndex)
        val addShortcut = createAddShortcutButton(currentDeckIndex, currentDeckName)

        val buttons = arrayOf(cancelButton, removeShortcutButton, addShortcut)
        JOptionPane.showOptionDialog(
            null,
            "Do you want to create or remove a shortcut?",
            "Manage deck shortcuts", 0,
            JOptionPane.QUESTION_MESSAGE, null, buttons, null
        )
    }

    private fun createCancelButton() =
        JButton("Cancel").apply {
            addActionListener { closeOptionPane() }
        }

    private fun createRemoveShortcutButton(currentDeckIndex: Int?) =
        JButton("Remove shortcut").apply {
            isEnabled = if (currentDeckIndex != null) {
                addActionListener {
                    shortcutsWithDeckData.remove(currentDeckIndex)
                    closeOptionPane()
                }
                true
            } else false
        }

    private fun createAddShortcutButton(currentDeckIndex: Int?, currentDeckName: String) =
        JButton("Add shortcut").apply {
            isEnabled = if (currentDeckIndex == null) {
                val firstFreeIndex = getFirstFreeIndex()
                addActionListener {
                    shortcutsWithDeckData[firstFreeIndex] = currentDeckName
                    closeOptionPane()
                }
                true
            } else false
        }

    private fun getFirstFreeIndex() =
        (1..Settings.maxNumShortcuts).first { shortcutsWithDeckData[it] == null }
}