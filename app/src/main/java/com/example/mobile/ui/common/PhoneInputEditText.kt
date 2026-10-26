package com.example.mobile.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.text.Editable
import android.text.TextPaint
import android.text.TextWatcher
import android.util.AttributeSet
import androidx.core.graphics.withClip
import com.example.mobile.R
import com.google.android.material.textfield.TextInputEditText

class PhoneInputEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.editTextStyle,
) : TextInputEditText(context, attrs, defStyleAttr) {
    private val mask = PhoneMask(context.getString(R.string.phone_hint))
    private val placeholderPaint = TextPaint()
    private var formatting = false
    private var previous = ""
    private var deletionStart = 0
    private var deletingSeparator = false

    init {
        textDirection = TEXT_DIRECTION_LTR
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (formatting) return
                previous = s?.toString().orEmpty()
                deletionStart = start
                deletingSeparator = count == 1 && after == 0 &&
                    previous.getOrNull(start)?.let { it !in '0'..'9' } == true
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                if (formatting || s == null) return
                var raw = s.toString()
                var cursor = selectionEnd.coerceIn(0, raw.length)
                if (deletingSeparator && deletionStart >= mask.prefix.length) {
                    val precedingDigit = previous.take(deletionStart).indexOfLast { it in '0'..'9' }
                    if (precedingDigit >= mask.prefix.length && precedingDigit < raw.length) {
                        raw = raw.removeRange(precedingDigit, precedingDigit + 1)
                        cursor = (cursor - 1).coerceAtLeast(mask.prefix.length)
                    }
                }
                val formatted = mask.format(raw)
                if (s.toString() != formatted) {
                    val atEnd = cursor >= raw.length
                    val digitsBeforeCursor = raw.take(cursor).count { it in '0'..'9' }
                    formatting = true
                    s.replace(0, s.length, formatted)
                    val digitPositions = formatted.indices.filter { formatted[it] in '0'..'9' }
                    val position = if (atEnd) formatted.length else
                        digitPositions.getOrNull(digitsBeforeCursor - 1)?.plus(1) ?: mask.prefix.length
                    setSelection(position.coerceIn(0, formatted.length))
                    formatting = false
                }
                invalidate()
            }
        })
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (focused && text.isNullOrEmpty()) {
            setText(mask.prefix)
            setSelection(text?.length ?: 0)
        } else if (!focused && text?.toString() == mask.prefix) {
            setText("")
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val value = text?.toString().orEmpty()
        val remaining = mask.remaining(value)
        val textLayout = layout ?: return
        if (remaining.isEmpty()) return
        placeholderPaint.set(paint)
        placeholderPaint.color = currentHintTextColor
        val x = compoundPaddingLeft + textLayout.getPrimaryHorizontal(value.length) - scrollX
        val y = baseline - scrollY
        canvas.withClip(compoundPaddingLeft, 0, width - compoundPaddingRight, height) {
            drawText(remaining, x, y.toFloat(), placeholderPaint)
        }
    }
}