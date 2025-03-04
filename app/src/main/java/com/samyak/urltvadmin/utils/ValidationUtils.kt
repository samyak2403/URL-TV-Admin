package com.samyak.urltvadmin.utils

import android.widget.EditText
import android.util.Patterns

object ValidationUtils {
    fun validateRequired(editText: EditText, errorMessage: String): Boolean {
        val text = editText.text.toString().trim()
        return if (text.isEmpty()) {
            editText.error = errorMessage
            false
        } else {
            true
        }
    }

    fun validateUrl(editText: EditText, errorMessage: String): Boolean {
        val url = editText.text.toString().trim()
        return if (url.isEmpty() || !Patterns.WEB_URL.matcher(url).matches()) {
            editText.error = errorMessage
            false
        } else {
            true
        }
    }
} 