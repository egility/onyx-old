/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.android.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.TextView
import org.egility.android.R
import org.egility.android.tools.AndroidUtils

/**
 * Created by mbrickman on 11/04/16.
 */
class QuickButton(context: Context, val attrs: AttributeSet?, defStyle: Int) : TextView(context, attrs, defStyle) {

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, android.R.attr.textViewStyle)

    constructor(context: Context) : this(context, null)

    var frame = false

    init {
        if (attrs != null) {
            val attributes = context.theme.obtainStyledAttributes(attrs, R.styleable.QuickButton, 0, 0)
            frame = !attributes.getBoolean(R.styleable.QuickButton_navigation, false)
        }
        isLongClickable = false
    }

    var isDown = false


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDown = true
                    postInvalidate()
                }
                MotionEvent.ACTION_UP -> {
                    isDown = false
                    postInvalidate()
                }
            }
        }
        return super.onTouchEvent(event)
    }

    val paint = Paint()


    override fun onDraw(canvas: Canvas?) {
        val textColor = Color.BLACK
        val backgroundColor = Color.WHITE
        val midColor = Color.GRAY


        val borderWidth = AndroidUtils.dpToPx(context, 2).toFloat()
        this.setTextColor(if (isEnabled) textColor else midColor)
        if (canvas != null) {
            var rect = canvas.clipBounds
            paint.color = if (isEnabled) textColor else midColor
            paint.style = Paint.Style.FILL

            if (frame) {

                canvas.drawRect(rect, paint)

                if (AndroidUtils.isFire) {
                    rect.left += 1
                    rect.right -= 1
                    rect.top += 1
                    rect.bottom -= 1
                } else {
                    rect.left += 2
                    rect.right -= 2
                    rect.top += 2
                    rect.bottom -= 2
                }
            }

            paint.color = backgroundColor
            canvas.drawRect(rect, paint)

            if (isDown && isEnabled) {
                rect = canvas.clipBounds
                rect.bottom = rect.top + 6
                paint.color = textColor
                canvas.drawRect(rect, paint)
            }
        }
        val textPaint = getPaint()
        textPaint.color = if (isEnabled) textColor else midColor
        super.onDraw(canvas)
    }


}