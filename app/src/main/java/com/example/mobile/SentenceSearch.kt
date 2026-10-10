package com.example.mobile

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText

class SentenceSearch(private val root: View) {
    private val input = root.findViewById<TextInputEditText>(R.id.inputText)
    private val findButton = root.findViewById<Button>(R.id.findButton)
    private val result = root.findViewById<TextView>(R.id.resultText)

    fun setup(initializeInput: Boolean) {
        if (initializeInput) {
            input.setText(R.string.default_text)
        }
        findButton.setOnClickListener {
            showResult(SentenceAnalyzer.findLongest(input.text?.toString().orEmpty()))
            hideKeyboard()
        }
    }

    private fun showResult(analysis: SentenceAnalysis?) {
        result.text = if (analysis == null) {
            root.context.getString(R.string.no_text_result)
        } else {
            val heading = root.resources.getQuantityString(
                R.plurals.word_count,
                analysis.wordCount,
                analysis.wordCount
            )
            "$heading\n\n${analysis.sentences.joinToString("\n\n")}"
        }
    }

    private fun hideKeyboard() {
        val manager = root.context.getSystemService(Context.INPUT_METHOD_SERVICE)
            as? InputMethodManager
        manager?.hideSoftInputFromWindow(input.windowToken, 0)
    }
}
