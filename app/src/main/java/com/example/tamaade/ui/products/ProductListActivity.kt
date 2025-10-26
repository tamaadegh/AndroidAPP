package com.example.tamaade.ui.products

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.tamaade.data.repository.ProductRepository
import com.example.tamaade.databinding.ActivityProductListBinding

class ProductListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductListBinding
    private lateinit var viewModel: ProductViewModel
    private lateinit var productAdapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repository = ProductRepository()
        val viewModelFactory = ProductViewModelFactory(repository)
        viewModel = ViewModelProvider(this, viewModelFactory).get(ProductViewModel::class.java)

        setupRecyclerView()
        setupObservers()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(emptyList())
        binding.recyclerViewProducts.apply {
            layoutManager = GridLayoutManager(this@ProductListActivity, 2)
            adapter = productAdapter
        }
    }

    private fun setupObservers() {
        viewModel.products.observe(this, Observer {
            products ->
            productAdapter.updateProducts(products)
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
