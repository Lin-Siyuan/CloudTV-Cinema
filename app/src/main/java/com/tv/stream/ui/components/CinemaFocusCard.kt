// File: app/src/main/java/com/tv/stream/ui/components/CinemaFocusCard.kt
package com.tv.stream.ui.components

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout

class CinemaFocusCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.parseColor("#00E5FF")
    }

    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        setWillNotDraw(false)
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        // Promote to Mali-G31 hardware layer during scale animation
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        animate()
            .scaleX(if (gainFocus) 1.08f else 1.0f)
            .scaleY(if (gainFocus) 1.08f else 1.0f)
            .alpha(if (gainFocus) 1.0f else 0.82f)
            .setDuration(160)
            .withEndAction { setLayerType(View.LAYER_TYPE_NONE, null) }
            .start()
        invalidate()
    }
}
