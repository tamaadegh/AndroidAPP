package com.example.tamaade.ui.products

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tamaade.R
import com.example.tamaade.data.remote.model.Product

class ProductAdapter(private var products: List<Product>) : 
    RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.product_image)
        val titleView: TextView = itemView.findViewById(R.id.product_title)
        val priceView: TextView = itemView.findViewById(R.id.product_price)
        val brandView: TextView = itemView.findViewById(R.id.product_brand)
        val ratingBar: RatingBar = itemView.findViewById(R.id.product_rating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.titleView.text = product.name // Use 'name' from the updated data class
        holder.brandView.text = product.category // Use 'category' as the brand
        holder.priceView.text = "$${product.price}" // Price is now a string
        // The API does not provide a rating, so we'll use a static value for the UI
        holder.ratingBar.rating = 4.5f

        if (!product.image.isNullOrEmpty()) {
            holder.imageView.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(product.image)
                .into(holder.imageView)
        } else {
            holder.imageView.visibility = View.INVISIBLE // Hide if no image
        }
    }

    override fun getItemCount(): Int = products.size

    fun updateProducts(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }
}
