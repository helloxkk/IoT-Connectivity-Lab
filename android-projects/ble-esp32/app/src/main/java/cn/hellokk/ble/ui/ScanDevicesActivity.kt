package cn.hellokk.ble.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.hellokk.ble.R
import cn.hellokk.ble.bluetoothcontroller.BluetoothDeviceInfo
import cn.hellokk.ble.ui.adapter.DeviceAdapter
import cn.hellokk.ble.ui.view.RippleAnimationView

/**
 * 作者: Kun on 2025/5/9.
 * 邮箱: vip@hellokk.cc.
 * 描述: 扫描设备页面
 */
class ScanDevicesActivity : AppCompatActivity() {

    private lateinit var buttonBack: ImageButton
    private lateinit var buttonHelp: ImageButton
    private lateinit var scanAnimationContainer: FrameLayout
    private lateinit var rippleAnimationView: RippleAnimationView
    private lateinit var textViewScanning: TextView
    private lateinit var textViewFoundDevices: TextView
    private lateinit var textViewNoDevices: TextView
    private lateinit var recyclerViewDevices: RecyclerView
    private lateinit var buttonScan: Button

    private var isScanning = false
    private val handler = Handler(Looper.getMainLooper())
    private val scanPeriod: Long = 15000 // 扫描持续时间 (15秒)

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val deviceList = mutableListOf<BluetoothDeviceInfo>()
    private lateinit var deviceAdapter: DeviceAdapter

    // 用于处理蓝牙权限请求结果
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            startScan()
        } else {
            Toast.makeText(this, "需要蓝牙权限才能扫描设备", Toast.LENGTH_LONG).show()
            showPermissionExplanationDialog()
        }
    }

    // 扫描文本动画
    private lateinit var scanningTextAnimation: Animation

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_devices)

        // 初始化蓝牙适配器
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        // 检查设备是否支持蓝牙
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(this, "此设备不支持蓝牙", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // 检查设备是否支持BLE
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "此设备不支持低功耗蓝牙", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        initViews()
        setupListeners()
        setupAnimations()
    }

    private fun initViews() {
        buttonBack = findViewById(R.id.buttonBack)
        buttonHelp = findViewById(R.id.buttonHelp)
        scanAnimationContainer = findViewById(R.id.scanAnimationContainer)
        rippleAnimationView = findViewById(R.id.rippleAnimationView)
        textViewScanning = findViewById(R.id.textViewScanning)
        textViewFoundDevices = findViewById(R.id.textViewFoundDevices)
        textViewNoDevices = findViewById(R.id.textViewNoDevices)
        recyclerViewDevices = findViewById(R.id.recyclerViewDevices)
        buttonScan = findViewById(R.id.buttonScan)

        // 设置设备适配器
        recyclerViewDevices.layoutManager = LinearLayoutManager(this)
        deviceAdapter = DeviceAdapter(deviceList) { device ->
            connectToDevice(device)
        }
        recyclerViewDevices.adapter = deviceAdapter

        // 初始状态下，设备列表和搜索动画都可见
        // 但搜索动画最初不活跃
        scanAnimationContainer.visibility = View.VISIBLE
        recyclerViewDevices.visibility = View.VISIBLE
        textViewNoDevices.visibility = View.GONE
    }

    private fun setupListeners() {
        buttonBack.setOnClickListener {
            finish()
        }

        buttonHelp.setOnClickListener {
            showHelpDialog()
        }

        buttonScan.setOnClickListener {
            if (isScanning) {
                stopScan()
            } else {
                checkPermissionsAndStartScan()
            }
        }
    }

    private fun setupAnimations() {
        // 创建扫描文本动画
        scanningTextAnimation = AlphaAnimation(1.0f, 0.5f).apply {
            duration = 800
            repeatCount = Animation.INFINITE
            repeatMode = Animation.REVERSE
        }
    }

    private fun showHelpDialog() {
        AlertDialog.Builder(this)
            .setTitle("如何连接设备")
            .setMessage("1. 确保您的蓝牙设备已开启并处于可发现模式\n\n" +
                    "2. 点击\"开始扫描\"按钮搜索附近的设备\n\n" +
                    "3. 在设备列表中选择您要连接的设备\n\n" +
                    "4. 如果设备要求配对，请按照提示操作")
            .setPositiveButton("我知道了", null)
            .show()
    }

    private fun checkPermissionsAndStartScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED -> {
                    startScan()
                }
                else -> {
                    // 请求权限
                    requestPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT
                        )
                    )
                }
            }
        } else {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startScan()
                }
                else -> {
                    // 请求权限
                    requestPermissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                    )
                }
            }
        }
    }

    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle("需要权限")
            .setMessage("扫描蓝牙设备需要位置权限。请在设置中授予权限。")
            .setPositiveButton("好的", null)
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun startScan() {
        // 检查蓝牙是否开启
        if (!bluetoothAdapter.isEnabled) {
            // 提示用户开启蓝牙
            Toast.makeText(this, "请开启蓝牙", Toast.LENGTH_SHORT).show()
            return
        }

        // 清空之前的设备列表
        deviceList.clear()
        deviceAdapter.notifyDataSetChanged()

        // 显示扫描动画和初始状态
        showScanningState()

        // 设置扫描参数
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        // 开始扫描
        try {
            bluetoothAdapter.bluetoothLeScanner.startScan(null, scanSettings, leScanCallback)
            isScanning = true
            buttonScan.text = "停止扫描"

            // 启动水波纹动画
            rippleAnimationView.startRippleAnimation()

            // 启动文本动画
            textViewScanning.startAnimation(scanningTextAnimation)

            // 设置扫描超时
            handler.postDelayed({
                stopScan()
            }, scanPeriod)

        } catch (e: Exception) {
            Log.e("ScanDevicesActivity", "Start scan failed", e)
            Toast.makeText(this, "扫描失败: ${e.message}", Toast.LENGTH_SHORT).show()
            stopScan()
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        if (isScanning) {
            try {
                bluetoothAdapter.bluetoothLeScanner.stopScan(leScanCallback)
            } catch (e: Exception) {
                Log.e("ScanDevicesActivity", "Stop scan failed", e)
            }

            isScanning = false
            buttonScan.text = "开始扫描"

            // 停止动画
            rippleAnimationView.stopRippleAnimation()
            textViewScanning.clearAnimation()

            // 更新UI状态
            updateScanResult()
        }
    }

    private fun showScanningState() {
        // 显示扫描动画
        scanAnimationContainer.visibility = View.VISIBLE
        textViewScanning.visibility = View.VISIBLE

        // 更新设备列表标题
        textViewFoundDevices.text = "正在搜索设备..."

        // 确保设备列表可见（实时显示）
        recyclerViewDevices.visibility = View.VISIBLE
        textViewNoDevices.visibility = View.GONE
    }

    private fun updateScanResult() {
        // 扫描结束后更新UI
        if (deviceList.isEmpty()) {
            textViewFoundDevices.text = "未发现设备"
            textViewNoDevices.visibility = View.VISIBLE
            recyclerViewDevices.visibility = View.GONE
        } else {
            textViewFoundDevices.text = "发现的设备 (${deviceList.size})"
            textViewNoDevices.visibility = View.GONE
            recyclerViewDevices.visibility = View.VISIBLE
        }

        // 扫描结束但保持动画容器可见，只是停止动画
        scanAnimationContainer.visibility = if (deviceList.isEmpty()) View.VISIBLE else View.GONE
        textViewScanning.visibility = View.GONE
    }

    @SuppressLint("MissingPermission")
    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)

            val device = result.device
            val deviceName = device.name ?: ""
            val deviceAddress = device.address
            val rssi = result.rssi

            // 即使没有名称也添加设备，但用地址作为显示名称
            if (!isDeviceAlreadyAdded(deviceAddress)) {
                val deviceInfo = BluetoothDeviceInfo(
                    name = if (deviceName.isNotEmpty()) deviceName else "未知设备_${deviceAddress.takeLast(5)}",
                    address = deviceAddress,
                    rssi = rssi
                )

                runOnUiThread {
                    // 添加新发现的设备
                    deviceList.add(deviceInfo)

                    // 按信号强度排序
                    deviceList.sortByDescending { it.rssi }

                    // 通知适配器更新（实时显示）
                    deviceAdapter.notifyDataSetChanged()

                    // 确保设备列表可见
                    if (deviceList.size == 1) {
                        // 第一个设备被发现时，更新标题
                        textViewFoundDevices.text = "发现的设备 (1)"
                        textViewNoDevices.visibility = View.GONE
                        recyclerViewDevices.visibility = View.VISIBLE
                    } else {
                        // 更新发现设备数量
                        textViewFoundDevices.text = "发现的设备 (${deviceList.size})"
                    }
                }
            } else {
                // 设备已存在，更新RSSI值
                val index = deviceList.indexOfFirst { it.address == deviceAddress }
                if (index != -1) {
                    val updatedDevice = deviceList[index].copy(rssi = rssi)
                    deviceList[index] = updatedDevice

                    runOnUiThread {
                        // 只更新这一项，更高效
                        deviceAdapter.notifyItemChanged(index)
                    }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e("ScanDevicesActivity", "Scan failed with error: $errorCode")
            runOnUiThread {
                Toast.makeText(
                    this@ScanDevicesActivity,
                    "扫描失败，错误码: $errorCode",
                    Toast.LENGTH_SHORT
                ).show()
                stopScan()
            }
        }
    }

    private fun isDeviceAlreadyAdded(address: String): Boolean {
        return deviceList.any { it.address == address }
    }

    private fun connectToDevice(device: BluetoothDeviceInfo) {
        // 停止扫描
        if (isScanning) {
            stopScan()
        }

        Toast.makeText(this, "正在连接到: ${device.name}", Toast.LENGTH_SHORT).show()

        // 跳转到设备控制页面
        val intent = Intent(this, DeviceControlActivity::class.java).apply {
            putExtra("DEVICE_NAME", device.name)
            putExtra("DEVICE_ID", device.address)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 确保停止扫描
        stopScan()
        // 移除所有回调
        handler.removeCallbacksAndMessages(null)
    }
}