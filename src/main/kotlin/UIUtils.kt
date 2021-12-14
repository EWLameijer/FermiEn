import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.KeyStroke
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