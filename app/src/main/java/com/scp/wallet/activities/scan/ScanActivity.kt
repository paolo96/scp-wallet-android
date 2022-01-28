package com.scp.wallet.activities.scan

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.scp.wallet.activities.receive.ReceiveActivity
import com.scp.wallet.databinding.ActivityScanBinding
import com.scp.wallet.exceptions.InvalidUnlockHashException
import com.scp.wallet.scp.UnlockHash
import com.scp.wallet.ui.Popup
import java.util.concurrent.Executor

class ScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanBinding

    private lateinit var scanner: BarcodeScanner
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var imageAnalysis: ImageAnalysis
    private lateinit var preview: Preview

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        scanner = BarcodeScanning.getClient(options)

        preview = Preview.Builder().build()
        preview.setSurfaceProvider(binding.cameraView.surfaceProvider)

        if(ContextCompat.checkSelfPermission(this@ScanActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA),CAMERA_PERMISSION_REQUEST)
        }

    }

    private fun foundQrCode(qrcodes: List<Barcode>) {
        for(qr in qrcodes) {
            qr.rawValue?.let { value ->
                try {
                    val unlockHash = UnlockHash.fromString(value)
                    val returnIntent = Intent()
                    returnIntent.putExtra(IE_ADDRESS, value)
                    returnIntent.putExtra(IE_UNLOCK_HASH, unlockHash)
                    setResult(RESULT_OK, returnIntent)
                    finish()
                } catch (e: InvalidUnlockHashException) {
                    Popup.showSimple("Invalid QR code", "This QR code does not contain a valid SCP address.", this) {
                        cameraProviderFuture.get().bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, imageAnalysis, preview)
                    }
                    cameraProviderFuture.get().unbindAll()
                }
            }
        }
    }

    private fun startCamera() {
        val executor = ContextCompat.getMainExecutor(this)

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        imageAnalysis = imageAnalysis(executor)

        cameraProviderFuture.addListener({

            val cameraProvider = cameraProviderFuture.get()

            try {

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, imageAnalysis, preview)

            } catch(exc: Exception) {
                exc.printStackTrace()
            }

        }, executor)

    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun imageAnalysis(executor: Executor) : ImageAnalysis {

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(executor, { imageProxy ->

            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        if(barcodes.size > 0) {
                            foundQrCode(barcodes)
                        }
                        imageProxy.close()
                    }
                    .addOnFailureListener {
                        imageProxy.close()
                    }
            }

        })

        return imageAnalysis
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Popup.showSimple("Camera permission not granted", "The permission to use the camera has not been granted to SCP wallet. You can manage permissions in your phone settings.", this) {
                    onBackPressed()
                }
            }
        }
    }

    companion object {
        const val CAMERA_PERMISSION_REQUEST = 1001
        const val IE_UNLOCK_HASH = "unlock-hash"
        const val IE_ADDRESS = "address"
    }

}