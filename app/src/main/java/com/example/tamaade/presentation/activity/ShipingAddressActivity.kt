package com.example.tamaade.presentation.activity

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tamaade.R
import com.example.tamaade.presentation.adapter.AddressAdapter
import com.example.tamaade.data.model.Address
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ShipingAddressActivity : AppCompatActivity() {

    private lateinit var addressRecyclerView: RecyclerView
    private lateinit var addAddressFab: FloatingActionButton
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var addressAdapter: AddressAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private val addressList = ArrayList<Address>()

    private val userCollectionRef = Firebase.firestore.collection("Users")
    val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private var userName: String = ""
    private var userPhone: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shiping_address)

        initViews()
        setupRecyclerView()
        loadSavedAddresses()
        setupClickListeners()
        getUserData()
    }

    private fun initViews() {
        addressRecyclerView = findViewById(R.id.addressRecyclerView)
        addAddressFab = findViewById(R.id.addAddress_ShippingPage)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        sharedPreferences = getSharedPreferences("shipping_addresses", MODE_PRIVATE)
    }

    private fun setupRecyclerView() {
        addressAdapter = AddressAdapter(addressList, this) { address ->
            // Handle address selection
            selectAddress(address)
        }
        addressRecyclerView.layoutManager = LinearLayoutManager(this)
        addressRecyclerView.adapter = addressAdapter
    }

    private fun setupClickListeners() {
        addAddressFab.setOnClickListener {
            showAddAddressDialog()
        }

        findViewById<ImageView>(R.id.backBtn_ShippingPage).setOnClickListener {
            finish()
        }
    }

    private fun getUserData() = CoroutineScope(Dispatchers.IO).launch {
        try {
            val querySnapshot = userCollectionRef
                .document(firebaseAuth.uid.toString())
                .get().await()

            userName = querySnapshot.data?.get("userName").toString()
            userPhone = querySnapshot.data?.get("userPhone").toString()

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ShipingAddressActivity, "Error fetching user data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddAddressDialog() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_add_address, null)
        bottomSheetDialog.setContentView(view)

        val addressTypeGroup = view.findViewById<RadioGroup>(R.id.addressTypeGroup)
        val addressEt = view.findViewById<EditText>(R.id.addressEt)
        val cityEt = view.findViewById<EditText>(R.id.cityEt)
        val trotroStationSpinner = view.findViewById<Spinner>(R.id.trotroStationSpinner)
        val trotroLayout = view.findViewById<LinearLayout>(R.id.trotroLayout)
        val homeLayout = view.findViewById<LinearLayout>(R.id.homeLayout)
        val saveBtn = view.findViewById<Button>(R.id.saveAddressBtn)

        // Setup trotro stations
        setupTrotroStations(trotroStationSpinner)

        // Handle address type selection
        addressTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.homeAddressRadio -> {
                    homeLayout.visibility = View.VISIBLE
                    trotroLayout.visibility = View.GONE
                }
                R.id.trotroStationRadio -> {
                    homeLayout.visibility = View.GONE
                    trotroLayout.visibility = View.VISIBLE
                }
            }
        }

        saveBtn.setOnClickListener {
            val selectedType = when (addressTypeGroup.checkedRadioButtonId) {
                R.id.homeAddressRadio -> "Home"
                R.id.trotroStationRadio -> "Trotro Station"
                else -> "Home"
            }

            val address = if (selectedType == "Home") {
                val addressText = addressEt.text.toString().trim()
                val city = cityEt.text.toString().trim()
                if (addressText.isEmpty() || city.isEmpty()) {
                    Toast.makeText(this, "Please fill all address fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                Address(
                    id = System.currentTimeMillis().toString(),
                    type = selectedType,
                    fullName = userName,
                    phone = userPhone,
                    address = addressText,
                    city = city,
                    isSelected = addressList.isEmpty()
                )
            } else {
                val selectedStation = trotroStationSpinner.selectedItem.toString()
                Address(
                    id = System.currentTimeMillis().toString(),
                    type = selectedType,
                    fullName = userName,
                    phone = userPhone,
                    address = selectedStation,
                    city = "Pickup Point",
                    isSelected = addressList.isEmpty()
                )
            }

            addAddress(address)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun setupTrotroStations(spinner: Spinner) {
        val stations = arrayOf(
            "Accra Central Station",
            "Kaneshie Station",
            "Madina Station",
            "Tema Station",
            "Kasoa Station",
            "Achimota Station",
            "Circle Station",
            "Lapaz Station",
            "Dansoman Station",
            "Adenta Station"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, stations)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun addAddress(address: Address) {
        addressList.add(address)
        addressAdapter.notifyItemInserted(addressList.size - 1)
        saveAddresses()
        updateEmptyState()
        Toast.makeText(this, "Address added successfully", Toast.LENGTH_SHORT).show()
    }

    private fun selectAddress(address: Address) {
        // Deselect all addresses
        addressList.forEach { it.isSelected = false }
        // Select the clicked address
        address.isSelected = true
        addressAdapter.notifyDataSetChanged()
        saveAddresses()
        Toast.makeText(this, "Address selected", Toast.LENGTH_SHORT).show()
    }

    private fun loadSavedAddresses() {
        // Load addresses from SharedPreferences
        // For now, we\'ll use a simple implementation
        updateEmptyState()
    }

    private fun saveAddresses() {
        // Save addresses to SharedPreferences
        // Implementation for persistence
    }

    private fun updateEmptyState() {
        if (addressList.isEmpty()) {
            emptyStateLayout.visibility = View.VISIBLE
            addressRecyclerView.visibility = View.GONE
        } else {
            emptyStateLayout.visibility = View.GONE
            addressRecyclerView.visibility = View.VISIBLE
        }
    }
}
