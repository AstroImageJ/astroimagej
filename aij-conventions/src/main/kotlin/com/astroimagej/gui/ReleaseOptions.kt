package com.astroimagej.gui

import com.astroimagej.updates.Type
import java.awt.*
import javax.swing.*

data class ReleaseOptions(
    val version: String,
    val skipRelease: Boolean,
    val notarize: Boolean,
    val windowsSign: Boolean,
    val crosspackage: Boolean,
    val releaseType: Type,
)

fun showReleaseOptionsDialog(parent: Frame? = null, releaseType: Type, version: String): ReleaseOptions? {
    val versionField = JTextField().apply {
        columns = 20
        toolTipText = "Release version (e.g. x.y.z.w)"
        text = version
    }

    val skipReleaseCb = JCheckBox("Skip creating a GitHub release").apply {
        isSelected = false
        toolTipText = "Skip creating a GitHub release"
    }

    val notarizeCb = JCheckBox("Notarize Mac Package").apply {
        isSelected = true
        toolTipText = "Notarize Mac Package"
    }

    val windowsSignCb = JCheckBox("Sign Windows Package").apply {
        isSelected = true
        toolTipText = "Sign Windows Package"
    }

    val crosspackageCb = JCheckBox("Cross package applications").apply {
        isSelected = true
        toolTipText = "Should not be changed, if disabled use jpackage to create the app images"
    }

    val releaseTypeCombo = JComboBox(Type.entries.toTypedArray()).apply {
        selectedItem = releaseType
        toolTipText = "Release type"
    }

    // Build layout
    val panel = JPanel(GridBagLayout())
    val c = GridBagConstraints().apply {
        fill = GridBagConstraints.HORIZONTAL
        anchor = GridBagConstraints.WEST
        weightx = 1.0
        insets = Insets(6, 6, 6, 6)
    }

    fun addLabelAnd(compLabel: String, component: JComponent, row: Int) {
        val label = JLabel(compLabel)
        c.gridx = 0
        c.gridy = row
        c.weightx = 0.0
        panel.add(label, c)
        c.gridx = 1
        c.weightx = 1.0
        panel.add(component, c)
    }

    var row = 0
    addLabelAnd("Version:", versionField, row++)

    c.gridx = 0
    c.gridy = row++
    c.gridwidth = 2
    panel.add(skipReleaseCb, c)
    c.gridy = row++
    panel.add(notarizeCb, c)
    c.gridy = row++
    panel.add(windowsSignCb, c)
    c.gridy = row++
    panel.add(crosspackageCb, c)
    c.gridwidth = 1
    addLabelAnd("Release type:", releaseTypeCombo, row++)

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

    var result: ReleaseOptions? = null

    okButton.addActionListener {
        val version = versionField.text.trim()
        if (version.isEmpty()) {
            JOptionPane.showMessageDialog(
                dialog,
                "Version is required and cannot be empty.",
                "Validation error",
                JOptionPane.ERROR_MESSAGE
            )
            versionField.requestFocusInWindow()
            return@addActionListener
        }

        val r = Regex("\\d+\\.\\d+\\.\\d+\\.\\d+")
        if (!r.matches(version)) {
            JOptionPane.showMessageDialog(
                dialog,
                "Version must be in the format x.y.z.w where x, y, z, and w are non-negative integer numbers.",
                "Validation error",
                JOptionPane.ERROR_MESSAGE
            )
            versionField.requestFocusInWindow()
            return@addActionListener
        }

        result = ReleaseOptions(
            version = version,
            skipRelease = skipReleaseCb.isSelected,
            notarize = notarizeCb.isSelected,
            windowsSign = windowsSignCb.isSelected,
            crosspackage = crosspackageCb.isSelected,
            releaseType = releaseTypeCombo.selectedItem as Type,
        )
        dialog.dispose()
    }

    dialog.defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE

    cancelButton.addActionListener {
        result = null
        dialog.dispose()
    }

    dialog.setLocationRelativeTo(parent)
    dialog.setIconImage(ImageIcon("icons/aij.png").image)
    dialog.isVisible = true
    dialog.requestFocus()
    return result
}

