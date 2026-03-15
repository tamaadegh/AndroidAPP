package com.example.tamaade.presentation.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.tamaade.data.local.AppDatabase
import com.example.tamaade.databinding.FragmentFavBinding
import com.example.tamaade.presentation.activity.ProductDetailActivity
import com.example.tamaade.presentation.adapter.ProductAdapter
import com.example.tamaade.ui.products.ProductViewModel
import com.example.tamaade.ui.products.ProductViewModelFactory
import com.example.tamaade.data.model.Product as LocalProduct

class FavFragment : Fragment() {

    private var _binding: FragmentFavBinding? = null
    private val binding get() = _binding!!

    private lateinit var productViewModel: ProductViewModel
    private lateinit var favoritesAdapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavBinding.inflate(inflater, container, false)
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

        setupRecyclerView()
        setupObservers()
    }

    private fun setupRecyclerView() {
        favoritesAdapter = ProductAdapter()
        binding.favoritesRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = favoritesAdapter
        }
        
        // Handle item clicks to open details
        favoritesAdapter.onItemClick = { product ->
             val intent = Intent(requireContext(), ProductDetailActivity::class.java).apply {
                 putExtra(ProductDetailActivity.PRODUCT_SLUG_EXTRA, product.slug)
             }
             startActivity(intent)
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
                binding.emptyBagMsgLayout.visibility = View.VISIBLE
                binding.favoritesRecyclerView.visibility = View.GONE
                binding.favoriteCountText.visibility = View.GONE
            } else {
                binding.emptyBagMsgLayout.visibility = View.GONE
                binding.favoritesRecyclerView.visibility = View.VISIBLE
                binding.favoriteCountText.visibility = View.VISIBLE
                binding.favoriteCountText.text = "You have ${localProducts.size} favorite items"
                favoritesAdapter.submitList(localProducts)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
