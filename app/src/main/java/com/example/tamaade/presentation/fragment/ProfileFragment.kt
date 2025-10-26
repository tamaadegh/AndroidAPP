package com.example.tamaade.presentation.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.tamaade.databinding.FragmentProfileBinding
import com.example.tamaade.presentation.activity.SettingsActivity

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

        // You can now access your views using binding
        binding.settingCdProfileFrag.setOnClickListener {
            val intent = Intent(requireActivity(), SettingsActivity::class.java)
            startActivity(intent)
        }

        // TODO: Add logic for other views like:
        // binding.profileNameProfileFrag.text = "Your Name"
        // binding.profileEmailProfileFrag.text = "your.email@example.com"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}