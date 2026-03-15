package com.example.tamaade.ui.products

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.tamaade.data.local.AppDatabase
import com.example.tamaade.databinding.ActivityProductListBinding
import com.example.tamaade.presentation.adapter.ProductAdapter
import com.example.tamaade.data.model.Product as LocalProduct

class ProductListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductListBinding
    private lateinit var viewModel: ProductViewModel
    private lateinit var productAdapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getDatabase(this)
        val viewModelFactory = ProductViewModelFactory(database.cartDao(), database.favoriteDao(), database.productDao())
        viewModel = ViewModelProvider(this, viewModelFactory).get(ProductViewModel::class.java)

        setupRecyclerView()
        setupObservers()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter()
        binding.recyclerViewProducts.apply {
            layoutManager = GridLayoutManager(this@ProductListActivity, 2)
            adapter = productAdapter
        }
    }

    private fun setupObservers() {
        viewModel.products.observe(this, Observer {
            remoteProducts ->
            val localProducts = remoteProducts.map { remoteProduct ->
                LocalProduct(
                    id = remoteProduct.id,
                    productName = remoteProduct.name,
                    productCategory = remoteProduct.category,
                    productDescription = remoteProduct.desc,
                    productImage = remoteProduct.image,
                    productPrice = remoteProduct.price,
                    slug = remoteProduct.slug
                )
            }
            productAdapter.submitList(localProducts)
        })

        viewModel.isLoading.observe(this, Observer {
            isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        })

        viewModel.errorMessage.observe(this, Observer {
            errorMessage ->
            binding.textViewError.text = errorMessage
            binding.textViewError.visibility = if (errorMessage != null) View.VISIBLE else View.GONE
        })
    }
}