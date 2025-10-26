package com.example.tamaade.presentation.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.tamaade.R
import com.example.tamaade.data.model.Address

class AddressAdapter(
    private val addressList: ArrayList<Address>,
    private val context: Context,
    private val onAddressClick: (Address) -> Unit
) : RecyclerView.Adapter<AddressAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_address, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val address = addressList[position]

        holder.fullNameTv.text = address.fullName
        holder.phoneTv.text = address.phone
        holder.addressTv.text = address.address
        holder.cityTv.text = address.city
        holder.typeTv.text = address.type

        // Set type icon
        when (address.type) {
            "Home" -> {
                holder.typeIcon.setImageResource(R.drawable.ic_home)
                holder.typeIcon.setColorFilter(ContextCompat.getColor(context, R.color.primary))
            }
            "Trotro Station" -> {
                holder.typeIcon.setImageResource(R.drawable.ic_baseline_location_on_24)
                holder.typeIcon.setColorFilter(ContextCompat.getColor(context, R.color.success))
            }
        }

        // Set selection state
        if (address.isSelected) {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.primary))
            holder.fullNameTv.setTextColor(ContextCompat.getColor(context, R.color.white))
            holder.phoneTv.setTextColor(ContextCompat.getColor(context, R.color.white))
            holder.addressTv.setTextColor(ContextCompat.getColor(context, R.color.white))
            holder.cityTv.setTextColor(ContextCompat.getColor(context, R.color.white))
            holder.typeTv.setTextColor(ContextCompat.getColor(context, R.color.white))
            holder.typeIcon.setColorFilter(ContextCompat.getColor(context, R.color.white))
            holder.selectedIcon.visibility = View.VISIBLE
        } else {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.backgroundAd))
            holder.fullNameTv.setTextColor(ContextCompat.getColor(context, R.color.mainText))
            holder.phoneTv.setTextColor(ContextCompat.getColor(context, R.color.textAd))
            holder.addressTv.setTextColor(ContextCompat.getColor(context, R.color.mainText))
            holder.cityTv.setTextColor(ContextCompat.getColor(context, R.color.textAd))
            holder.typeTv.setTextColor(ContextCompat.getColor(context, R.color.primary))
            holder.selectedIcon.visibility = View.GONE
        }

        holder.cardView.setOnClickListener {
            onAddressClick(address)
        }
    }

    override fun getItemCount(): Int = addressList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.addressCardView)
        val typeIcon: ImageView = itemView.findViewById(R.id.typeIcon)
        val selectedIcon: ImageView = itemView.findViewById(R.id.selectedIcon)
        val fullNameTv: TextView = itemView.findViewById(R.id.fullNameTv)
        val phoneTv: TextView = itemView.findViewById(R.id.phoneTv)
        val addressTv: TextView = itemView.findViewById(R.id.addressTv)
        val cityTv: TextView = itemView.findViewById(R.id.cityTv)
        val typeTv: TextView = itemView.findViewById(R.id.typeTv)
    }
}