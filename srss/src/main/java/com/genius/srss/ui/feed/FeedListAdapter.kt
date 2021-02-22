package com.genius.srss.ui.feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.clear
import coil.load
import coil.size.Scale
import com.genius.srss.R
import com.genius.srss.databinding.RvFeedItemBinding
import com.genius.srss.di.services.converters.IConverters
import com.ub.utils.base.BaseListAdapter

class FeedListAdapter(
    private val converters: IConverters
) : BaseListAdapter<FeedItemModel, RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return FeedItemViewHolder(RvFeedItemBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is FeedItemViewHolder) {
            holder.bind(getItem(position))
        }
    }

    inner class FeedItemViewHolder(binding: RvFeedItemBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

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
            if (model.pictureUrl != null) {
                articleImage.isVisible = true
                articleImage.load(model.pictureUrl) {
                    scale(Scale.FIT)
                    placeholder(R.drawable.layer_list_image_placeholder)
                    error(R.drawable.layer_list_image_placeholder)
                }
            } else {
                articleImage.isGone = true
                articleImage.clear()
            }
            if (model.publicationDate != null) {
                publicationDate.isVisible = true
                publicationDate.text = converters.formatDateToString(model.publicationDate)
            } else {
                publicationDate.isGone = true
            }
        }

        override fun onClick(v: View) {
            val position = adapterPosition
            listListener?.onClick(v, getItem(position), position)
        }
    }
}