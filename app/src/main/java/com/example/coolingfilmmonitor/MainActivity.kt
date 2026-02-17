package com.example.coolingfilmmonitor

import android.Manifest
import android.annotation.SuppressLint
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
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
import com.google.android.material.snackbar.Snackbar
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    // UI
    private lateinit var tempGauge: CircularProgressIndicator
    private lateinit var tvTemperature: TextView
    private lateinit var tvCoolingStatus: TextView
    private lateinit var heroCard: MaterialCardView
    private lateinit var tvConnectionStatus: TextView
    private lateinit var lineChart: LineChart
    private lateinit var gridTile2: MaterialCardView
    private lateinit var gridText2: TextView
    private lateinit var btnConnect: Button

    // Bluetooth
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var isScanning = false
    private val SCAN_PERIOD: Long = 10000

    // Temperature + Chart
    private var alertAlreadyShown = false
    private val handler = Handler(Looper.getMainLooper())
    private val tempUpdateInterval = 2000L
    private val chartUpdateInterval = 1000L

    private val temperatureEntries = mutableListOf<Entry>()
    private var chartTime = 0f
    private var currentTemp = 20
    private var minTemp = Int.MAX_VALUE
    private var maxTemp = Int.MIN_VALUE

    // Permission launcher
    private val requestBluetoothPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) startScanning()
            else Toast.makeText(this, "Permissions Denied", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

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
        btnConnect = findViewById(R.id.btnConnect)

        setupLineChart()

        // CONNECT BUTTON (from first code)
        btnConnect.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestBluetoothPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
            } else {
                startScanning()
            }
        }

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

    // TEMP LOGIC (from second code)
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
        Snackbar.make(
            findViewById(android.R.id.content),
            "⚠ Temperature exceeded threshold!",
            Snackbar.LENGTH_LONG
        )
            .setBackgroundTint(ContextCompat.getColor(this, R.color.alert_red))
            .setTextColor(ContextCompat.getColor(this, android.R.color.white))
            .show()
    }

    private fun animateTemperature(from: Int, to: Int) {

        val displayTemp =
            if (SettingsActivity.useFahrenheit) (to * 9 / 5) + 32 else to
        val unit =
            if (SettingsActivity.useFahrenheit) "°F" else "°C"

        ValueAnimator.ofInt(from, to).apply {
            duration = 1500
            addUpdateListener {
                tempGauge.progress = it.animatedValue as Int
            }
            start()
        }

        ValueAnimator.ofObject(
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
            start()
        }

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

    // GRAPH (white text kept)
    private val chartUpdater = object : Runnable {
        override fun run() {

            val newValue = Random.nextInt(20, 35).toFloat()
            temperatureEntries.add(Entry(chartTime, newValue))
            chartTime += 1f

            if (temperatureEntries.size > 20)
                temperatureEntries.removeAt(0)

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
            lineChart.invalidate()

            handler.postDelayed(this, chartUpdateInterval)
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

    // BLE SCANNING
    @SuppressLint("MissingPermission")
    private fun startScanning() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return

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

    private val leScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {

            val deviceName = result.device.name ?: return

            if (deviceName.contains("CoolingFilm")) {
                Toast.makeText(
                    this@MainActivity,
                    "Sensor Found: $deviceName",
                    Toast.LENGTH_SHORT
                ).show()

                isScanning = false
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(this)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(tempUpdater)
        handler.removeCallbacks(chartUpdater)
    }
}
