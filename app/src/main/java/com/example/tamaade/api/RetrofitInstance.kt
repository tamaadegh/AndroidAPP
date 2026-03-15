package com.example.tamaade.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://dashboard-production-8b7f.up.railway.app/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: ProductApi by lazy {
        retrofit.create(ProductApi::class.java)
    }
}