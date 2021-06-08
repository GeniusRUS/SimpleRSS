package com.genius.srss.ui.feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import coil.clear
import coil.load
import coil.size.Scale
import com.genius.srss.R
import com.genius.srss.databinding.RvFeedEmptyBinding
import com.genius.srss.databinding.RvFeedItemBinding
import com.genius.srss.di.services.converters.IConverters
import com.ub.utils.base.BaseListAdapter

class FeedListAdapter(
    private val converters: IConverters
) : BaseListAdapter<FeedModels, RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        return getItem(position).layoutId
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.layout.rv_feed_item -> FeedItemViewHolder(RvFeedItemBinding.inflate(inflater, parent, false))
            R.layout.rv_feed_empty -> FeedEmptyViewHolder(RvFeedEmptyBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Unknown view type for inflate: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is FeedItemViewHolder) {
            holder.bind(getItem(position) as FeedItemModel)
        } else if (holder is FeedEmptyViewHolder) {
            holder.bind(getItem(position) as FeedEmptyModel)
        }
    }

    inner class FeedItemViewHolder(binding: RvFeedItemBinding) : RecyclerView.ViewHolder(binding.root),
        View.OnClickListener {

        private val articleTitle: TextView = binding.articleTitle
        private val articleImage: ImageView = binding.image
        private val publicationDate: TextView = binding.publicationDate

        init {
            binding.feedContent.setOnClickListener(this)
        }

        fun bind(model: FeedItemModel) {
            if (model.title != null) {
                articleTitle.isVisible = true
                articleTitle.text = HtmlCompat.fromHtml(model.title, HtmlCompat.FROM_HTML_MODE_LEGACY)
            } else {
                articleTitle.isGone = true
            }
            articleImage.clear()
            articleImage.load(model.pictureUrl) {
                scale(Scale.FIT)
                crossfade(true)
                placeholder(R.drawable.layer_list_image_placeholder)
                fallback(R.drawable.layer_list_image_placeholder)
                error(R.drawable.layer_list_image_placeholder)
            }
            if (model.publicationDate != null) {
                publicationDate.isVisible = true
                publicationDate.text = converters.formatDateToString(model.publicationDate)
            } else {
                publicationDate.isGone = true
            }
        }

        override fun onClick(v: View) {
            val position = absoluteAdapterPosition
            listListener?.onClick(v, getItem(position), position)
        }
    }

    inner class FeedEmptyViewHolder(binding: RvFeedEmptyBinding) : RecyclerView.ViewHolder(binding.root),
        View.OnClickListener {

        private val iconImage: ImageView = binding.icon
        private val reasonText: TextView = binding.message
        private val action: Button = binding.action

        init {
            action.setOnClickListener(this)
        }

        fun bind(model: FeedEmptyModel) {
            iconImage.setImageDrawable(VectorDrawableCompat.create(itemView.context.resources, model.icon, itemView.context.theme))
            reasonText.text = model.message
            if (model.actionText.isNullOrEmpty()) {
                action.isGone = true
            } else {
                action.isVisible = true
                action.text = model.actionText
            }
        }

        override fun onClick(v: View) {
            val position = absoluteAdapterPosition
            listListener?.onClick(v, getItem(position), position)
        }
    }
}