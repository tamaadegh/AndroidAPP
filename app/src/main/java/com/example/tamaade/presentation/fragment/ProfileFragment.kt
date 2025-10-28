package com.example.tamaade.presentation.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tamaade.R
import com.example.tamaade.databinding.FragmentProfileBinding
import com.example.tamaade.presentation.activity.LoginActivity
import com.example.tamaade.presentation.activity.SettingsActivity
import com.example.tamaade.presentation.adapter.ProfileAction
import com.example.tamaade.presentation.adapter.ProfileActionsAdapter
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

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

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        binding.profileName.text = currentUser?.displayName ?: "N/A"
        binding.profileEmail.text = currentUser?.email ?: "N/A"

        val actions = listOf(
            ProfileAction(R.drawable.ic_orders, "Orders"),
            ProfileAction(R.drawable.ic_favorite, "Favorites"),
            ProfileAction(R.drawable.ic_settings, "Settings"),
            ProfileAction(R.drawable.ic_logout, "Logout")
        )

        val adapter = ProfileActionsAdapter(actions) { action ->
            when (action.title) {
                "Orders" -> Toast.makeText(context, "Orders Clicked", Toast.LENGTH_SHORT).show()
                "Favorites" -> Toast.makeText(context, "Favorites Clicked", Toast.LENGTH_SHORT).show()
                "Settings" -> startActivity(Intent(requireContext(), SettingsActivity::class.java))
                "Logout" -> {
                    auth.signOut()
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                    requireActivity().finish()
                }
            }
        }

        binding.profileActionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.profileActionsRecyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}