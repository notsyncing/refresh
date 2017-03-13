package io.github.notsyncing.refresh.app

import java.awt.BorderLayout
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JProgressBar

class RefreshAppLauncherGui {
    private val label: JLabel
    private val frame: JFrame
    private val progressBar: JProgressBar

    init {
        frame = JFrame()
        frame.setSize(500, 30)
        label = JLabel()
        label.text = "..."

        frame.add(BorderLayout.NORTH, label)
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.isResizable = false
        frame.isVisible = false
        frame.isUndecorated = true

        progressBar = JProgressBar(0, 500)
        progressBar.minimum = 0
        progressBar.maximum = 100
        frame.add(BorderLayout.CENTER, progressBar)

        frame.setLocationRelativeTo(null)
    }

    fun show() {
        frame.isVisible = true
    }

    fun hide() {
        frame.isVisible = false
    }

    fun changeText(text: String) {
        label.text = text
    }

    fun changeProgress(progress: Int) {
        if (progress >= 0) {
            progressBar.isIndeterminate = false
            progressBar.value = progress
        } else {
            progressBar.isIndeterminate = true
        }
    }

    fun destroy() {
        frame.dispose()
    }
}