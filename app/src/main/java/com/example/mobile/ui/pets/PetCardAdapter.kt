package com.example.mobile.ui.pets

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mobile.presenter.pets.PetCard
import com.example.mobile.R

internal class PetCardAdapter(
    private val photos: PetAvatarLoader,
    private val ownerCards: Boolean = false,
    private val openPet: (String) -> Unit,
) : ListAdapter<PetCard, PetCardAdapter.Holder>(object : DiffUtil.ItemCallback<PetCard>() {
    override fun areItemsTheSame(oldItem: PetCard, newItem: PetCard) = oldItem.pet.id == newItem.pet.id
    override fun areContentsTheSame(oldItem: PetCard, newItem: PetCard) = oldItem == newItem
}) {
    internal class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: ImageView = view.findViewById(R.id.pet_avatar)
        val name: TextView = view.findViewById(R.id.pet_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        LayoutInflater.from(parent.context).inflate(if (ownerCards) R.layout.item_owner_pet else R.layout.item_pet, parent, false),
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val card = getItem(position)
        val text = PetText(holder.itemView.context)
        holder.name.text = card.pet.name
        photos.show(holder.avatar, card.pet.photoUri, card.pet.species)
        holder.itemView.setOnClickListener { openPet(card.pet.id) }
        if (ownerCards) holder.itemView.findViewById<TextView>(R.id.pet_age).text = text.age(card.age)
        else {
            holder.itemView.findViewById<TextView>(R.id.pet_details).text = text.description(card)
            holder.itemView.findViewById<TextView>(R.id.pet_status).setText(R.string.no_health_records)
            holder.itemView.findViewById<TextView>(R.id.pet_weight).setText(R.string.pet_no_value)
            holder.itemView.findViewById<TextView>(R.id.pet_next_event).setText(R.string.pet_no_value)
        }
    }

    override fun onViewRecycled(holder: Holder) {
        photos.clear(holder.avatar)
        super.onViewRecycled(holder)
    }
}