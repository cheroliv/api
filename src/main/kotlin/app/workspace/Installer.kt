package app.workspace

import app.Constants.EMPTY_STRING
import app.Loggers.w
import java.awt.EventQueue.invokeLater
import javax.swing.UIManager.getInstalledLookAndFeels
import javax.swing.UIManager.setLookAndFeel
import javax.swing.UnsupportedLookAndFeelException

object Installer {
    @JvmStatic
    fun main(args: Array<String>) = try {
        getInstalledLookAndFeels()
            .find { it.name == "Nimbus" }
            ?.let { setLookAndFeel(it.className) }
    } catch (ex: Exception) {
        when (ex) {
            is ClassNotFoundException,
            is InstantiationException,
            is IllegalAccessException,
            is UnsupportedLookAndFeelException -> w(EMPTY_STRING, ex)
            // Rethrow unknown exceptions
            else -> throw ex
        }
    }.run {
        invokeLater { run(::SetupSwingFrame).run { isVisible = true } }
    }
}