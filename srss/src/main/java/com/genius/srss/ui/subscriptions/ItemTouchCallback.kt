package com.genius.srss.ui.subscriptions

import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper

class ItemTouchCallback(
    private val listener: TouchListener,
    private val deleteIconDrawable: Drawable,
    @ColorInt
    private val backgroundColor: Int
) : ItemTouchHelper.Callback() {

    // хороший тутор по возможностям helper'a
    // https://codeburst.io/android-swipe-menu-with-recyclerview-8f28a235ff28
    // а вот неплохая реализация Swipe delete
    // https://medium.com/@kitek/recyclerview-swipe-to-delete-easier-than-you-thought-cff67ff5e5f6

    private val intrinsicWidth = deleteIconDrawable.intrinsicWidth
    private val intrinsicHeight = deleteIconDrawable.intrinsicHeight
    private val background = ColorDrawable()
    private val clearPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        return makeMovementFlags(0, ItemTouchHelper.START or ItemTouchHelper.LEFT)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = listener.onItemDismiss(viewHolder.adapterPosition)

    override fun isLongPressDragEnabled(): Boolean = false

    override fun isItemViewSwipeEnabled(): Boolean = true

    override fun onChildDraw(canvas: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        val itemView = viewHolder.itemView
        val isCanceled = dX == 0f && !isCurrentlyActive

        if (isCanceled) {
            clearCanvas(canvas, itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
            super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        // Draw the red delete background
        background.color = backgroundColor
        background.setBounds(
            itemView.right + dX.toInt(),
            itemView.top,
            itemView.right,
            itemView.bottom
        )
        background.draw(canvas)

        // Calculate position of delete icon
        val itemHeight = itemView.bottom - itemView.top
        val iconMargin = (itemHeight - intrinsicHeight) / 2

        val iconLeft = itemView.right - iconMargin - intrinsicWidth
        val iconTop = itemView.top + iconMargin
        val iconRight = itemView.right - iconMargin
        val iconBottom = iconTop + intrinsicHeight

        // Draw the delete icon
        deleteIconDrawable.setBounds(
            (background.bounds.left + iconMargin).coerceAtLeast(iconLeft),
            iconTop,
            (background.bounds.left + iconMargin + intrinsicWidth).coerceAtLeast(iconRight),
            iconBottom
        )
        deleteIconDrawable.draw(canvas)

        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    private fun clearCanvas(c: Canvas?, left: Float, top: Float, right: Float, bottom: Float) {
        c?.drawRect(left, top, right, bottom, clearPaint)
    }

    interface TouchListener {
        fun onItemDismiss(position: Int)
    }
}