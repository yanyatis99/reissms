package com.example.sms

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var contactsListView: ListView
    private lateinit var messageEditText: EditText
    private lateinit var selectContactsButton: Button
    private lateinit var sendButton: Button
    private lateinit var selectAllCheckBox: CheckBox

    private var contactsList = mutableListOf<Contact>()
    private lateinit var contactsAdapter: ContactsAdapter

    private val CONTACTS_PERMISSION_REQUEST = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupClickListeners()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                CONTACTS_PERMISSION_REQUEST
            )
        }
    }

    private fun initViews() {
        contactsListView = findViewById(R.id.contactsListView)
        messageEditText = findViewById(R.id.messageEditText)
        selectContactsButton = findViewById(R.id.selectContactsButton)
        sendButton = findViewById(R.id.sendButton)
        selectAllCheckBox = findViewById(R.id.selectAllCheckBox)

        contactsAdapter = ContactsAdapter(this, contactsList)
        contactsListView.adapter = contactsAdapter
    }

    private fun setupClickListeners() {
        selectContactsButton.setOnClickListener {
            loadContacts()
        }

        sendButton.setOnClickListener {
            sendBulkMessages()
        }

        selectAllCheckBox.setOnCheckedChangeListener { _, isChecked ->
            contactsList.forEach { it.isSelected = isChecked }
            contactsAdapter.notifyDataSetChanged()
        }
    }

    private fun loadContacts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Please grant contacts permission", Toast.LENGTH_SHORT).show()
            return
        }

        contactsList.clear()

        val cursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val name = it.getString(nameIndex) ?: "Unknown"
                val number = it.getString(numberIndex)?.replace("[^+\\d]".toRegex(), "") ?: ""

                if (number.isNotEmpty()) {
                    contactsList.add(Contact(name, number, false))
                }
            }
        }

        contactsAdapter.notifyDataSetChanged()
        Toast.makeText(this, "Loaded ${contactsList.size} contacts", Toast.LENGTH_SHORT).show()
    }

    private fun sendBulkMessages() {
        val message = messageEditText.text.toString().trim()
        if (message.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedContacts = contactsList.filter { it.isSelected }
        if (selectedContacts.isEmpty()) {
            Toast.makeText(this, "Please select at least one contact", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isWhatsAppInstalled()) {
            Toast.makeText(this, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Starting to send ${selectedContacts.size} messages...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.Main).launch {
            for ((index, contact) in selectedContacts.withIndex()) {
                sendWhatsAppMessage(contact.phoneNumber, message)

                if (index < selectedContacts.size - 1) {
                    delay(2000)
                }
            }
            Toast.makeText(this@MainActivity, "All messages processed!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendWhatsAppMessage(phoneNumber: String, message: String) {
        try {
            val formattedNumber = formatPhoneNumber(phoneNumber)
            val url = "https://api.whatsapp.com/send?phone=$formattedNumber&text=${Uri.encode(message)}"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                setPackage("com.whatsapp")
            }

            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error sending message to $phoneNumber", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatPhoneNumber(phoneNumber: String): String {
        var formatted = phoneNumber.replace("[^+\\d]".toRegex(), "")
        if (!formatted.startsWith("+")) {
            formatted = "+90$formatted" // Türkiye için +90
        }
        return formatted
    }

    private fun isWhatsAppInstalled(): Boolean {
        return try {
            packageManager.getPackageInfo("com.whatsapp", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CONTACTS_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission granted! You can now load contacts.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Permission denied. Cannot access contacts.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
