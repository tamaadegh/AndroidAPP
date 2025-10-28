package com.example.tamaade.presentation.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tamaade.R
import com.example.tamaade.databinding.FragmentProfileBinding
import com.example.tamaade.presentation.adapter.ProfileAction
import com.example.tamaade.presentation.adapter.ProfileActionsAdapter

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set user info (replace with actual data)
        binding.profileName.text = "John Doe"
        binding.profileEmail.text = "john.doe@example.com"

        val actions = listOf(
            ProfileAction(R.drawable.ic_orders, "Orders"),
            ProfileAction(R.drawable.ic_favorite, "Favorites"),
            ProfileAction(R.drawable.ic_settings, "Settings"),
            ProfileAction(R.drawable.ic_logout, "Logout")
        )

        val adapter = ProfileActionsAdapter(actions)
        binding.profileActionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.profileActionsRecyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}