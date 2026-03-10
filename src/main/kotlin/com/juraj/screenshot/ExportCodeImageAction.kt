package com.juraj.screenshot

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ui.Messages

class ExportCodeImageAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)

        if (editor == null || !editor.selectionModel.hasSelection()) {
            Messages.showInfoMessage("Please select some code first.", "Beautiful Code Screenshot")
            return
        }

        try {
            val defaultTitle = FileDocumentManager.getInstance().getFile(editor.document)?.name ?: ""
            PreviewDialog(e.project, editor, defaultTitle).show()
        } catch (ex: Exception) {
            Messages.showErrorDialog("Failed to render image:\n${ex.message}", "Beautiful Code Screenshot")
        }
    }
}
