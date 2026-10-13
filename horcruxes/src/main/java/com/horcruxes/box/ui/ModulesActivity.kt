package com.horcruxes.box.ui

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.horcruxes.box.R
import com.horcruxes.box.data.HorcruxRepository

class ModulesActivity : Activity() {

    private val TEXT = Color.parseColor("#D6E5D6")
    private val DIM = Color.parseColor("#7A8F7E")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modules)

        val list = findViewById<LinearLayout>(R.id.modulesList)
        for (mod in HorcruxRepository.defaultModules()) {
            list.addView(moduleRow(mod))
        }
    }

    private fun moduleRow(mod: HorcruxRepository.Module): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0C130D"))
                cornerRadius = dp(10).toFloat()
                setStroke(1, Color.parseColor("#14291B"))
            }
        }

        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val textCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        textCol.addView(TextView(this).apply {
            text = mod.name
            textSize = 15f
            setTextColor(TEXT)
            setTypeface(typeface, Typeface.BOLD)
        })
        textCol.addView(TextView(this).apply {
            text = "${mod.id} • v${mod.version}"
            textSize = 11f
            setTextColor(DIM)
        })
        head.addView(textCol, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        head.addView(Switch(this).apply {
            isChecked = HorcruxRepository.isModuleEnabled(this@ModulesActivity, mod.id)
            setOnCheckedChangeListener { _, checked ->
                HorcruxRepository.setModuleEnabled(this@ModulesActivity, mod.id, checked)
                Toast.makeText(
                    this@ModulesActivity,
                    if (checked) "${mod.name} bound." else "${mod.name} sealed.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
        card.addView(head)

        card.addView(TextView(this).apply {
            text = mod.description
            textSize = 12f
            setTextColor(DIM)
            setPadding(0, dp(6), 0, 0)
        })

        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        wrap.addView(card)
        wrap.addView(View(this).apply { minimumHeight = dp(12) })
        return wrap
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
