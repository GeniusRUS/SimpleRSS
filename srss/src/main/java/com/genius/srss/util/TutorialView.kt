package com.genius.srss.util

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.genius.srss.R
import com.ub.utils.renew
import dev.chrisbanes.insetter.applyInsetter

class TutorialView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attributeSet, defStyleAttr), View.OnClickListener {

    private var message: TextView? = null
    private var icon: ImageView? = null
    private var skip: Button? = null
    private var previous: Button? = null
    private var parentView: View? = null

    private var skipCallback: ((view: TutorialView) -> Unit)? = null
    private val displayedTips: MutableList<Tip> = mutableListOf()
    private var currentDisplayedTip = 0

    init {
        val view = View.inflate(context, R.layout.component_tutorial, this)

        message = view.findViewById(R.id.message)
        icon = view.findViewById(R.id.icon)
        skip = view.findViewById(R.id.skip)
        previous = view.findViewById(R.id.to_previous_tip)

        setOnClickListener(this)
        skip?.setOnClickListener(this)
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
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState() ?: return null

        return TutorialViewSavedState(superState).apply {
            currentTipPosition = this@TutorialView.currentDisplayedTip
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is TutorialViewSavedState) {
            super.onRestoreInstanceState(state)
            return
        }

        super.onRestoreInstanceState(state.superState)

        currentDisplayedTip = state.currentTipPosition
        updateDisplayedTip(currentDisplayedTip)
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
            R.id.to_previous_tip -> {
                if (currentDisplayedTip > 0) {
                    currentDisplayedTip -= 1
                }
                updateDisplayedTip(currentDisplayedTip)
            }
            else -> {
                if (currentDisplayedTip + 1 < displayedTips.size) {
                    currentDisplayedTip += 1
                    updateDisplayedTip(currentDisplayedTip)
                } else {
                    onClick(skip)
                }
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

    fun setDisplayedTips(tips: List<Tip>) {
        this.displayedTips.renew(tips)
        updateDisplayedTip(currentDisplayedTip)
    }

    private fun updateDisplayedTip(position: Int) {
        previous?.isVisible = currentDisplayedTip > 0
        displayedTips.getOrNull(position)?.let { tip ->
            message?.text = context.getString(tip.message)
            tip.icon?.let { iconResource ->
                icon?.setImageResource(iconResource)
            } ?: icon?.setImageDrawable(null)
            invalidate()
        }
    }

    class Tip(
        @StringRes val message: Int,
        @DrawableRes val icon: Int? = null
    )

    private class TutorialViewSavedState : BaseSavedState {
        var currentTipPosition: Int = 0

        constructor(superState: Parcelable) : super(superState)

        private constructor(`in`: Parcel) : super(`in`) {
            this.currentTipPosition = `in`.readInt()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(this.currentTipPosition)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<TutorialViewSavedState> {
            override fun createFromParcel(parcel: Parcel): TutorialViewSavedState {
                return TutorialViewSavedState(parcel)
            }

            override fun newArray(size: Int): Array<TutorialViewSavedState?> {
                return arrayOfNulls(size)
            }
        }
    }

    companion object {
        const val IS_TUTORIAL_SHOW = "is_tutorial_show"
    }
}