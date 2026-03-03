package com.familytracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.familytracker.data.PreferencesManager
import com.familytracker.databinding.ActivityEmergencyContactsBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class EmergencyContact(
    val name: String,
    val number: String
)

class EmergencyContactsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityEmergencyContactsBinding
    private lateinit var preferencesManager: PreferencesManager
    
    private var contact1: EmergencyContact? = null
    private var contact2: EmergencyContact? = null
    private var contact3: EmergencyContact? = null
    
    private var currentContactSlot = 0
    
    private val contactPickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri ->
        uri?.let { contactUri ->
            getContactInfo(contactUri)?.let { contact ->
                when (currentContactSlot) {
                    1 -> {
                        contact1 = contact
                        updateContactUI(1, contact)
                    }
                    2 -> {
                        contact2 = contact
                        updateContactUI(2, contact)
                    }
                    3 -> {
                        contact3 = contact
                        updateContactUI(3, contact)
                    }
                }
            }
        }
    }
    
    private val contactsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            openContactPicker(currentContactSlot)
        } else {
            Toast.makeText(this, "Contacts permission required to select contacts", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmergencyContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferencesManager = PreferencesManager(this)
        
        setupToolbar()
        setupClickListeners()
        loadSavedContacts()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupClickListeners() {
        binding.cardContact1.setOnClickListener {
            currentContactSlot = 1
            checkContactsPermissionAndPick()
        }
        
        binding.cardContact2.setOnClickListener {
            currentContactSlot = 2
            checkContactsPermissionAndPick()
        }
        
        binding.cardContact3.setOnClickListener {
            currentContactSlot = 3
            checkContactsPermissionAndPick()
        }
        
        binding.ivContact1Clear.setOnClickListener {
            contact1 = null
            clearContactUI(1)
        }
        
        binding.ivContact2Clear.setOnClickListener {
            contact2 = null
            clearContactUI(2)
        }
        
        binding.ivContact3Clear.setOnClickListener {
            contact3 = null
            clearContactUI(3)
        }
        
        binding.tilManualNumber.setEndIconOnClickListener {
            val number = binding.etManualNumber.text.toString().trim()
            if (number.isNotEmpty() && isValidPhoneNumber(number)) {
                addManualNumber(number)
                binding.etManualNumber.text?.clear()
            } else {
                binding.tilManualNumber.error = "Enter valid phone number with country code"
            }
        }
        
        binding.btnSave.setOnClickListener {
            saveEmergencyContacts()
        }
    }
    
    private fun checkContactsPermissionAndPick() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                openContactPicker(currentContactSlot)
            }
            else -> {
                contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }
    
    private fun openContactPicker(slot: Int) {
        currentContactSlot = slot
        contactPickerLauncher.launch(null)
    }
    
    private fun getContactInfo(contactUri: Uri): EmergencyContact? {
        var name = ""
        var phoneNumber = ""
        
        // Get contact name
        val nameCursor: Cursor? = contentResolver.query(
            contactUri, null, null, null, null
        )
        nameCursor?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    name = cursor.getString(nameIndex) ?: ""
                }
                
                val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                if (idIndex >= 0) {
                    val contactId = cursor.getString(idIndex)
                    
                    // Get phone number
                    val phoneCursor: Cursor? = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(contactId),
                        null
                    )
                    phoneCursor?.use { phone ->
                        if (phone.moveToFirst()) {
                            val phoneIndex = phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            if (phoneIndex >= 0) {
                                phoneNumber = phone.getString(phoneIndex) ?: ""
                            }
                        }
                    }
                }
            }
        }
        
        return if (name.isNotEmpty() && phoneNumber.isNotEmpty()) {
            EmergencyContact(name, normalizePhoneNumber(phoneNumber))
        } else {
            Toast.makeText(this, "Could not get contact info", Toast.LENGTH_SHORT).show()
            null
        }
    }
    
    private fun normalizePhoneNumber(number: String): String {
        return number.replace(Regex("[^+0-9]"), "")
    }
    
    private fun isValidPhoneNumber(number: String): Boolean {
        val normalized = normalizePhoneNumber(number)
        return normalized.length >= 10 && normalized.matches(Regex("^\\+?[0-9]{10,15}$"))
    }
    
    private fun addManualNumber(number: String) {
        val normalized = normalizePhoneNumber(number)
        val contact = EmergencyContact("Manual Entry", normalized)
        
        when {
            contact1 == null -> {
                contact1 = contact
                updateContactUI(1, contact)
            }
            contact2 == null -> {
                contact2 = contact
                updateContactUI(2, contact)
            }
            contact3 == null -> {
                contact3 = contact
                updateContactUI(3, contact)
            }
            else -> {
                Toast.makeText(this, "All contact slots are filled", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateContactUI(slot: Int, contact: EmergencyContact) {
        val (nameView, numberView, iconView, clearView) = getContactViews(slot)
        
        nameView.text = contact.name
        nameView.setTextColor(ContextCompat.getColor(this, android.R.color.black))
        numberView.text = contact.number
        numberView.visibility = View.VISIBLE
        iconView.setImageResource(R.drawable.ic_person)
        iconView.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_blue_dark))
        iconView.setBackgroundResource(R.drawable.circle_bg_blue)
        clearView.visibility = View.VISIBLE
    }
    
    private fun clearContactUI(slot: Int) {
        val (nameView, numberView, iconView, clearView) = getContactViews(slot)
        
        nameView.text = "Tap to select contact"
        nameView.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        numberView.text = ""
        numberView.visibility = View.GONE
        iconView.setImageResource(R.drawable.ic_add_contact)
        iconView.setColorFilter(ContextCompat.getColor(this, android.R.color.darker_gray))
        iconView.setBackgroundResource(R.drawable.circle_bg_gray)
        clearView.visibility = View.GONE
    }
    
    private fun getContactViews(slot: Int): ContactViews {
        return when (slot) {
            1 -> ContactViews(
                binding.tvContact1Name,
                binding.tvContact1Number,
                binding.ivContact1Icon,
                binding.ivContact1Clear
            )
            2 -> ContactViews(
                binding.tvContact2Name,
                binding.tvContact2Number,
                binding.ivContact2Icon,
                binding.ivContact2Clear
            )
            else -> ContactViews(
                binding.tvContact3Name,
                binding.tvContact3Number,
                binding.ivContact3Icon,
                binding.ivContact3Clear
            )
        }
    }
    
    private data class ContactViews(
        val name: TextView,
        val number: TextView,
        val icon: ImageView,
        val clear: ImageView
    )
    
    private fun loadSavedContacts() {
        lifecycleScope.launch {
            val savedNumbers = preferencesManager.authorizedNumbers.first()
            val backupPhone = preferencesManager.backupPhone.first()
            
            val allNumbers = mutableListOf<String>()
            backupPhone?.let { allNumbers.add(it) }
            allNumbers.addAll(savedNumbers)
            
            allNumbers.forEachIndexed { index, number ->
                val contact = EmergencyContact("Saved Contact ${index + 1}", number)
                when (index) {
                    0 -> {
                        contact1 = contact
                        updateContactUI(1, contact)
                    }
                    1 -> {
                        contact2 = contact
                        updateContactUI(2, contact)
                    }
                    2 -> {
                        contact3 = contact  
                        updateContactUI(3, contact)
                    }
                }
            }
        }
    }
    
    private fun saveEmergencyContacts() {
        val contacts = listOfNotNull(contact1, contact2, contact3)
        
        if (contacts.isEmpty()) {
            Toast.makeText(this, "Add at least one emergency contact", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            // Save primary contact as backup phone
            contact1?.let { 
                preferencesManager.saveBackupPhone(it.number)
            }
            
            // Save all numbers as authorized numbers
            val numbers = contacts.map { it.number }.toSet()
            preferencesManager.saveAuthorizedNumbers(numbers)
            
            Toast.makeText(
                this@EmergencyContactsActivity, 
                "Saved ${contacts.size} emergency contacts", 
                Toast.LENGTH_SHORT
            ).show()
            
            setResult(RESULT_OK)
            finish()
        }
    }
}
