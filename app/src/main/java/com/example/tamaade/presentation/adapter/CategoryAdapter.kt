package com.example.tamaade.presentation.adapter


import android.content.Context

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tamaade.data.model.Category

import com.example.tamaade.R

class CategoryAdapter(var ctx: Context, private var categoryList: ArrayList<Category>) :RecyclerView.Adapter<CategoryAdapter.ViewHolder>(){



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val categoryView = LayoutInflater.from(parent.context).inflate(R.layout.category_single,parent,false)
        return ViewHolder(categoryView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {



        val item: Category = categoryList[position]

        holder.categoryName_CateSingle.text = item.Name

        Glide.with(ctx)
            .load(item.Image)
            .into(holder.categoryImage_CateSingle)

    }

    override fun getItemCount(): Int {
        return categoryList.size
    }

    fun updateData(newCategories: List<Category>) {
        categoryList.clear()
        categoryList.addAll(newCategories)
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){

        val categoryImage_CateSingle: ImageView = itemView.findViewById(R.id.categoryImage_CateSingle)
        val categoryName_CateSingle: TextView = itemView.findViewById(R.id.categoryName_CateSingle)

    }

}