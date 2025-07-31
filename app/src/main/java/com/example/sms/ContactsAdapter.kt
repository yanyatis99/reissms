package com.example.sms

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.TextView

class ContactsAdapter(
    private val context: Context,
    private val contacts: List<Contact>
) : BaseAdapter() {

    override fun getCount(): Int = contacts.size

    override fun getItem(position: Int): Any = contacts[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.contact_item, parent, false)

        val contact = contacts[position]

        val nameTextView = view.findViewById<TextView>(R.id.contactName)
        val phoneTextView = view.findViewById<TextView>(R.id.contactPhone)
        val checkBox = view.findViewById<CheckBox>(R.id.contactCheckBox)

        nameTextView.text = contact.name
        phoneTextView.text = contact.phoneNumber
        checkBox.isChecked = contact.isSelected

        checkBox.setOnCheckedChangeListener { _, isChecked ->
            contact.isSelected = isChecked
        }

        view.setOnClickListener {
            contact.isSelected = !contact.isSelected
            checkBox.isChecked = contact.isSelected
        }

        return view
    }
}
