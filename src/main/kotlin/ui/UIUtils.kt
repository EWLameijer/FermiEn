package ui

import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import javax.swing.*

fun closeOptionPane() = JOptionPane.getRootFrame().dispose()

class ProgrammableAction(private val m_action: () -> Unit) : AbstractAction() {
    override fun actionPerformed(ae: ActionEvent) = m_action()
}

fun JFrame.createKeyListener(keyEvent: Int, action: () -> Unit) {
    rootPane.createKeyListener(KeyStroke.getKeyStroke(keyEvent, 0), action)
}

fun JComponent.createKeyListener(keyStroke: KeyStroke, action: () -> Unit) {
    val eventId = "Pressed$keyStroke"
    getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, eventId)
    actionMap.put(eventId, ProgrammableAction(action))
}

private fun createKeyPressSensitiveButton(text: String, actionKey: KeyStroke, action: () -> Unit): JButton =
    JButton(text).apply {
        mnemonic = KeyEvent.getExtendedKeyCodeForChar(actionKey.keyChar.code)
        createKeyListener(actionKey, action)
        addActionListener { action() }
    }

fun createKeyPressSensitiveButton(text: String, key: Char, action: () -> Unit): JButton =
    createKeyPressSensitiveButton(text, KeyStroke.getKeyStroke(key), action)

fun createKeyPressSensitiveButton(text: String, key: String, action: () -> Unit): JButton =
    createKeyPressSensitiveButton(text, KeyStroke.getKeyStroke(key), action)

class UnfocusableButton (text: String, action: ActionListener) : JButton(text) {
    init {
        addActionListener(action)
        isFocusable = false
    }
}