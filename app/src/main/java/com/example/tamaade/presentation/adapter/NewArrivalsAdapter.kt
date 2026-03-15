package com.example.tamaade.presentation.adapter

import android.content.Intent
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tamaade.R
import com.example.tamaade.data.model.Product
import com.example.tamaade.databinding.ItemProductCardBinding
import com.example.tamaade.presentation.activity.ProductDetailActivity
import com.example.tamaade.presentation.activity.SignUpActivity
import com.google.firebase.auth.FirebaseAuth

class NewArrivalsAdapter : ListAdapter<Product, NewArrivalsAdapter.ProductViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding =
            ItemProductCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = getItem(position)
        holder.bind(product)
        
        // Single Click: Open Details (No Auth Required)
        holder.itemView.setOnClickListener {
            val intent = Intent(it.context, ProductDetailActivity::class.java).apply {
                putExtra(ProductDetailActivity.PRODUCT_SLUG_EXTRA, product.slug)
            }
            it.context.startActivity(intent)
        }

        // Long Click: Like / Quick Action (Auth Required)
        holder.itemView.setOnLongClickListener {
            if (isUserSignedIn()) {
                // Logic to like the item
                Toast.makeText(it.context, "Liked ${product.productName}!", Toast.LENGTH_SHORT).show()
                // Ideally toggle heart icon visually
            } else {
                redirectToSignUp(it.context)
            }
            true // Consumed
        }

        // Add to Cart Click (Auth Required)
        holder.binding.btnAdd.setOnClickListener {
            if (isUserSignedIn()) {
                // Logic to add to cart
                Toast.makeText(it.context, "Added ${product.productName} to cart", Toast.LENGTH_SHORT).show()
            } else {
                redirectToSignUp(it.context)
            }
        }
    }

    private fun isUserSignedIn(): Boolean {
        // Use Firebase directly or a util wrapper
        return FirebaseAuth.getInstance().currentUser != null
    }

    private fun redirectToSignUp(context: android.content.Context) {
        Toast.makeText(context, "Please sign in to continue", Toast.LENGTH_SHORT).show()
        val intent = Intent(context, SignUpActivity::class.java)
        context.startActivity(intent)
    }


    inner class ProductViewHolder(val binding: ItemProductCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.tvProductName.text = product.productName
            binding.tvProductPrice.text = "GHS ${product.productPrice}"
            
            // robust dummy logic for old price (20% markup)
            val currentPrice = product.productPrice.toDoubleOrNull() ?: 0.0
            val oldPrice = currentPrice * 1.2
            binding.tvOldPrice.text = String.format("GHS %.2f", oldPrice)
            binding.tvOldPrice.paintFlags = binding.tvOldPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

            Glide.with(itemView.context)
                .load(product.productImage)
                .into(binding.ivProductImage)
        }
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }
}
