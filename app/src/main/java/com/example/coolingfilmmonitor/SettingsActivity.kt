package com.example.coolingfilmmonitor

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.materialswitch.MaterialSwitch

class SettingsActivity : AppCompatActivity() {

    companion object {
        var alertEnabled = false
        var alertThreshold = 28
        var useFahrenheit = false
        var resetRequested = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val thresholdInput = findViewById<EditText>(R.id.etThreshold)
        val switchAlert = findViewById<MaterialSwitch>(R.id.switchAlert)
        val switchUnit = findViewById<MaterialSwitch>(R.id.switchUnit)
        val btnReset = findViewById<Button>(R.id.btnReset)
        val btnSave = findViewById<Button>(R.id.btnSave)

        // Set current values
        thresholdInput.setText(alertThreshold.toString())
        switchAlert.isChecked = alertEnabled
        switchUnit.isChecked = useFahrenheit

        // Enable/Disable threshold input based on alert switch
        thresholdInput.isEnabled = alertEnabled

        switchAlert.setOnCheckedChangeListener { _, isChecked ->
            alertEnabled = isChecked
            thresholdInput.isEnabled = isChecked
        }

        switchUnit.setOnCheckedChangeListener { _, isChecked ->
            useFahrenheit = isChecked
        }

        btnReset.setOnClickListener {
            resetRequested = true
            Toast.makeText(this, "Data will reset", Toast.LENGTH_SHORT).show()
        }

        btnSave.setOnClickListener {
            if (alertEnabled) {
                val value = thresholdInput.text.toString()
                if (value.isNotEmpty()) {
                    alertThreshold = value.toInt()
                } else {
                    Toast.makeText(this, "Enter threshold value", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
