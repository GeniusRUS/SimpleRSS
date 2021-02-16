package com.genius.srss.ui.subscriptions

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class ItemTouchCallback(
    private val listener: TouchListener,
    private val deleteIconDrawable: Drawable,
    @ColorInt
    private val backgroundColor: Int,
    private val excludedViewHolderTypes: List<KClass<out RecyclerView.ViewHolder>> = listOf(),
    private val coroutineScope: CoroutineScope
) : ItemTouchHelper.Callback() {

    // хороший тутор по возможностям helper'a
    // https://codeburst.io/android-swipe-menu-with-recyclerview-8f28a235ff28
    // а вот неплохая реализация Swipe delete
    // https://medium.com/@kitek/recyclerview-swipe-to-delete-easier-than-you-thought-cff67ff5e5f6

    private val intrinsicWidth = deleteIconDrawable.intrinsicWidth
    private val intrinsicHeight = deleteIconDrawable.intrinsicHeight
    private val background = ColorDrawable()
    private val clearPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
    private var dragAction: Job? = null

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        return if (excludedViewHolderTypes.contains(viewHolder::class)) {
            makeMovementFlags(0, 0)
        } else {
            val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN or
                    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            makeMovementFlags(
                dragFlags,
                ItemTouchHelper.START or ItemTouchHelper.LEFT
            )
        }
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = viewHolder is SubscriptionsListAdapter.SubscriptionItemViewHolder && target is SubscriptionsListAdapter.SubscriptionFolderViewHolder

    override fun onMoved(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, fromPos: Int, target: RecyclerView.ViewHolder, toPos: Int, x: Int, y: Int) {
        super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y)
        dragAction?.cancel()
        dragAction = coroutineScope.launch {
            delay(250L)
            listener.onDragHolderToPosition(viewHolder.adapterPosition, target.adapterPosition)
        }
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = listener.onItemDismiss(viewHolder.adapterPosition)

    override fun isLongPressDragEnabled(): Boolean = true

    override fun isItemViewSwipeEnabled(): Boolean = true

    override fun onChildDraw(
        canvas: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (actionState == ACTION_STATE_DRAG) {
            super.onChildDraw(
                canvas,
                recyclerView,
                viewHolder,
                dX,
                dY,
                actionState,
                isCurrentlyActive
            )
            return
        }
        val itemView = viewHolder.itemView
        val isCanceled = dX == 0f && !isCurrentlyActive

        if (isCanceled) {
            clearCanvas(
                canvas,
                itemView.right + dX,
                itemView.top.toFloat(),
                itemView.right.toFloat(),
                itemView.bottom.toFloat()
            )
            super.onChildDraw(
                canvas,
                recyclerView,
                viewHolder,
                dX,
                dY,
                actionState,
                isCurrentlyActive
            )
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

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
//        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
//            if (viewHolder is ItemTouchHelperViewHolder) {
//                val itemViewHolder: ItemTouchHelperViewHolder =
//                    viewHolder as ItemTouchHelperViewHolder
//                itemViewHolder.onItemSelected()
//            }
//        }
        super.onSelectedChanged(viewHolder, actionState)
        if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
            dragAction?.cancel()
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        dragAction?.cancel()
    }

    private fun clearCanvas(c: Canvas?, left: Float, top: Float, right: Float, bottom: Float) {
        c?.drawRect(left, top, right, bottom, clearPaint)
    }

    interface TouchListener {
        fun onItemDismiss(position: Int)
        fun onDragHolderToPosition(holderPosition: Int, targetPosition: Int)
    }
}