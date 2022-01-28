package com.scp.wallet.activities.launch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import com.scp.wallet.R

class LaunchViewModel(application: Application) : AndroidViewModel(application) {

    private val lottieLogoAnim = MutableLiveData<LottieComposition>().apply {
        value = LottieCompositionFactory.fromRawResSync(getApplication(), R.raw.logo).value
    }
    fun getLottieLogoAnim(): LiveData<LottieComposition> { return lottieLogoAnim }

}