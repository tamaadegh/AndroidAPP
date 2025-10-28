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
import com.example.tamaade.data.repository.ProductRepository
import com.example.tamaade.databinding.FragmentShopBinding
import com.example.tamaade.presentation.adapter.ProductAdapter
import com.example.tamaade.ui.products.ProductViewModel
import com.example.tamaade.ui.products.ProductViewModelFactory

class ShopFragment : Fragment() {

    private var _binding: FragmentShopBinding? = null
    private val binding get() = _binding!!

    private lateinit var productViewModel: ProductViewModel
    private lateinit var newProductsAdapter: ProductAdapter
    private lateinit var allProductsAdapter: ProductAdapter

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

        val repository = ProductRepository()
        val viewModelFactory = ProductViewModelFactory(repository)
        productViewModel = ViewModelProvider(this, viewModelFactory).get(ProductViewModel::class.java)

        setupRecyclerViews()

        productViewModel.products.observe(viewLifecycleOwner, Observer {
            products ->
            newProductsAdapter.submitList(products.shuffled().take(10))
            allProductsAdapter.submitList(products)
        })

        productViewModel.isLoading.observe(viewLifecycleOwner, Observer {
            isLoading ->
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}