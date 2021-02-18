package com.genius.srss.ui.folder

import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.genius.srss.R
import kotlin.reflect.KClass

class FolderTouchHelperCallback(context: Context,
                                private val listener: TouchFolderListener,
                                @DrawableRes drawableRes: Int,
                                @ColorInt backgroundColorInt: Int,
                                private val excludedSwipeClasses: List<KClass<out RecyclerView.ViewHolder>>? = null) : ItemTouchHelper.Callback() {

    // хороший тутор по возможностям helper'a
    // https://codeburst.io/android-swipe-menu-with-recyclerview-8f28a235ff28
    // а вот неплохая реализация Swipe delete
    // https://medium.com/@kitek/recyclerview-swipe-to-delete-easier-than-you-thought-cff67ff5e5f6

    private val deleteIcon = ResourcesCompat.getDrawable(context.resources, drawableRes, context.theme)
    private val intrinsicWidth = deleteIcon?.intrinsicWidth ?: 0
    private val intrinsicHeight = deleteIcon?.intrinsicHeight ?: 0
    private val background = ColorDrawable().apply {
        color = backgroundColorInt
    }
    private val clearPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        return if (excludedSwipeClasses?.firstOrNull { clazz -> viewHolder::class == clazz::class } == null) {
            makeMovementFlags(0, ItemTouchHelper.END or ItemTouchHelper.RIGHT)
        } else {
            makeMovementFlags(0, 0)
        }
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = listener.onFolderDismiss(viewHolder.adapterPosition)

    override fun isLongPressDragEnabled(): Boolean = false

    override fun isItemViewSwipeEnabled(): Boolean = true

    override fun onChildDraw(canvas: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        val itemView = viewHolder.itemView
        val isCanceled = dX == 0f && !isCurrentlyActive

        if (isCanceled) {
            canvas.drawRect(
                itemView.right + dX,
                itemView.top.toFloat(),
                itemView.right.toFloat(),
                itemView.bottom.toFloat(),
                clearPaint
            )
            itemView.background = itemView.context.getSelectableBackgroundDrawable()
            super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        background.setBounds(
            itemView.left,
            itemView.top,
            itemView.left + dX.toInt(),
            itemView.bottom
        )
        background.draw(canvas)

        // Calculate position of delete icon
        val itemHeight = itemView.bottom - itemView.top
        val iconMargin = (itemHeight - intrinsicHeight) / 2

        val iconLeft = itemView.left + iconMargin
        val iconTop = itemView.top + iconMargin
        val iconRight = itemView.left + iconMargin + intrinsicWidth
        val iconBottom = iconTop + intrinsicHeight

        // Draw the delete icon
        deleteIcon?.setBounds(
            (background.bounds.right - iconMargin - intrinsicWidth).coerceAtMost(iconLeft),
            iconTop,
            (background.bounds.right - iconMargin).coerceAtMost(iconRight),
            iconBottom
        )
        deleteIcon?.draw(canvas)

        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    private fun Context.getSelectableBackgroundDrawable(): Drawable? {
        val outValue = TypedValue()
        theme.resolveAttribute(R.attr.selectableItemBackground, outValue, true)
        return ContextCompat.getDrawable(this, outValue.resourceId)
    }

    interface TouchFolderListener {
        fun onFolderDismiss(position: Int)
    }
}