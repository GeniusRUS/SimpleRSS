package com.genius.srss.ui

import android.content.Context
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.genius.srss.R

class TutorialView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
): ConstraintLayout(context, attributeSet, defStyleAttr) {

    private var messageView: TextView? = null
    private var parentView: View? = null

    init {
        val view = View.inflate(context, R.layout.component_tutorial, this)

        messageView = view.findViewById(R.id.message)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val blurRenderer = RenderEffect.createBlurEffect(
                20F,
                20F,
                Shader.TileMode.CLAMP
            )
            parentView?.setRenderEffect(blurRenderer)
        } else {
            val darkenShape = GradientDrawable().apply {
                this.setColor(Color.CYAN)
                this.alpha = 175 // 255 max value
            }
            background = darkenShape
        }

        messageView?.setText(R.string.tutorial_assign_subscription_to_folder)
    }

    fun setRootView(view: View) {
        this.parentView = view
        requestLayout()
    }
}