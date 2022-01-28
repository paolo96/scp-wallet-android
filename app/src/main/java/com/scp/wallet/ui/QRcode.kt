package com.scp.wallet.ui

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

object QRcode {

    private const val PIXEL_SIZE = 300

    fun create(data: String) : Bitmap {

        val bitMatrix = QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, PIXEL_SIZE, PIXEL_SIZE)

        val bitmap = Bitmap.createBitmap(PIXEL_SIZE, PIXEL_SIZE, Bitmap.Config.ARGB_8888)
        for (x in 0 until PIXEL_SIZE) {
            for (y in 0 until PIXEL_SIZE) {
                val fillColor = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                bitmap.setPixel(x, y, fillColor)
            }
        }

        return bitmap

    }

}