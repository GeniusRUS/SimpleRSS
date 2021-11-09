package com.genius.srss.ui

import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.genius.srss.R
import com.genius.srss.util.getAttrColorValue
import dev.chrisbanes.insetter.applyInsetter

class TutorialView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
): ConstraintLayout(context, attributeSet, defStyleAttr), View.OnClickListener {

    private var messageView: TextView? = null
    private var skip: Button? = null
    private var next: Button? = null
    private var previous: Button? = null
    private var parentView: View? = null

    private var skipCallback: ((view: TutorialView) -> Unit)? = null

    init {
        val view = View.inflate(context, R.layout.component_tutorial, this)

        messageView = view.findViewById(R.id.message)
        skip = view.findViewById(R.id.skip)
        next = view.findViewById(R.id.to_next_tip)
        previous = view.findViewById(R.id.to_previous_tip)

        skip?.setOnClickListener(this)
        next?.setOnClickListener(this)
        previous?.setOnClickListener(this)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        applyInsetter {
            type(navigationBars = true, statusBars = true) {
                margin()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val blurRenderer = RenderEffect.createBlurEffect(
                20F,
                20F,
                Shader.TileMode.CLAMP
            )
            parentView?.setRenderEffect(blurRenderer)
        } else {
            val darkenShape = GradientDrawable().apply {
                this.setColor(context.getAttrColorValue(android.R.attr.windowBackground))
                this.alpha = 175.coerceIn(0, 255)
            }
            background = darkenShape
        }

        messageView?.setText(R.string.tutorial_assign_subscription_to_folder)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.skip -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    parentView?.setRenderEffect(null)
                } else {
                    background = null
                }
                skipCallback?.invoke(this)
            }
            R.id.to_next_tip -> {

            }
            R.id.to_previous_tip -> {

            }
        }
    }

    fun setRootView(view: View) {
        this.parentView = view
        requestLayout()

        // TODO maybe not better solution. need to redraw blur effect on updating [parentView]
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            this.parentView?.viewTreeObserver?.addOnDrawListener {
                invalidate()
            }
        }
    }

    fun setSkipCallback(action: (view: TutorialView) -> Unit) {
        this.skipCallback = action
    }

    companion object {
        const val IS_TUTORIAL_SHOW = "is_tutorial_show"
    }
}