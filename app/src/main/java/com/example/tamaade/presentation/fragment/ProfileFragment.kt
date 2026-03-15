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
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        loadProfileData()
    }

    private fun loadProfileData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Display name from Firebase Auth (updated during sign up)
            binding.profileName.text = currentUser.displayName ?: "User"
            binding.profileEmail.text = currentUser.email

            // Fetch latest name from Firestore as a backup/sync
            firestore.collection("Users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    val name = document.getString("userName")
                    if (!name.isNullOrEmpty()) {
                        binding.profileName.text = name
                    }
                }
        } else {
            binding.profileName.text = "Guest User"
            binding.profileEmail.text = "Sign in to view profile"
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        loadProfileData()

        val actions = listOf(
            ProfileAction(R.drawable.ic_orders, "Orders"),
            ProfileAction(R.drawable.ic_favorite, "Favorites"),
            ProfileAction(R.drawable.ic_settings, "Settings"),
            ProfileAction(R.drawable.ic_logout, "Logout")
        )

        val adapter = ProfileActionsAdapter(actions) { action ->
            when (action.title) {
                "Orders" -> Toast.makeText(context, "Orders Clicked", Toast.LENGTH_SHORT).show()
                "Favorites" -> {
                    // Navigate to favorites if needed, or show toast
                    Toast.makeText(context, "Favorites Clicked", Toast.LENGTH_SHORT).show()
                }
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
