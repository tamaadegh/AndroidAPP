package com.example.tamaade.presentation.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.tamaade.R
import com.example.tamaade.data.network.ImageDto
import com.example.tamaade.data.network.ProductDetailDto
import com.example.tamaade.databinding.ActivityProductDetailBinding
import com.example.tamaade.presentation.adapter.ImageSliderAdapter
import com.example.tamaade.presentation.viewmodel.ProductDetailViewModel
import com.google.firebase.auth.FirebaseAuth

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailBinding
    private lateinit var viewModel: ProductDetailViewModel
    private var productSlug: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(ProductDetailViewModel::class.java)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "" // Clear title
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }

        productSlug = intent.getStringExtra(PRODUCT_SLUG_EXTRA)

        binding.loadingView.visibility = View.VISIBLE // Show loading initially
        
        if (productSlug != null) {
            viewModel.fetchProductDetails(productSlug!!)
        } else {
            Toast.makeText(this, "Error: Product not found", Toast.LENGTH_SHORT).show()
            finish()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.product.observe(this) { product ->
            binding.loadingView.visibility = View.GONE
            if (product != null) {
                populateUi(product)
            } else {
                 Toast.makeText(this, "Error processing product data", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.isFavorite.observe(this) { isFav ->
            if (isFav) {
                binding.ivFavorite.setImageResource(R.drawable.ic_favorite_filled)
                binding.ivFavorite.setColorFilter(ContextCompat.getColor(this, R.color.primary))
            } else {
                binding.ivFavorite.setImageResource(R.drawable.ic_favorite_border)
                binding.ivFavorite.setColorFilter(ContextCompat.getColor(this, android.R.color.darker_gray))
            }
        }

        viewModel.error.observe(this) { error ->
            binding.loadingView.visibility = View.GONE
            Toast.makeText(this, error, Toast.LENGTH_LONG).show() // Long toast for errors
            Log.e("ProductDetail", "Error fetching product: $error")
        }
    }

    private fun populateUi(product: ProductDetailDto) {
        Log.d("ProductDetail", "Populating UI: ${product.texts.name}")
        
        binding.productName.text = product.texts.name
        
        // Price logic: Avoid double GHS
        val price = product.variants.firstOrNull()?.price
        val displayPrice = if (price != null) {
            val p = price.trim()
            if (p.startsWith("GHS", ignoreCase = true)) p else "GHS $p"
        } else "Price N/A"
        binding.productPrice.text = displayPrice

        // Description
        binding.productDescription.text = product.texts.description ?: product.texts.summary ?: "No description available."

        // Image Logic: Carousel vs Thumbnail
        if (product.images.isNotEmpty()) {
            setupImageSlider(product.images)
        } else {
            binding.imageSlider.visibility = View.GONE
            binding.slideIndicator.visibility = View.GONE
            binding.productThumbnail.visibility = View.VISIBLE
            
            // Image: Try high-res image first, fall back to thumbnail
            val imageUrl = product.productThumbnail
            Glide.with(this)
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_delete)
                .into(binding.productThumbnail)
        }

        // Handle Add to Cart logic
        binding.btnAddToCart.setOnClickListener {
             if (isUserSignedIn()) {
                 val variantId = product.variants.firstOrNull()?.id ?: product.id
                 viewModel.addToCart(variantId, 1) 
                 Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show()
             } else {
                 redirectToSignUp()
             }
        }

        // Handle Favorite Click
        binding.ivFavorite.setOnClickListener {
            if (isUserSignedIn()) {
                 viewModel.toggleFavorite()
            } else {
                 redirectToSignUp()
            }
        }
    }
    
    private fun setupImageSlider(images: List<ImageDto>) {
        binding.productThumbnail.visibility = View.GONE
        binding.imageSlider.visibility = View.VISIBLE
        binding.slideIndicator.visibility = View.VISIBLE
        
        val adapter = ImageSliderAdapter(images)
        binding.imageSlider.adapter = adapter
        
        setupIndicators(images.size)
        setCurrentIndicator(0)
        
        binding.imageSlider.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setCurrentIndicator(position)
            }
        })
    }
    
    private fun setupIndicators(count: Int) {
        binding.slideIndicator.removeAllViews()
        val indicators = arrayOfNulls<ImageView>(count)
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(8, 0, 8, 0)
        for (i in indicators.indices) {
            indicators[i] = ImageView(applicationContext)
            indicators[i]?.setImageDrawable(
                ContextCompat.getDrawable(
                    applicationContext,
                    R.drawable.circle_shape // Uses circle_shape drawable
                )
            )
            // Initial grey tint
            indicators[i]?.setColorFilter(ContextCompat.getColor(applicationContext, android.R.color.darker_gray)) 
             
            layoutParams.width = 20
            layoutParams.height = 20
            indicators[i]?.layoutParams = layoutParams
            binding.slideIndicator.addView(indicators[i])
        }
    }
    
    private fun setCurrentIndicator(index: Int) {
        val childCount = binding.slideIndicator.childCount
        for (i in 0 until childCount) {
            val imageView = binding.slideIndicator.getChildAt(i) as ImageView
            if (i == index) {
                imageView.setColorFilter(ContextCompat.getColor(applicationContext, R.color.primary)) // Green active
            } else {
                imageView.setColorFilter(ContextCompat.getColor(applicationContext, android.R.color.darker_gray)) // Grey inactive
            }
        }
    }

    private fun isUserSignedIn(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
    }

    private fun redirectToSignUp() {
        Toast.makeText(this, "Please sign in to continue", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, com.example.tamaade.presentation.activity.SignUpActivity::class.java)
        startActivity(intent)
    }

    companion object {
        const val PRODUCT_SLUG_EXTRA = "PRODUCT_SLUG_EXTRA"
    }
}
