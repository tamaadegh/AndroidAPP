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

class CoverProductAdapter(var ctx: Context, private var coverProductList: ArrayList<Product>) :RecyclerView.Adapter<CoverProductAdapter.ViewHolder>(){



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val productView = LayoutInflater.from(parent.context).inflate(R.layout.cover_single,parent,false)
        return ViewHolder(productView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {



        val coverPro: Product = coverProductList[position]

        // Use fields from the new Product model
        holder.productNoteCover.text = coverPro.desc
        Glide.with(ctx)
            .load(coverPro.image)
            .into(holder.productImage_coverPage)


        holder.productCheck_coverPage.setOnClickListener {

            goDetailsPage(position)


        }

    }




    override fun getItemCount(): Int {
        return coverProductList.size
    }

    fun updateData(newProducts: List<Product>) {
        coverProductList.clear()
        coverProductList.addAll(newProducts)
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){

        val productImage_coverPage: ImageView = itemView.findViewById(R.id.productImage_coverPage)
        val productNoteCover: TextView = itemView.findViewById(R.id.productNoteCover)
        val productCheck_coverPage: Button = itemView.findViewById(R.id.productCheck_coverPage)


    }

    private fun goDetailsPage(position: Int) {
        val intent = Intent(ctx , ProductDetailsActivity::class.java)
        intent.putExtra("ProductIndex", position)
        intent.putExtra("ProductFrom", "Cover")
        ctx.startActivity(intent)
    }
}