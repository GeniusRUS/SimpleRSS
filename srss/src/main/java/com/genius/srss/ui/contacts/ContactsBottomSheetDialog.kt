package com.genius.srss.ui.contacts

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.genius.srss.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ContactsBottomSheetDialog : BottomSheetDialogFragment(), View.OnClickListener {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_contacts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.email).setOnClickListener(this)
        view.findViewById<TextView>(R.id.github).setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.email -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("mailto:${v.context.getString(R.string.email_link)}"))
                startActivity(Intent.createChooser(intent, getString(R.string.open_with)))
            }
            R.id.github -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(v.context.getString(R.string.github_link)))
                startActivity(Intent.createChooser(intent, getString(R.string.open_with)))
            }
        }
    }
}