package com.scp.wallet.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.widget.GridLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.scp.wallet.R
import com.scp.wallet.utils.capitalized
import com.scp.wallet.utils.px

object SeedInterface {


    //We draw the words programmatically to ensure that every word is visible without the need
    //to scroll and thus preventing the user from missing any word
    fun drawSeed(seed: String, container: GridLayout, context: Context) {

        container.post {

            val words = seed.split(" ")
            val rowNumber = if(words.size == 29) 8 else if(words.size == 28) 7 else return@post
            container.rowCount = rowNumber

            var background: Drawable? = ResourcesCompat.getDrawable(context.resources, R.drawable.edittext_background, context.theme)
            val paddingH = 7.px
            var textSize = 13f
            val containerHeight = container.height
            var cellHeight = 30.px
            var cellMargin = 0
            if(cellHeight <= containerHeight/rowNumber) {
                cellMargin = containerHeight/rowNumber - cellHeight
            } else {
                //In case there isn't much space available we try to save space
                cellHeight = containerHeight/rowNumber
                background = null
                textSize = 10f
            }
            for(i in words.indices) {

                val layoutParams = if(i == words.size-1 && words.size == 29) {
                    GridLayout.LayoutParams(GridLayout.spec(i/4), GridLayout.spec(i%4, 4, 1f))
                } else {
                    GridLayout.LayoutParams(GridLayout.spec(i/4), GridLayout.spec(i%4, 1f))
                }
                layoutParams.width = GridLayout.LayoutParams.WRAP_CONTENT
                layoutParams.setGravity(Gravity.CENTER_HORIZONTAL)
                layoutParams.height = cellHeight
                layoutParams.bottomMargin = cellMargin

                val wordTextView = TextView(context)
                wordTextView.layoutParams = layoutParams
                wordTextView.background = background
                wordTextView.setPadding(paddingH,0,paddingH,0)
                wordTextView.textSize = textSize
                wordTextView.gravity = Gravity.CENTER
                val text = "${i+1}. ${words[i].capitalized}"
                wordTextView.text = text
                container.addView(wordTextView)

            }

        }


    }

}