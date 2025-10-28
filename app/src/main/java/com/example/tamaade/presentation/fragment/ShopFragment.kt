package com.example.tamaade.presentation.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tamaade.data.local.AppDatabase
import com.example.tamaade.databinding.FragmentShopBinding
import com.example.tamaade.presentation.adapter.CategoryAdapter
import com.example.tamaade.presentation.adapter.ProductAdapter
import com.example.tamaade.ui.products.ProductViewModel
import com.example.tamaade.ui.products.ProductViewModelFactory
import com.example.tamaade.data.model.Product as LocalProduct

class ShopFragment : Fragment() {

    private var _binding: FragmentShopBinding? = null
    private val binding get() = _binding!!

    private lateinit var productViewModel: ProductViewModel
    private lateinit var newProductsAdapter: ProductAdapter
    private lateinit var allProductsAdapter: ProductAdapter
    private lateinit var categoryAdapter: CategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShopBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = AppDatabase.getDatabase(requireContext())
        val viewModelFactory = ProductViewModelFactory(database.cartDao(), database.favoriteDao())
        productViewModel = ViewModelProvider(this, viewModelFactory).get(ProductViewModel::class.java)

        setupRecyclerViews()
        setupObservers()
    }

    private fun setupRecyclerViews() {
        newProductsAdapter = ProductAdapter()
        binding.newProductsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = newProductsAdapter
        }

        allProductsAdapter = ProductAdapter()
        binding.allProductsRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = allProductsAdapter
        }

        categoryAdapter = CategoryAdapter()
        binding.categoriesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }
    }

    private fun setupObservers() {
        productViewModel.products.observe(viewLifecycleOwner, Observer { remoteProducts ->
            val localProducts = remoteProducts.map { remoteProduct ->
                LocalProduct(
                    id = remoteProduct.id,
                    productName = remoteProduct.name,
                    productDescription = remoteProduct.desc,
                    productImage = remoteProduct.image,
                    productPrice = remoteProduct.price,
                    quantity = remoteProduct.quantity,
                    productCategory = remoteProduct.category,
                    productBrand = null,
                    productRating = 0f,
                    productHave = null,
                    productDisCount = null
                )
            }
            newProductsAdapter.submitList(localProducts.shuffled().take(10))
            allProductsAdapter.submitList(localProducts)
        })

        productViewModel.categories.observe(viewLifecycleOwner, Observer { categories ->
            categoryAdapter.submitList(categories)
        })

        productViewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            if (isLoading) {
                binding.shimmerViewContainer.startShimmer()
                binding.shimmerViewContainer.visibility = View.VISIBLE
                binding.allProductsRecyclerView.visibility = View.GONE
            } else {
                binding.shimmerViewContainer.stopShimmer()
                binding.shimmerViewContainer.visibility = View.GONE
                binding.allProductsRecyclerView.visibility = View.VISIBLE
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}