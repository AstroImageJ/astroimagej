package com.astroimagej.gui

import com.astroimagej.updates.Type
import java.awt.*
import javax.swing.*

fun showReleaseOptionsDialog(parent: Frame? = null, releaseType: Type, version: String, options: Map<String, WorkflowInput>): Map<String, String> {
    // Build layout
    val panel = JPanel(GridBagLayout())
    val c = GridBagConstraints().apply {
        fill = GridBagConstraints.HORIZONTAL
        anchor = GridBagConstraints.WEST
        weightx = 1.0
        insets = Insets(6, 6, 6, 6)
        gridy = 0
    }

    val processed = mutableListOf<Option>()

    options.forEach { (name, input) ->
        if (input.type == InputType.BOOLEAN) {
            c.gridwidth = 2
        } else {
            c.gridwidth = 1
        }
        val o = toComponent(name, input, version)
        processed.add(o)
        panel.add(o.comp, c)
        c.gridy++
    }

    val okButton = JButton("OK")
    val cancelButton = JButton("Cancel")
    val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
    buttonPanel.add(cancelButton)
    buttonPanel.add(okButton)

    val dialog = JDialog(parent, "Release Options", true).apply {
        contentPane.add(panel, "Center")
        contentPane.add(buttonPanel, "South")
        rootPane.defaultButton = okButton
        minimumSize = Dimension(420, 300)
        pack()
    }

    var result: Map<String, String> = emptyMap()

    okButton.addActionListener {
        result = processed.associate { it.valueFunc() }
        dialog.dispose()
    }

    dialog.defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE

    cancelButton.addActionListener {
        result = emptyMap()
        dialog.dispose()
    }

    dialog.setLocationRelativeTo(parent)
    dialog.setIconImage(ImageIcon("icons/aij.png").image)
    dialog.isAlwaysOnTop = true
    dialog.toFront()
    dialog.isVisible = true
    dialog.requestFocus()
    return result
}

fun toComponent(name: String, input: WorkflowInput, version: String): Option {
    if (name == "version") {
        val comps = version.split(".")

        val b = Box.createHorizontalBox()
        val major = JSpinner(SpinnerNumberModel(comps[0].toInt(), 0, 999, 1))
        val minor = JSpinner(SpinnerNumberModel(comps[1].toInt(), 0, 999, 1))
        val patch = JSpinner(SpinnerNumberModel(comps[2].toInt(), 0, 999, 1))
        val daily = JSpinner(SpinnerNumberModel(comps[3].toInt(), 0, 999, 1))

        b.add(JLabel(input.description ?: name))
        b.add(Box.createHorizontalStrut(10))
        b.add(Box.createHorizontalGlue())
        b.add(major)
        b.add(minor)
        b.add(patch)
        b.add(daily)

        return Option(name, b) {
            Pair(name, "${major.value}.${minor.value}.${patch.value}.${daily.value.toString().padStart(2, '0')}")
        }
    }

    return when (input.type) {
        InputType.STRING -> {
            val tf = JTextField().apply {
                columns = 20
                toolTipText = input.description
                text = input.default
            }

            val b = Box.createHorizontalBox()
            b.add(JLabel(input.description ?: name))
            b.add(Box.createHorizontalStrut(10))
            b.add(Box.createHorizontalGlue())
            b.add(tf)
            Option(name, b) { Pair(name, tf.text) }
        }
        InputType.BOOLEAN -> {
            val b = input.default.toBoolean()
            val cb = JCheckBox(input.description ?: name).apply {
                isSelected = b
                toolTipText = input.description
            }
            Option(name, cb) { Pair(name, cb.isSelected.toString()) }
        }
        InputType.CHOICE -> {
            val combo = JComboBox(input.options!!.toTypedArray()).apply {
                selectedItem = input.default ?: input.options.first()
                toolTipText = input.description
            }

            val b = Box.createHorizontalBox()
            b.add(JLabel(input.description ?: name))
            b.add(Box.createHorizontalStrut(10))
            b.add(Box.createHorizontalGlue())
            b.add(combo)
            Option(name, b) { Pair(name, combo.selectedItem as String) }
        }
    }
}

data class Option(
    val name: String,
    val comp: JComponent,
    val valueFunc: () -> Pair<String, String>
)
