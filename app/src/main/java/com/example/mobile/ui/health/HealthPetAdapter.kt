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
import com.example.mobile.presenter.health.PetHealthSummary
import com.example.mobile.ui.pets.PetAvatarLoader

internal class HealthPetAdapter(private val photos: PetAvatarLoader, private val openProfile: (String) -> Unit) :
    ListAdapter<PetHealthSummary, HealthPetAdapter.Holder>(object : DiffUtil.ItemCallback<PetHealthSummary>() {
        override fun areItemsTheSame(oldItem: PetHealthSummary, newItem: PetHealthSummary) = oldItem.pet.id == newItem.pet.id
        override fun areContentsTheSame(oldItem: PetHealthSummary, newItem: PetHealthSummary) = oldItem == newItem
    }) {
    internal class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: ImageView = view.findViewById(R.id.pet_avatar)
        val name: TextView = view.findViewById(R.id.pet_name)
        val summary: TextView = view.findViewById(R.id.pet_health_summary)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_health_pet, parent, false),
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        holder.name.text = item.pet.name
        holder.summary.text = MedicalText(holder.itemView.context).petSummary(item)
        photos.show(holder.avatar, item.pet.photoUri, item.pet.species)
        holder.itemView.setOnClickListener { openProfile(item.pet.id) }
    }

    override fun onViewRecycled(holder: Holder) {
        photos.clear(holder.avatar)
        super.onViewRecycled(holder)
    }
}