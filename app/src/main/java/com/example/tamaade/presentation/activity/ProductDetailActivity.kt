package com.example.tamaade.presentation.activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.tamaade.R
import com.example.tamaade.data.local.AppDatabase
import com.example.tamaade.data.model.Product
import com.example.tamaade.databinding.ActivityProductDetailBinding
import com.example.tamaade.ui.products.ProductViewModel
import com.example.tamaade.ui.products.ProductViewModelFactory

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailBinding
    private lateinit var viewModel: ProductViewModel
    private var product: Product? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getDatabase(this)
        val viewModelFactory = ProductViewModelFactory(database.cartDao(), database.favoriteDao())
        viewModel = ViewModelProvider(this, viewModelFactory).get(ProductViewModel::class.java)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }

        product = intent.getParcelableExtra(PRODUCT_EXTRA)

        product?.let { 
            populateUi(it)
            setupObservers(it.id)
        }

        binding.btnAddToCart.setOnClickListener {
            product?.let {
                viewModel.addToCart(it.id, 1)
                Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show()
            }
        }

        binding.fabFavorite.setOnClickListener {
            product?.let { viewModel.toggleFavorite(it.id) }
        }
    }

    private fun populateUi(product: Product) {
        binding.collapsingToolbar.title = product.productName
        binding.productName.text = product.productName
        binding.productPrice.text = product.productPrice
        binding.productDescription.text = product.productDescription

        Glide.with(this)
            .load(product.productImage)
            .into(binding.productImage)
    }

    private fun setupObservers(productId: Int) {
        viewModel.favoriteProductIds.observe(this) { favoriteIds ->
            val isFavorite = favoriteIds.contains(productId)
            val icon = if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
            binding.fabFavorite.setImageResource(icon)
        }
    }

    companion object {
        const val PRODUCT_EXTRA = "PRODUCT_EXTRA"
    }
}