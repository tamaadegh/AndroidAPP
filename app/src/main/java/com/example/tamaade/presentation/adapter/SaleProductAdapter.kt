package com.example.tamaade.presentation.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
// Import the correct Product model
import com.example.tamaade.data.remote.model.Product
import com.example.tamaade.presentation.activity.ProductDetailsActivity
import com.example.tamaade.R

class SaleProductAdapter(private val saleProductList: ArrayList<Product>, context: Context):  RecyclerView.Adapter<SaleProductAdapter.ViewHolder>()  {

    val ctx: Context = context


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val productView = LayoutInflater.from(parent.context).inflate(R.layout.single_product,parent,false)
        return ViewHolder(productView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val product: Product = saleProductList[position]
        // Use fields from the new Product model
        holder.productBrandName_singleProduct.text = product.category
        holder.productName_singleProduct.text = product.name
        holder.productPrice_singleProduct.text = "$${product.price}"
        // The API does not provide a rating, so we'll use a static value for the UI
        holder.productRating_singleProduct.rating = 4.5f

        Glide.with(ctx)
            .load(product.image)
            .placeholder(R.drawable.bn)
            .into(holder.productImage_singleProduct)


        holder.discount_singleProduct.visibility = View.VISIBLE
        holder.discountTv_singleProduct.text = "New"


        holder.itemView.setOnClickListener {
            goDetailsPage(position)
        }
    }

    override fun getItemCount(): Int {
        return saleProductList.size
    }





    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){

        val productImage_singleProduct: ImageView = itemView.findViewById(R.id.productImage_singleProduct)
        val productAddToFav_singleProduct: ImageView = itemView.findViewById(R.id.productAddToFav_singleProduct)
        val productRating_singleProduct: RatingBar = itemView.findViewById(R.id.productRating_singleProduct)
        val productBrandName_singleProduct: TextView = itemView.findViewById(R.id.productBrandName_singleProduct)
        val discountTv_singleProduct: TextView = itemView.findViewById(R.id.discountTv_singleProduct)
        val productName_singleProduct: TextView = itemView.findViewById(R.id.productName_singleProduct)
        val productPrice_singleProduct: TextView = itemView.findViewById(R.id.productPrice_singleProduct)
        val discount_singleProduct = itemView.findViewById<LinearLayout>(R.id.discount_singleProduct)


    }

    private fun goDetailsPage(position: Int) {
        val intent = Intent(ctx , ProductDetailsActivity::class.java)
        intent.putExtra("ProductIndex", position)
        intent.putExtra("ProductFrom", "Cover")
        ctx.startActivity(intent)
    }
}