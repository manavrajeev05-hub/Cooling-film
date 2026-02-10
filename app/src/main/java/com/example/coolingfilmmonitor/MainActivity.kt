package com.example.coolingfilmmonitor

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private var bluetoothAdapter: android.bluetooth.BluetoothAdapter? = null
    private val REQUEST_ENABLE_BT = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        // Connects the app to the phone's Bluetooth chip
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

// TODO: When sensor arrives, add the scan and data reception logic here
        val btnConnect = findViewById<android.widget.Button>(R.id.btnConnect)
        btnConnect.setOnClickListener {
            android.widget.Toast.makeText(this, "Connecting to Sensor...", android.widget.Toast.LENGTH_SHORT).show()
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}