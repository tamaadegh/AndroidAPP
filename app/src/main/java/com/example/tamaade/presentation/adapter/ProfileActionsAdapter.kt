package com.example.tamaade.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tamaade.databinding.ItemProfileActionBinding

data class ProfileAction(val iconResId: Int, val title: String)

class ProfileActionsAdapter(private val actions: List<ProfileAction>) :
    RecyclerView.Adapter<ProfileActionsAdapter.ActionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionViewHolder {
        val binding =
            ItemProfileActionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ActionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ActionViewHolder, position: Int) {
        holder.bind(actions[position])
    }

    override fun getItemCount(): Int = actions.size

    inner class ActionViewHolder(private val binding: ItemProfileActionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(action: ProfileAction) {
            binding.actionIcon.setImageResource(action.iconResId)
            binding.actionTitle.text = action.title
        }
    }
}