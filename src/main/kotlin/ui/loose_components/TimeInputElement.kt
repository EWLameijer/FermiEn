package ui.loose_components


import Update
import data.TimeInterval
import data.TimeUnit
import data.doubleToMaxPrecisionString
import data.stringToDouble
import eventhandling.BlackBoard
import eventhandling.DelegatingDocumentListener
import java.awt.Dimension

import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

/**
 * A TimeInputElement contains a text field and combo box that allow the user to
 * input say "5.5 minutes" or "3 hours".
 *
 * @author Eric-Wubbo Lameijer
 */
class TimeInputElement(name: String, inputTimeInterval: TimeInterval) : JPanel() {
    private val label = JLabel(name)

    // Text field that sets the quantity (the '3' in 3 hours), the combo box
    // selects the unit (the hours in "3 hours").
    private val scalarField = JTextField()

    // Combo box used (in combination with a text field) to set the value for the
    // initial study interval. Contains the hours of "3 hours".
    private val unitComboBox = JComboBox<String>()

    private val documentListener = DelegatingDocumentListener { notifyDataFieldChangeListeners() }

    // Returns the time interval encapsulated by this TimeInputElement.
    var interval: TimeInterval
        get() {
            val timeUnit = TimeUnit.parseUnit(unitComboBox.selectedItem!!.toString())!!
            val timeIntervalScalar = stringToDouble(scalarField.text) ?: 0.01
            return TimeInterval(timeIntervalScalar, timeUnit)
        }
        set(timeInterval) {
            scalarField.text = doubleToMaxPrecisionString(timeInterval.scalar, 2)
            unitComboBox.selectedItem = timeInterval.unit.userInterfaceName
            notifyDataFieldChangeListeners()
        }

    init {
        scalarField.document = FixedSizeNumberDocument(scalarField, 5, 2)
        scalarField.document.addDocumentListener(documentListener)

        scalarField.preferredSize = Dimension(40, 20)
        unitComboBox.model = DefaultComboBoxModel(TimeUnit.unitNames())
        unitComboBox.addActionListener { notifyDataFieldChangeListeners() }
        interval = inputTimeInterval
        add(label)
        add(scalarField)
        add(unitComboBox)
    }

    private fun notifyDataFieldChangeListeners() = BlackBoard.post(Update(UpdateType.INPUTFIELD_CHANGED))
}