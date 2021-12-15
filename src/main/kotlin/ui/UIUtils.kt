import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class ProgrammableAction(private val m_action: () -> Unit) : AbstractAction() {
    override fun actionPerformed(ae: ActionEvent) = m_action()
}

class DelegatingDocumentListener(private val handler: () -> Unit) : DocumentListener {

    private fun processUpdate() = handler()

    override fun changedUpdate(arg0: DocumentEvent) = processUpdate()

    override fun insertUpdate(arg0: DocumentEvent) = processUpdate()

    override fun removeUpdate(arg0: DocumentEvent) = processUpdate()
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