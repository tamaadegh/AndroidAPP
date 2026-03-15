package com.example.tamaade.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tamaade.data.network.ImageDto
import com.example.tamaade.databinding.ItemImageSliderBinding

class ImageSliderAdapter(private val images: List<ImageDto>) : RecyclerView.Adapter<ImageSliderAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding =
            ItemImageSliderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int = images.size

    inner class ImageViewHolder(private val binding: ItemImageSliderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(image: ImageDto) {
            Glide.with(itemView.context)
                .load(image.image)
                .into(binding.imageView)
        }
    }
}
