package com.scp.wallet.ui

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.widget.GridLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.scp.wallet.R
import com.scp.wallet.utils.capitalized
import com.scp.wallet.utils.px
import com.scp.wallet.utils.sp


object SeedInterface {

    //We draw the words programmatically to ensure that every word is visible without the need
    //to scroll and thus preventing the user from missing any word
    fun drawSeed(seed: String, container: GridLayout, context: Context) {

        container.post {

            val words = seed.split(" ")
            val rowNumber = if(words.size == 29) 8 else if(words.size == 28) 7 else return@post
            container.rowCount = rowNumber
            container.clipChildren = false

            val containerHeight = container.height
            val containerWidth = container.width

            var background: Drawable? = ResourcesCompat.getDrawable(context.resources, R.drawable.edittext_background, context.theme)
            var marginH = 7.px
            var textSize = 13f
            var cellHeight = 30.px
            var cellMargin = 0
            if(cellHeight <= containerHeight/rowNumber) {
                cellMargin = containerHeight/rowNumber - cellHeight
            } else {
                //In case there isn't enough vertical space available we try to save space
                cellHeight = containerHeight/rowNumber
                background = null
                textSize = 10f
            }

            val maxWordWidth = maxWidthOfSeedWord(words, textSize)+marginH*2
            //In case there isn't enough horizontal space available we try to save space
            //Could be improved by reducing only the long words
            if(containerWidth/4 - maxWordWidth < -50) {
                textSize = 9f
                marginH = 0
                background = null
            } else if(containerWidth/4 - maxWordWidth < -10) {
                textSize = 11f
                marginH = 3.px
            } else if(containerWidth/4 - maxWordWidth < 10) {
                textSize = 12f
                marginH = 4.px
            }
            for(i in words.indices) {

                var layoutParams =GridLayout.LayoutParams(GridLayout.spec(i/4), GridLayout.spec(i%4, 1f))
                if(i == words.size-1 && words.size == 29) {
                    layoutParams = GridLayout.LayoutParams(GridLayout.spec(i/4), GridLayout.spec(i%4, 4, 1f))
                    layoutParams.width = containerWidth/4
                    layoutParams.setGravity(Gravity.CENTER)
                } else {
                    layoutParams.width = 0
                    layoutParams.setGravity(Gravity.FILL)
                }
                layoutParams.height = cellHeight
                layoutParams.bottomMargin = cellMargin
                layoutParams.marginStart = marginH
                layoutParams.marginEnd = marginH

                val wordTextView = TextView(context)
                wordTextView.layoutParams = layoutParams
                wordTextView.background = background
                wordTextView.textSize = textSize
                wordTextView.gravity = Gravity.CENTER
                val text = "${i+1}. ${words[i].capitalized}"
                wordTextView.text = text
                container.addView(wordTextView)

            }

        }


    }

    private fun maxWidthOfSeedWord(words: List<String>, textSize: Float) : Float {
        val mPaint = Paint()
        mPaint.textSize = textSize.sp

        var maxWidthText = 0f
        for(wI in words.indices) {
            val testText = "${wI+1}. ${words[wI].capitalized}"
            val wL = mPaint.measureText(testText, 0, testText.length)
            if(wL > maxWidthText) {
                maxWidthText = wL
            }
        }
        return maxWidthText

    }

}