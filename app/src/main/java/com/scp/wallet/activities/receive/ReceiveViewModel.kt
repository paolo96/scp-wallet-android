package com.scp.wallet.activities.receive

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.scp.wallet.scp.Transaction
import com.scp.wallet.wallet.Wallet

class ReceiveViewModel(application: Application) : AndroidViewModel(application) {

    val address = MutableLiveData<String>()

}