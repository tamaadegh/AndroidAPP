package com.example.tamaade.presentation.adapter


import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tamaade.data.model.Product
import com.example.tamaade.presentation.activity.ProductDetailsActivity
import com.example.tamaade.R
import com.example.tamaade.R.drawable.*

class ProductAdapter(private val productList: ArrayList<Product>, context: Context): RecyclerView.Adapter<ProductAdapter.ViewHolder>()  {

    val ctx: Context = context
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
    private val favoriteItems = mutableSetOf<String>()

    init {
        loadFavorites()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val productView = LayoutInflater.from(parent.context).inflate(R.layout.single_product,parent,false)
        return ViewHolder(productView)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val product: Product = productList[position]
        holder.productBrandName_singleProduct.text = product.productBrand
        holder.productName_singleProduct.text = product.productName
        holder.productPrice_singleProduct.text = "$"+product.productPrice
        holder.productRating_singleProduct.rating = product.productRating

        Glide.with(ctx)
            .load(product.productImage)
            .placeholder(bn)
            .into(holder.productImage_singleProduct)


        if(product.productHave == true){
            holder.discountTv_singleProduct.text = product.productDisCount
            holder.discount_singleProduct.visibility = View.VISIBLE
        }

        if(product.productHave == false){

            holder.discount_singleProduct.visibility = View.VISIBLE
            holder.discountTv_singleProduct.text = "New"

        }

        // Set favorite button state
        val productId = product.productName + "_" + product.productBrand
        val isFavorite = favoriteItems.contains(productId)
        updateFavoriteButton(holder.productAddToFav_singleProduct, isFavorite)

        // Handle favorite button click
        holder.productAddToFav_singleProduct.setOnClickListener {
            toggleFavorite(productId, holder.productAddToFav_singleProduct)
        }

        holder.itemView.setOnClickListener {
            goDetailsPage(position)
        }

    }

    override fun getItemCount(): Int {
         return productList.size
    }

    class ViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){

        val productImage_singleProduct:ImageView = itemView.findViewById(R.id.productImage_singleProduct)
        val productAddToFav_singleProduct:ImageView = itemView.findViewById(R.id.productAddToFav_singleProduct)
        val productRating_singleProduct:RatingBar = itemView.findViewById(R.id.productRating_singleProduct)
        val productBrandName_singleProduct:TextView = itemView.findViewById(R.id.productBrandName_singleProduct)
        val discountTv_singleProduct:TextView = itemView.findViewById(R.id.discountTv_singleProduct)
        val productName_singleProduct:TextView = itemView.findViewById(R.id.productName_singleProduct)
        val productPrice_singleProduct:TextView = itemView.findViewById(R.id.productPrice_singleProduct)
        val discount_singleProduct = itemView.findViewById<LinearLayout>(R.id.discount_singleProduct)


    }

    private fun goDetailsPage(position: Int) {
        val intent = Intent(ctx , ProductDetailsActivity::class.java)
        intent.putExtra("ProductIndex", position)
        intent.putExtra("ProductFrom", "New")
        ctx.startActivity(intent)
    }

    private fun loadFavorites() {
        val favoritesSet = sharedPreferences.getStringSet("favorite_products", emptySet()) ?: emptySet()
        favoriteItems.clear()
        favoriteItems.addAll(favoritesSet)
    }

    private fun saveFavorites() {
        sharedPreferences.edit()
            .putStringSet("favorite_products", favoriteItems)
            .apply()
    }

    private fun toggleFavorite(productId: String, favoriteButton: ImageView) {
        val isFavorite = favoriteItems.contains(productId)

        if (isFavorite) {
            favoriteItems.remove(productId)
            animateFavoriteButton(favoriteButton, false)
            Toast.makeText(ctx, "Removed from favorites", Toast.LENGTH_SHORT).show()
        } else {
            favoriteItems.add(productId)
            animateFavoriteButton(favoriteButton, true)
            Toast.makeText(ctx, "Added to favorites", Toast.LENGTH_SHORT).show()
        }

        saveFavorites()
        updateFavoriteButton(favoriteButton, !isFavorite)
    }

    private fun updateFavoriteButton(favoriteButton: ImageView, isFavorite: Boolean) {
        if (isFavorite) {
            favoriteButton.setImageResource(R.drawable.ic_heart_filled)
            favoriteButton.clearColorFilter()
        } else {
            favoriteButton.setImageResource(R.drawable.ic_heart_outline)
            favoriteButton.clearColorFilter()
        }
    }

    private fun animateFavoriteButton(favoriteButton: ImageView, isFavorite: Boolean) {
        val scaleAnimation = if (isFavorite) {
            ScaleAnimation(1.0f, 1.3f, 1.0f, 1.3f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f)
        } else {
            ScaleAnimation(1.3f, 1.0f, 1.3f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f)
        }

        scaleAnimation.duration = 200
        scaleAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                val reverseAnimation = ScaleAnimation(1.3f, 1.0f, 1.3f, 1.0f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f)
                reverseAnimation.duration = 200
                favoriteButton.startAnimation(reverseAnimation)
            }
            override fun onAnimationRepeat(animation: Animation?) {}
        })

        favoriteButton.startAnimation(scaleAnimation)
    }

    fun getFavoriteCount(): Int {
        return favoriteItems.size
    }
}