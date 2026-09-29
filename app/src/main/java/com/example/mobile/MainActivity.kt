package com.example.mobile

import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private val defaultText = "Наступило утро. Мы отправились в долгую и увлекательную поездку через весь город. Ради одной пары в шесть вечера. Зато не нормисы."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val input = findViewById<TextInputEditText>(R.id.inputText)
        val findButton = findViewById<Button>(R.id.findButton)
        val result = findViewById<TextView>(R.id.resultText)

        if (savedInstanceState == null) {
            input.setText(defaultText)
        }

        findButton.setOnClickListener {
            val text = input.text?.toString().orEmpty()
            val analysis = SentenceAnalyzer.findLongest(text)

            result.text = if (analysis == null) {
                getString(R.string.no_text_result)
            } else {
                val heading = resources.getQuantityString(
                    R.plurals.word_count,
                    analysis.wordCount,
                    analysis.wordCount
                )
                "$heading\n\n${analysis.sentences.joinToString("\n\n")}"
            }

            val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(input.windowToken, 0)
        }
    }
}
