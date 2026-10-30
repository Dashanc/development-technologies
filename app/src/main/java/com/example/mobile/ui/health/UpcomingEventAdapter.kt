package com.example.mobile.ui.health

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mobile.R
import com.example.mobile.presenter.health.UpcomingHealthEvent

internal class UpcomingEventAdapter(private val openHistory: (String) -> Unit) :
    ListAdapter<UpcomingHealthEvent, UpcomingEventAdapter.Holder>(object : DiffUtil.ItemCallback<UpcomingHealthEvent>() {
        override fun areItemsTheSame(oldItem: UpcomingHealthEvent, newItem: UpcomingHealthEvent) =
            oldItem.recordId == newItem.recordId && oldItem.type == newItem.type
        override fun areContentsTheSame(oldItem: UpcomingHealthEvent, newItem: UpcomingHealthEvent) = oldItem == newItem
    }) {
    internal class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.event_title)
        val date: TextView = view.findViewById(R.id.event_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_upcoming_event, parent, false),
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val event = getItem(position)
        val context = holder.itemView.context
        val text = MedicalText(context)
        holder.title.text = context.getString(R.string.pet_detail_join, event.petName, text.eventTitle(event))
        holder.date.text = text.eventDescription(event)
        holder.itemView.setOnClickListener { openHistory(event.petId) }
    }
}