package com.example.tamaade.presentation.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.tamaade.data.local.AppDatabase
import com.example.tamaade.databinding.FragmentFavoriteBinding
import com.example.tamaade.presentation.adapter.ProductAdapter
import com.example.tamaade.ui.products.ProductViewModel
import com.example.tamaade.ui.products.ProductViewModelFactory
import com.example.tamaade.data.model.Product as LocalProduct

class FavoriteFragment : Fragment() {

    private var _binding: FragmentFavoriteBinding? = null
    private val binding get() = _binding!!

    private lateinit var productViewModel: ProductViewModel
    private lateinit var favoritesAdapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoriteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = AppDatabase.getDatabase(requireContext())
        val viewModelFactory = ProductViewModelFactory(
            database.cartDao(),
            database.favoriteDao(),
            database.productDao()
        )
        productViewModel = ViewModelProvider(this, viewModelFactory).get(ProductViewModel::class.java)

        setupRecyclerViews()
        setupObservers()
    }

    private fun setupRecyclerViews() {
        favoritesAdapter = ProductAdapter()
        binding.allProductsRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = favoritesAdapter
        }
    }

    private fun setupObservers() {
        productViewModel.favoriteProducts.observe(viewLifecycleOwner, Observer { remoteProducts ->
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

            if (localProducts.isEmpty()) {
                // Show empty state?
                favoritesAdapter.submitList(emptyList())
            } else {
                favoritesAdapter.submitList(localProducts)
            }
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