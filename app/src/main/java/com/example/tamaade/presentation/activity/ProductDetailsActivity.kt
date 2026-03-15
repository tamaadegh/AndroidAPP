package com.example.tamaade.presentation.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.tamaade.R
import com.example.tamaade.data.local.AppDatabase
import com.example.tamaade.data.local.room.CartViewModel
import com.example.tamaade.data.local.room.CartViewModelFactory
import com.example.tamaade.data.local.room.ProductEntity
import com.example.tamaade.data.remote.model.Product
import com.example.tamaade.databinding.ActivityProductDetailsBinding
import com.example.tamaade.ui.products.ProductViewModel
import com.example.tamaade.ui.products.ProductViewModelFactory
import com.example.tamaade.utils.DefaultCard.GetDefCard
import com.example.tamaade.utils.Extensions.cardXXGen
import com.example.tamaade.utils.Extensions.toast
import com.google.android.material.bottomsheet.BottomSheetDialog

class ProductDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailsBinding
    private lateinit var cartViewModel: CartViewModel
    private lateinit var productViewModel: ProductViewModel
    private var product: Product? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        val database = AppDatabase.getDatabase(this)
        val productViewModelFactory = ProductViewModelFactory(database.cartDao(), database.favoriteDao(), database.productDao())
        productViewModel = ViewModelProvider(this, productViewModelFactory).get(ProductViewModel::class.java)

        val cartViewModelFactory = CartViewModelFactory(application)
        cartViewModel = ViewModelProvider(this, cartViewModelFactory).get(CartViewModel::class.java)

        val productId = intent.getIntExtra("PRODUCT_ID", -1)

        productViewModel.products.observe(this, { products ->
            product = products.find { it.id == productId }
            product?.let { setProductData(it) }
        })

        binding.backIvProfileFrag.setOnClickListener { onBackPressed() }

        binding.addToCartProductDetailsPage.setOnClickListener { showAddToCartBottomSheet() }

        setupCardView()
    }

    private fun setProductData(product: Product) {
        binding.productNameProductDetailsPage.text = product.name
        binding.productPriceProductDetailsPage.text = "$${product.price}"
        binding.productDesProductDetailsPage.text = product.desc

        Glide.with(this)
            .load(product.image)
            .into(binding.productImageProductDetailsPage)
    }

    private fun setupCardView() {
        val cardNumber = GetDefCard()
        if (cardNumber.isNullOrEmpty()) {
            binding.cardNumberProductDetails.text = "You Have No Cards"
        } else {
            binding.cardNumberProductDetails.text = cardXXGen(cardNumber)
        }

        binding.shippingAddressProductDetailsPage.setOnClickListener {
            startActivity(android.content.Intent(this, PaymentMethodActivity::class.java))
        }
    }

    private fun showAddToCartBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val bottomSheetView = LayoutInflater.from(applicationContext).inflate(
            R.layout.fragment_add_to_bag,
            findViewById(R.id.bottomSheet)
        )

        bottomSheetView.findViewById<View>(R.id.addToCart_BottomSheet).setOnClickListener {
            val quantity = bottomSheetView.findViewById<android.widget.EditText>(R.id.quantityEtBottom).text.toString().toInt()
            product?.let {
                val productEntity = ProductEntity(
                    it.name,
                    quantity,
                    it.price.toInt() * quantity,
                    it.id.toString(),
                    it.image ?: ""
                )
                cartViewModel.insert(productEntity)
                toast("Added to Bag Successfully")
            }
            bottomSheetDialog.dismiss()
        }
        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()
    }
}