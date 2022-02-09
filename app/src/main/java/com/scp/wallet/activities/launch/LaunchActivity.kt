package com.scp.wallet.activities.launch

import android.animation.Animator
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import com.scp.wallet.activities.wallets.WalletsActivity
import com.scp.wallet.api.API
import com.scp.wallet.databinding.ActivityLaunchBinding

class LaunchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLaunchBinding

    private val launchViewModel: LaunchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLaunchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val serverSP = getSharedPreferences(SP_FILE_SETTINGS, MODE_PRIVATE)
        val host = serverSP.getString(SP_HOST, null)
        if(host == null) {
            val pickedHost = API.TRUSTED_HOSTS.random()
            API.host = pickedHost
            serverSP.edit().putString(SP_HOST, pickedHost).apply()
        } else {
            API.host = host
        }

        initViews()

    }

    private fun initViews() {

        launchViewModel.getLottieLogoAnim().observe(this) { anim ->
            binding.logoAnimation.setComposition(anim)
            binding.logoAnimation.playAnimation()
        }
        binding.logoAnimation.addAnimatorListener(object : Animator.AnimatorListener {

            override fun onAnimationRepeat(animation: Animator?) {}
            override fun onAnimationEnd(animation: Animator?) {
                val i = Intent(this@LaunchActivity, WalletsActivity::class.java)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                this@LaunchActivity.startActivity(i)
            }
            override fun onAnimationCancel(animation: Animator?) {}
            override fun onAnimationStart(animation: Animator?) {}

        })

    }

    companion object {
        //TODO rename SP_FILE_SETTINGS value to 'settings' on next breaking release
        const val SP_FILE_SETTINGS = "server"
        const val SP_CURRENCY = "currency"
        const val SP_HOST = "host"
    }

}