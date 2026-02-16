package com.example.coolingfilmmonitor

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private var bluetoothAdapter: android.bluetooth.BluetoothAdapter? = null
    private val requestEnableBt = 1
    private var isScanning = false
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val SCAN_PERIOD: Long = 10000
    private val requestBluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            // This is where you will trigger the sensor search
            Toast.makeText(this, "Permissions Granted!", Toast.LENGTH_SHORT).show()
            startScanning()
        } else {
            Toast.makeText(this, "Permissions Denied", Toast.LENGTH_SHORT).show()
        }
    }
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
            // 1. Check if we are on Android 12 or higher (API 31+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // 2. Launch the permission pop-up you created at Line 20
                requestBluetoothPermissionLauncher.launch(arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ))
            } else {
                // 3. For older phones, just try to start the connection
                startScanning()
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    @SuppressLint("MissingPermission")
    override fun onPause() {
        super.onPause()
        if (isScanning) {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(leScanCallback)
            isScanning = false
        }
    }
    @SuppressLint("MissingPermission")
    private fun startScanning() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            Toast.makeText(this, "Scanner not available", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isScanning) {
            handler.postDelayed({
                isScanning = false
                scanner.stopScan(leScanCallback)
                Toast.makeText(this, "Scan Stopped", Toast.LENGTH_SHORT).show()
            }, SCAN_PERIOD)

            isScanning = true
            scanner.startScan(leScanCallback)
            Toast.makeText(this, "Searching for Cooling Sensor...", Toast.LENGTH_SHORT).show()
        }
    }

    private val leScanCallback: android.bluetooth.le.ScanCallback = object : android.bluetooth.le.ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, @SuppressLint("MissingPermission") result: android.bluetooth.le.ScanResult) {
            val deviceName: String? = result.device.name
            if (deviceName != null && deviceName.contains("CoolingFilm")) {
                Toast.makeText(this@MainActivity, "Sensor Found: $deviceName", Toast.LENGTH_SHORT).show()
                isScanning = false
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(leScanCallback)
            }
        }
    }
}