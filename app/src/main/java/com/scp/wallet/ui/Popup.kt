package com.scp.wallet.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.scp.wallet.R
import com.scp.wallet.exceptions.WrongWalletPasswordException
import com.scp.wallet.wallet.Wallet


object Popup {

    fun showSimple(title: String, message: String, context: Context, callback: (() -> Unit)? = null) {

        val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view: View = inflater.inflate(R.layout.alert_custom, null)

        val builder = AlertDialog.Builder(context)
        val alert = builder.create()
        alert.setView(view)
        alert.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        view.findViewById<Button>(R.id.alertButtonNegative).visibility = View.GONE
        view.findViewById<TextView>(R.id.alertTitle).text = title
        view.findViewById<TextView>(R.id.alertDescription).text = message
        view.findViewById<Button>(R.id.alertButtonPositive).setOnClickListener {
            alert.dismiss()
            callback?.let { it() }
        }

        alert.show()
    }

    fun showChoice(title: String, message: String, context: Context, callback: (Boolean) -> Unit) {

        val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view: View = inflater.inflate(R.layout.alert_custom, null)

        val builder = AlertDialog.Builder(context)
        val alert = builder.create()
        alert.setView(view)
        alert.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        view.findViewById<TextView>(R.id.alertTitle).text = title
        view.findViewById<TextView>(R.id.alertDescription).text = message

        var positive = false
        view.findViewById<Button>(R.id.alertButtonPositive).setOnClickListener {
            positive = true
            callback(true)
            alert.dismiss()
        }
        view.findViewById<Button>(R.id.alertButtonNegative).setOnClickListener {
            alert.dismiss()
        }
        alert.setOnDismissListener {
            if(!positive) callback(false)
        }

        alert.show()
    }

    fun showUnlockWallet(wallet: Wallet, context: Context, newPassword: Boolean = false, callback: (Boolean) -> Unit) {

        val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view: View = inflater.inflate(R.layout.alert_custom, null)

        val builder = AlertDialog.Builder(context)
        val alert = builder.create()
        alert.setView(view)
        alert.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val editPassword = view.findViewById<EditText>(R.id.alertInput)
        editPassword.visibility = View.VISIBLE
        view.findViewById<TextView>(R.id.alertInputLabel).visibility = View.VISIBLE
        view.findViewById<TextView>(R.id.alertTitle).text = context.getString(R.string.popup_title_unlock_wallet)
        view.findViewById<TextView>(R.id.alertDescription).text = context.getString(R.string.popup_description_unlock_wallet)

        var positive = false
        view.findViewById<Button>(R.id.alertButtonPositive).setOnClickListener {
            val insertedPassword = editPassword.text.toString()
            try {
                wallet.unlock(insertedPassword)
                positive = true
                callback(true)
                alert.dismiss()
            } catch (e: WrongWalletPasswordException) {
                editPassword.setText("")
                editPassword.hint = context.getString(R.string.wrong_password)
            }
        }
        view.findViewById<Button>(R.id.alertButtonNegative).setOnClickListener {
            alert.dismiss()
        }
        alert.setOnDismissListener {
            if(!positive) callback(false)
        }
        if(newPassword) {
            val newEditPassword = view.findViewById<EditText>(R.id.alertInputTwo)
            newEditPassword.visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.alertInputTwoLabel).visibility = View.VISIBLE
        }

        alert.show()

    }

    fun showChangePasswordWallet(wallet: Wallet, context: Context, callback: (Boolean) -> Unit) {

        val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view: View = inflater.inflate(R.layout.alert_custom, null)

        val builder = AlertDialog.Builder(context)
        val alert = builder.create()
        alert.setView(view)
        alert.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val editPassword = view.findViewById<EditText>(R.id.alertInput)
        editPassword.visibility = View.VISIBLE
        view.findViewById<TextView>(R.id.alertInputLabel).visibility = View.VISIBLE
        val newEditPassword = view.findViewById<EditText>(R.id.alertInputTwo)
        newEditPassword.visibility = View.VISIBLE
        view.findViewById<TextView>(R.id.alertInputTwoLabel).visibility = View.VISIBLE

        view.findViewById<TextView>(R.id.alertTitle).text = context.getString(R.string.popup_title_change_password)
        view.findViewById<TextView>(R.id.alertDescription).text = context.getString(R.string.popup_description_change_password)

        var positive = false
        view.findViewById<Button>(R.id.alertButtonPositive).setOnClickListener {
            val insertedPassword = editPassword.text.toString()
            try {
                wallet.changePassword(insertedPassword, newEditPassword.text.toString())
                positive = true
                callback(true)
                alert.dismiss()
            } catch (e: WrongWalletPasswordException) {
                editPassword.setText("")
                editPassword.hint = context.getString(R.string.wrong_password)
            }
        }
        view.findViewById<Button>(R.id.alertButtonNegative).setOnClickListener {
            alert.dismiss()
        }
        alert.setOnDismissListener {
            if(!positive) callback(false)
        }

        alert.show()

    }

}