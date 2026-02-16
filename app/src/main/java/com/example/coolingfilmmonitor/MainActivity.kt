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
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlin.random.Random

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
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)

        tempGauge = findViewById(R.id.tempGauge)
        tvTemperature = findViewById(R.id.tvTemperature)
        tvCoolingStatus = findViewById(R.id.tvCoolingStatus)
        heroCard = findViewById(R.id.heroCard)
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus)
        lineChart = findViewById(R.id.lineChart)
        gridTile2 = findViewById(R.id.gridTile2)
        gridText2 = findViewById(R.id.gridText2)

        setupLineChart()

        gridTile2.setOnClickListener {
            gridText2.text = getString(R.string.data_logged, currentTemp)
        }

        heroCard.setOnClickListener {
            if (minTemp != Int.MAX_VALUE && maxTemp != Int.MIN_VALUE) {
                tvCoolingStatus.text =
                    getString(R.string.min_max, minTemp, maxTemp)
            }
        }

        handler.post(tempUpdater)
        handler.post(chartUpdater)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()

        if (SettingsActivity.resetRequested) {
            minTemp = Int.MAX_VALUE
            maxTemp = Int.MIN_VALUE
            temperatureEntries.clear()
            lineChart.clear()
            SettingsActivity.resetRequested = false
        }
    }

    private val tempUpdater = object : Runnable {
        override fun run() {

            val newTemp = Random.nextInt(20, 35)

            if (newTemp < minTemp) minTemp = newTemp
            if (newTemp > maxTemp) maxTemp = newTemp

            animateTemperature(currentTemp, newTemp)
            currentTemp = newTemp

            if (SettingsActivity.alertEnabled) {

                if (newTemp >= SettingsActivity.alertThreshold) {

                    tvCoolingStatus.text = getString(R.string.heating)

                    if (!alertAlreadyShown) {
                        showInAppAlert()
                        alertAlreadyShown = true
                    }

                } else {
                    tvCoolingStatus.text = getString(R.string.cooling_stable)
                    alertAlreadyShown = false
                }

            } else {
                tvCoolingStatus.text = getString(R.string.monitoring)
            }

            handler.postDelayed(this, tempUpdateInterval)
        }
    }


    private fun showInAppAlert() {
        com.google.android.material.snackbar.Snackbar
            .make(
                findViewById(android.R.id.content),
                "⚠ Temperature exceeded threshold!",
                com.google.android.material.snackbar.Snackbar.LENGTH_LONG
            )
            .setBackgroundTint(ContextCompat.getColor(this, R.color.alert_red))
            .setTextColor(ContextCompat.getColor(this, android.R.color.white))
            .show()
    }


    private fun animateTemperature(from: Int, to: Int) {

        val displayTemp = if (SettingsActivity.useFahrenheit) {
            (to * 9 / 5) + 32
        } else {
            to
        }

        val unit = if (SettingsActivity.useFahrenheit) "°F" else "°C"

        val progressAnimator = ValueAnimator.ofInt(from, to).apply {
            duration = 1500
            addUpdateListener {
                tempGauge.progress = it.animatedValue as Int
            }
        }
        progressAnimator.start()

        val colorAnimator = ValueAnimator.ofObject(
            ArgbEvaluator(),
            getColorForTemp(from),
            getColorForTemp(to)
        ).apply {
            duration = 1500
            addUpdateListener {
                val color = it.animatedValue as Int
                tempGauge.setIndicatorColor(color)
                tvTemperature.setShadowLayer(8f, 0f, 0f, color)
            }
        }
        colorAnimator.start()

        tvTemperature.text = "$displayTemp$unit"
    }

    private fun getColorForTemp(temp: Int): Int {
        return when {
            temp < 25 ->
                ContextCompat.getColor(this, R.color.status_blue)
            temp in 25..30 ->
                ContextCompat.getColor(this, R.color.warning_yellow)
            else ->
                ContextCompat.getColor(this, R.color.alert_red)
        }
    }

    private val chartUpdater = object : Runnable {
        override fun run() {
            val newValue = Random.nextInt(20, 35).toFloat()
            temperatureEntries.add(Entry(chartTime, newValue))
            chartTime += 1f

            if (temperatureEntries.size > 20) {
                temperatureEntries.removeAt(0)
            }

            val dataSet = LineDataSet(temperatureEntries, "Temp").apply {
                lineWidth = 2f
                setDrawCircles(false)
                color = ContextCompat.getColor(this@MainActivity, R.color.status_blue)
                valueTextColor = ContextCompat.getColor(
                    this@MainActivity,
                    android.R.color.white
                )
            }

            lineChart.data = LineData(dataSet)

            lineChart.legend.textColor =
                ContextCompat.getColor(this@MainActivity, android.R.color.white)

            lineChart.invalidate()

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
    }

    private fun setupLineChart() {
        lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(false)

            legend.textColor =
                ContextCompat.getColor(this@MainActivity, android.R.color.white)

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.textColor =
                ContextCompat.getColor(this@MainActivity, android.R.color.white)

            axisLeft.textColor =
                ContextCompat.getColor(this@MainActivity, android.R.color.white)

            axisRight.isEnabled = false
            axisLeft.axisMinimum = 0f
            axisLeft.axisMaximum = 40f
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

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(tempUpdater)
        handler.removeCallbacks(chartUpdater)
    }
}
