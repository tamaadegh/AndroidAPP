package com.example.tamaade.presentation.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.tamaade.databinding.FragmentHomeBinding
import com.example.tamaade.presentation.activity.ProductDetailActivity
import com.example.tamaade.presentation.adapter.NewArrivalsAdapter
import com.example.tamaade.presentation.adapter.ProductAdapter
import com.example.tamaade.ui.products.ProductViewModel
import com.example.tamaade.data.model.Product as LocalProduct

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var newArrivalsAdapter: NewArrivalsAdapter
    private lateinit var productAdapter: ProductAdapter
    private lateinit var viewModel: ProductViewModel

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

        val db = com.example.tamaade.data.local.AppDatabase.getDatabase(requireContext())
        val factory = com.example.tamaade.ui.products.ProductViewModelFactory(db.cartDao(), db.favoriteDao(), db.productDao())
        viewModel = ViewModelProvider(this, factory).get(ProductViewModel::class.java)

        setupRecyclerViews()
        setupSearch()
        setupSwipeRefresh()
        setupScrollListener()
        observeViewModel()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshProducts()
        }
        
        binding.swipeRefresh.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )
    }

    private fun setupScrollListener() {
        binding.nestedScrollView.setOnScrollChangeListener { v: androidx.core.widget.NestedScrollView, _, scrollY, _, _ ->
            if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                // Bottom of list
                viewModel.loadMoreProducts()
            }
        }
    }

    private fun setupSearch() {
        binding.etSearch.setOnEditorActionListener { textView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                val query = textView.text.toString().trim()
                viewModel.searchProducts(query)
                hideKeyboard()
                true
            } else {
                false
            }
        }

        binding.etSearch.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                val query = binding.etSearch.text.toString().trim()
                viewModel.searchProducts(query)
                hideKeyboard()
                true
            } else {
                false
            }
        }

        // Clear search when the clear icon is clicked
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    viewModel.refreshProducts()
                }
            }
        })
    }

    private fun hideKeyboard() {
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    private fun setupRecyclerViews() {
        newArrivalsAdapter = NewArrivalsAdapter()
        binding.rvNewArrivals.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = newArrivalsAdapter
        }

        productAdapter = ProductAdapter()
        binding.productsRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = productAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun observeViewModel() {
        viewModel.products.observe(viewLifecycleOwner) { products ->
            binding.animationView.visibility = View.GONE
            
            // if (products.isNullOrEmpty()) { ... } logic could be kept, but with cache we might have empty first then load.
            // If empty and not loading, show empty state?
            
            val localProducts = products.map { product ->
                LocalProduct(
                    id = product.id,
                    productName = product.name,
                    productCategory = product.category,
                    productDescription = product.desc,
                    productImage = product.image,
                    productPrice = product.price,
                    slug = product.slug
                )
            }
            
            if (localProducts.isNotEmpty()) {
                val heroItem = localProducts.first()
                Glide.with(this)
                    .load(heroItem.productImage)
                    .centerCrop()
                    .into(binding.ivHeroImage)

                binding.cvHeroBanner.setOnClickListener {
                    val intent = Intent(requireContext(), ProductDetailActivity::class.java).apply {
                        putExtra(ProductDetailActivity.PRODUCT_SLUG_EXTRA, heroItem.slug)
                    }
                    startActivity(intent)
                }

                newArrivalsAdapter.submitList(localProducts.take(5))
                productAdapter.submitList(localProducts)
                binding.cvHeroBanner.visibility = View.VISIBLE
            } else {
                 // Maybe show empty view or keep previous
                 binding.cvHeroBanner.visibility = View.GONE
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
             // If initial load (empty list), show animationView?
             if (isLoading && productAdapter.itemCount == 0) {
                 binding.animationView.visibility = View.VISIBLE
             } else {
                 binding.animationView.visibility = View.GONE
             }
        }
        
        viewModel.isMoreLoading.observe(viewLifecycleOwner) { isMore ->
             // Could show a progress bar at bottom of list
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            binding.animationView.visibility = View.GONE
            binding.swipeRefresh.isRefreshing = false
            if (!error.isNullOrEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
