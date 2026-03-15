package com.example.tamaade.presentation.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tamaade.data.model.Product
import com.example.tamaade.databinding.ItemProductBinding
import com.example.tamaade.presentation.activity.ProductDetailActivity
import com.example.tamaade.presentation.activity.SignUpActivity
import com.google.firebase.auth.FirebaseAuth

class ProductAdapter : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    var onItemClick: ((Product) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding =
            ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = getItem(position)
        holder.bind(product)
        
        // Single Click: Details (No Auth)
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(product) ?: run {
                val intent = Intent(it.context, ProductDetailActivity::class.java).apply {
                    putExtra(ProductDetailActivity.PRODUCT_SLUG_EXTRA, product.slug)
                }
                it.context.startActivity(intent)
            }
        }

        // Long Click: Like (Auth Required)
        holder.itemView.setOnLongClickListener {
            if (isUserSignedIn()) {
                Toast.makeText(it.context, "Liked ${product.productName}!", Toast.LENGTH_SHORT).show()
                // Logic to call API or update UI
            } else {
                redirectToSignUp(it.context)
            }
            true
        }
    }

    private fun isUserSignedIn(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
    }

    private fun redirectToSignUp(context: android.content.Context) {
        Toast.makeText(context, "Please sign in to continue", Toast.LENGTH_SHORT).show()
        val intent = Intent(context, SignUpActivity::class.java)
        context.startActivity(intent)
    }

    inner class ProductViewHolder(private val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.productName.text = product.productName
            binding.productPrice.text = "GHS ${product.productPrice}"

            Glide.with(itemView.context)
                .load(product.productImage)
                .centerCrop() // Improved image loading
                .into(binding.productImage)
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
