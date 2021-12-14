import java.awt.event.ActionEvent
import javax.swing.AbstractAction
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