package com.ekku.nfc.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ekku.nfc.R
import com.ekku.nfc.model.Tag
import com.ekku.nfc.model.TagAPI

class TagListAdapter : ListAdapter<TagAPI, TagListAdapter.TagViewHolder>(TagsComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        return TagViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current)
    }

    class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tagItemView: TextView = itemView.findViewById(R.id.textView)

        fun bind(tag: TagAPI?) {
            "${tag?.id}, ${tag?.tag_uid}, ${tag?.tag_date_time}, ${tag?.tag_phone_uid}, ${tag?.tag_sync}".also { tagItemView.text = it }
        }

        companion object {
            fun create(parent: ViewGroup): TagViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.recyclerview_item, parent, false)
                return TagViewHolder(view)
            }
        }
    }

    class TagsComparator : DiffUtil.ItemCallback<TagAPI>() {
        override fun areItemsTheSame(oldItem: TagAPI, newItem: TagAPI): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: TagAPI, newItem: TagAPI): Boolean {
            return oldItem.tag_uid == newItem.tag_uid
        }
    }

}