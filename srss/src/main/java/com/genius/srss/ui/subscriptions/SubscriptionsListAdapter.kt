package com.genius.srss.ui.subscriptions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.genius.srss.R
import com.genius.srss.databinding.RvSubscriptionFolderItemBinding
import com.genius.srss.databinding.RvSubscriptionItemBinding
import com.ub.utils.base.BaseListAdapter

class SubscriptionsListAdapter : BaseListAdapter<BaseSubscriptionModel, RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        return getItem(position).getLayoutId()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.layout.rv_subscription_item -> SubscriptionItemViewHolder(RvSubscriptionItemBinding.inflate(inflater, parent, false))
            R.layout.rv_subscription_folder_item -> SubscriptionFolderViewHolder(RvSubscriptionFolderItemBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Unknown view type for inflate: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SubscriptionItemViewHolder) {
            holder.bind(getItem(position) as SubscriptionItemModel)
        } else if (holder is SubscriptionFolderViewHolder) {
            holder.bind(getItem(position) as SubscriptionFolderItemModel)
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

    inner class SubscriptionFolderViewHolder(binding: RvSubscriptionFolderItemBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        private val folderName: TextView = binding.folderName
        private val folderCount: TextView = binding.folderCount

        fun bind(model: SubscriptionFolderItemModel) {
            folderName.text = model.name
            folderCount.text = String.format(
                itemView.context.resources.getQuantityString(R.plurals.subscriptions_folder_count_template, model.countOtOfSources, model.countOtOfSources)
            )
        }

        override fun onClick(v: View) {
            val position = adapterPosition
            listListener?.onClick(v, getItem(position), position)
        }
    }
}