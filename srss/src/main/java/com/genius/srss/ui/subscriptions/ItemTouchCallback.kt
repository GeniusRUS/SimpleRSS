package com.genius.srss.ui.subscriptions

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_SWIPE
import androidx.recyclerview.widget.RecyclerView
import com.genius.srss.R
import kotlin.reflect.KClass

class ItemTouchCallback(
    private val recyclerView: RecyclerView,
    private val listener: TouchListener,
    private val deleteIconDrawable: Drawable,
    @ColorInt
    private val backgroundColor: Int,
    private val excludedViewHolderTypes: List<KClass<out RecyclerView.ViewHolder>> = listOf()
) : ItemTouchHelper.Callback() {

    // хороший тутор по возможностям helper'a
    // https://codeburst.io/android-swipe-menu-with-recyclerview-8f28a235ff28
    // а вот неплохая реализация Swipe delete
    // https://medium.com/@kitek/recyclerview-swipe-to-delete-easier-than-you-thought-cff67ff5e5f6
    // drag-and-drop функционал с останановкой like a folder
    // https://stackoverflow.com/questions/47431169/recyclerview-drag-and-drop-like-file-folder

    private val intrinsicWidth = deleteIconDrawable.intrinsicWidth
    private val intrinsicHeight = deleteIconDrawable.intrinsicHeight
    private val background = ColorDrawable()
    private val clearPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
    private var itemPosition: Int = RecyclerView.NO_POSITION
    private var folder: View? = null

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
    ): Boolean {
//        viewHolder is SubscriptionsListAdapter.SubscriptionItemViewHolder && target is SubscriptionsListAdapter.SubscriptionFolderViewHolder
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = listener.onItemDismiss(viewHolder.absoluteAdapterPosition)

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
        if (actionState == ACTION_STATE_DRAG && isCurrentlyActive) {
            if (folder != null) {
                folder?.setBackgroundResource(0)
                folder = null
            }
            if (itemPosition != RecyclerView.NO_POSITION) {
                itemPosition = RecyclerView.NO_POSITION
            }
            val itemActualPositionY = viewHolder.itemView.top + dY + viewHolder.itemView.height / 2f
            val itemActualPositionX = viewHolder.itemView.left + dX + viewHolder.itemView.width / 2f

            for (i in 0 until recyclerView.childCount) {
                val child = recyclerView.getChildAt(i)
                if (child != viewHolder.itemView) {
                    if (excludedViewHolderTypes.contains(recyclerView.getChildViewHolder(child)::class)) {
                        if (child.top < itemActualPositionY
                            && itemActualPositionY < child.bottom
                            && child.left < itemActualPositionX
                            && itemActualPositionX < child.right
                        ) {
                            folder = child
                            itemPosition = viewHolder.absoluteAdapterPosition

                            folder?.background = ResourcesCompat.getDrawable(
                                recyclerView.context.resources,
                                R.drawable.shape_default_background_border,
                                recyclerView.context.theme
                            )
                            break
                        }
                    }
                }
            }

            super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }
        val itemView = viewHolder.itemView
        val isSwipeCanceled = dX == 0f && !isCurrentlyActive

        if (isSwipeCanceled) {
            canvas.drawRect(
                itemView.right + dX,
                itemView.top.toFloat(),
                itemView.right.toFloat(),
                itemView.bottom.toFloat(),
                clearPaint
            )
        } else if (actionState == ACTION_STATE_SWIPE && isCurrentlyActive) {
            background.color = backgroundColor
            background.setBounds(
                itemView.right + dX.toInt(),
                itemView.top,
                itemView.right,
                itemView.bottom
            )
            background.draw(canvas)

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
        }

        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        if (actionState == ACTION_STATE_DRAG) {
            if (folder != null) {
                folder?.setBackgroundResource(0)
                folder = null
            }
            if (itemPosition != RecyclerView.NO_POSITION) {
                itemPosition = RecyclerView.NO_POSITION
            }
        } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
            if (folder != null) {
                folder?.setBackgroundResource(0)
                recyclerView.getChildViewHolder(recyclerView.getChildAt(itemPosition)).setIsRecyclable(true)
                listener.onDragHolderToPosition(
                    itemPosition,
                    recyclerView.getChildAdapterPosition(folder ?: return)
                )
            }
        }
    }

    interface TouchListener {
        fun onItemDismiss(position: Int)
        fun onDragHolderToPosition(holderPosition: Int, targetPosition: Int)
    }
}