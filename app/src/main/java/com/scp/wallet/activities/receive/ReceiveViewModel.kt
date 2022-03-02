package com.scp.wallet.activities.receive

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class ReceiveViewModel(application: Application) : AndroidViewModel(application) {

    val address = MutableLiveData<String>()

}