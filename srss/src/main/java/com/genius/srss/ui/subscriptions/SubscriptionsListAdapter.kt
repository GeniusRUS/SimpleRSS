package com.genius.srss.ui.subscriptions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.genius.srss.R
import com.genius.srss.databinding.RvSubscriptionFolderItemBinding
import com.genius.srss.databinding.RvSubscriptionItemBinding
import com.ub.utils.base.BaseListAdapter
import android.view.DragEvent
import android.os.Build.VERSION_CODES

import android.os.Build.VERSION

import android.view.View.DragShadowBuilder

import android.content.ClipData

import android.content.ClipDescription
import android.widget.*
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.genius.srss.databinding.RvFeedEmptyBinding
import com.genius.srss.ui.feed.FeedEmptyModel

class SubscriptionsListAdapter : BaseListAdapter<BaseSubscriptionModel, RecyclerView.ViewHolder>() {

    var longTouchListener: SubscriptionsItemTouchCallback.TouchListener? = null

    override fun getItemViewType(position: Int): Int {
        return getItem(position).layoutId
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.layout.rv_subscription_item -> SubscriptionItemViewHolder(RvSubscriptionItemBinding.inflate(inflater, parent, false))
            R.layout.rv_subscription_folder_item -> SubscriptionFolderViewHolder(RvSubscriptionFolderItemBinding.inflate(inflater, parent, false))
            R.layout.rv_feed_empty -> SubscriptionFolderEmptyViewHolder(RvFeedEmptyBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Unknown view type for inflate: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SubscriptionItemViewHolder) {
            holder.bind(getItem(position) as SubscriptionItemModel)
        } else if (holder is SubscriptionFolderViewHolder) {
            holder.bind(getItem(position) as SubscriptionFolderItemModel)
        } else if (holder is SubscriptionFolderEmptyViewHolder) {
            holder.bind(getItem(position) as SubscriptionFolderEmptyModel)
        }
    }

    inner class SubscriptionItemViewHolder(binding: RvSubscriptionItemBinding) : RecyclerView.ViewHolder(binding.root),
        View.OnClickListener, View.OnLongClickListener {

        private val feedName: TextView = binding.feedName
        private val subscriptionContent: LinearLayout = binding.subscriptionContent

        init {
            subscriptionContent.setOnClickListener(this)
            if (longTouchListener != null) {
                subscriptionContent.setOnLongClickListener(this)
            }
        }

        fun bind(model: SubscriptionItemModel) {
            feedName.text = model.title
            subscriptionContent.tag = model.urlToLoad
        }

        override fun onClick(v: View) {
            val position = absoluteAdapterPosition
            listListener?.onClick(v, getItem(position), position)
        }

        @Suppress("DEPRECATION")
        override fun onLongClick(view: View): Boolean {
            val position = ClipData.Item(absoluteAdapterPosition.toString())
            val mimeTypes = arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN)
            val dragData = ClipData(
                view.tag.toString(),
                mimeTypes, position
            )
            val myShadow = DragShadowBuilder(view)

            if (VERSION.SDK_INT >= VERSION_CODES.N) {
                view.startDragAndDrop(dragData, myShadow, null, 0)
            } else {
                view.startDrag(dragData, myShadow, null, 0)
            }
            return true
        }
    }

    inner class SubscriptionFolderViewHolder(binding: RvSubscriptionFolderItemBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        private val folderRoot: FrameLayout = binding.folderRoot
        private val folderName: TextView = binding.folderName
        private val folderCount: TextView = binding.folderCount
        private val folderContent: LinearLayout = binding.folderContent

        init {
            folderContent.setOnClickListener(this)
            folderContent.setOnDragListener { _, event ->
                return@setOnDragListener when (event.action) {
                    DragEvent.ACTION_DRAG_STARTED -> {                     // drag has started, return true to tell that you're listening to the drag
                        true
                    }
                    DragEvent.ACTION_DROP -> {
                        val position = event.clipData.getItemAt(0).text.toString().toInt()
                        // the dragged item was dropped into this view
                        longTouchListener?.onDragHolderToPosition(
                            holderPosition = position,
                            targetPosition = absoluteAdapterPosition
                        )
                        folderRoot.setBackgroundResource(0)
                        true
                    }
                    DragEvent.ACTION_DRAG_ENDED ->                     // the drag has ended
                        false
                    DragEvent.ACTION_DRAG_ENTERED -> {
                        folderRoot.setBackgroundResource(R.drawable.shape_default_background_border)
                        false
                    }
                    DragEvent.ACTION_DRAG_EXITED -> {
                        folderRoot.setBackgroundResource(0)
                        false
                    }
                    else -> false
                }
            }
        }

        fun bind(model: SubscriptionFolderItemModel) {
            folderName.text = model.name
            folderCount.text = String.format(
                itemView.context.resources.getQuantityString(R.plurals.subscriptions_folder_count_template, model.countOtOfSources, model.countOtOfSources)
            )
        }

        override fun onClick(v: View) {
            val position = absoluteAdapterPosition
            listListener?.onClick(v, getItem(position), position)
        }
    }

    inner class SubscriptionFolderEmptyViewHolder(binding: RvFeedEmptyBinding) : RecyclerView.ViewHolder(binding.root),
        View.OnClickListener {

        private val iconImage: ImageView = binding.icon
        private val reasonText: TextView = binding.message
        private val action: Button = binding.action

        init {
            action.setOnClickListener(this)
        }

        fun bind(model: SubscriptionFolderEmptyModel) {
            iconImage.setImageDrawable(VectorDrawableCompat.create(itemView.context.resources, model.icon, itemView.context.theme))
            reasonText.text = model.message
            if (model.action.isNullOrEmpty()) {
                action.isGone = true
            } else {
                action.isVisible = true
                action.text = model.action
            }
        }

        override fun onClick(v: View) {
            val position = absoluteAdapterPosition
            listListener?.onClick(v, getItem(position), position)
        }
    }
}