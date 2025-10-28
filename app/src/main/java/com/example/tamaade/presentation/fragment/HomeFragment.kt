package com.example.tamaade.presentation.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.tamaade.api.ProductApi
import com.example.tamaade.data.model.Product as LocalProduct
import com.example.tamaade.databinding.FragmentHomeBinding
import com.example.tamaade.presentation.adapter.ProductAdapter
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var productAdapter: ProductAdapter
    private lateinit var productApi: ProductApi

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://tamaadeapi-7it5.onrender.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        productApi = retrofit.create(ProductApi::class.java)

        setupRecyclerView()
        fetchProducts()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter()
        binding.productsRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = productAdapter
        }
    }

    private fun fetchProducts() {
        binding.animationView.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = productApi.getProducts()
                val localProducts = response.map { remoteProduct ->
                    LocalProduct(
                        id = remoteProduct.id,
                        productName = remoteProduct.name,
                        productDescription = remoteProduct.desc,
                        productImage = remoteProduct.image,
                        productPrice = remoteProduct.price,
                        quantity = remoteProduct.quantity,
                        productCategory = remoteProduct.category,
                        productBrand = null, // Not available in remote model
                        productRating = 0f, // Not available in remote model
                        productHave = null, // Not available in remote model
                        productDisCount = null // Not available in remote model
                    )
                }
                productAdapter.submitList(localProducts)
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error fetching products", e)
            }
            binding.animationView.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}