package com.example.tamaade.presentation.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tamaade.activities.CheckoutActivity
import com.example.tamaade.data.local.room.CartViewModel
import com.example.tamaade.data.local.room.ProductEntity
import com.example.tamaade.databinding.FragmentBagBinding
import com.example.tamaade.presentation.adapter.CartAdapter
import com.example.tamaade.presentation.adapter.CartItemClickAdapter

class BagFragment : Fragment(), CartItemClickAdapter {

    private var _binding: FragmentBagBinding? = null
    private val binding get() = _binding!!

    private lateinit var cartViewModel: CartViewModel
    private lateinit var cartAdapter: CartAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBagBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cartViewModel = ViewModelProvider(this).get(CartViewModel::class.java)

        setupRecyclerView()

        cartViewModel.allproducts.observe(viewLifecycleOwner, Observer { list ->
            cartAdapter.submitList(list)
            updateUI(list)
        })

        binding.checkoutButton.setOnClickListener {
            val totalAmount = cartViewModel.allproducts.value?.sumOf { it.price }?.toDouble() ?: 0.0

            if (totalAmount > 0) {
                val intent = Intent(requireActivity(), CheckoutActivity::class.java).apply {
                    putExtra(CheckoutActivity.EXTRA_AMOUNT, totalAmount)
                }
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "Your cart is empty.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(requireContext(), this)
        binding.cartRecView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cartAdapter
        }
    }

    private fun updateUI(list: List<ProductEntity>?) {
        if (list.isNullOrEmpty()) {
            binding.animationViewCartPage.playAnimation()
            binding.animationViewCartPage.loop(true)
            binding.bottomCartLayout.visibility = View.GONE
            binding.MybagText.visibility = View.GONE
            binding.emptyBagMsgLayout.visibility = View.VISIBLE
        } else {
            binding.animationViewCartPage.pauseAnimation()
            binding.bottomCartLayout.visibility = View.VISIBLE
            binding.MybagText.visibility = View.VISIBLE
            binding.emptyBagMsgLayout.visibility = View.GONE
        }

        val sum = list?.sumOf { it.price } ?: 0
        binding.totalPriceBagFrag.text = "$${sum}"
    }

    override fun onItemDeleteClick(product: ProductEntity) {
        cartViewModel.deleteCart(product)
        Toast.makeText(requireContext(), "Removed From Bag", Toast.LENGTH_SHORT).show()
    }

    override fun onItemUpdateClick(product: ProductEntity) {
        cartViewModel.updateCart(product)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}