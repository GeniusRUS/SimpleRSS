package com.genius.srss.ui.subscriptions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.clear
import coil.load
import com.genius.srss.databinding.RvSubscriptionItemBinding
import com.ub.utils.base.BaseListAdapter

class SubscriptionsListAdapter : BaseListAdapter<SubscriptionItemModel, RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return SubscriptionItemViewHolder(RvSubscriptionItemBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SubscriptionItemViewHolder) {
            holder.bind(getItem(position))
        }
    }

    inner class SubscriptionItemViewHolder(binding: RvSubscriptionItemBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        private val feedName: TextView = binding.feedName
        private val subscriptionContent: LinearLayout = binding.subscriptionContent

        init {
            subscriptionContent.setOnClickListener(this)
        }

        fun bind(model: SubscriptionItemModel) {
            feedName.text = model.title
        }

        override fun onClick(v: View) {
            val position = adapterPosition
            listListener?.onClick(v, getItem(position), position)
        }
    }
}