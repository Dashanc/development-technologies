package com.example.mobile.ui.health

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mobile.R
import com.example.mobile.domain.model.VaccinationRecord
import com.example.mobile.domain.model.VetVisitRecord
import com.example.mobile.domain.model.WeightRecord
import com.example.mobile.presenter.health.MedicalHistoryItem

internal class MedicalHistoryAdapter : ListAdapter<MedicalHistoryItem, MedicalHistoryAdapter.Holder>(object : DiffUtil.ItemCallback<MedicalHistoryItem>() {
    override fun areItemsTheSame(oldItem: MedicalHistoryItem, newItem: MedicalHistoryItem) = oldItem.record.id == newItem.record.id
    override fun areContentsTheSame(oldItem: MedicalHistoryItem, newItem: MedicalHistoryItem) = oldItem == newItem
}) {
    internal class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.record_icon)
        val title: TextView = view.findViewById(R.id.record_title)
        val description: TextView = view.findViewById(R.id.record_description)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_health_record, parent, false),
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        val context = holder.itemView.context
        val text = MedicalText(context)
        holder.title.text = context.getString(R.string.pet_detail_join, item.petName, text.title(item.record))
        holder.description.text = text.description(item.record)
        holder.icon.setImageResource(when (item.record) {
            is WeightRecord -> R.drawable.ic_weight
            is VaccinationRecord -> R.drawable.ic_vaccination
            is VetVisitRecord -> R.drawable.ic_health
        })
    }
}