package cn.hellokk.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.hellokk.ble.bluetoothcontroller.BluetoothDeviceInfo
import cn.hellokk.ble.ui.DeviceControlActivity
import cn.hellokk.ble.ui.ScanDevicesActivity
import cn.hellokk.ble.ui.ScenesActivity
import cn.hellokk.ble.ui.adapter.DeviceMainAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.concurrent.ConcurrentHashMap
/**
 * 作者: Kun on 2025/5/9.
 * 邮箱: vip@hellokk.cc.
 * 描述: 首页
 */
@SuppressLint("MissingPermission")
class MainActivity : AppCompatActivity() {

    private lateinit var buttonScan: Button
    private lateinit var buttonSettings: ImageButton
    private lateinit var recyclerViewDevices: RecyclerView
    private lateinit var cardViewNoDevices: CardView
    private lateinit var emptyStateButton: Button
    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val deviceList = mutableListOf<BluetoothDeviceInfo>()
    private lateinit var deviceAdapter: DeviceMainAdapter

    // 设备连接状态缓存，使用ConcurrentHashMap保证线程安全
    private val deviceConnectionStates = ConcurrentHashMap<String, Boolean>()

    // 设备最后活动时间，用于判断设备是否离线
    private val deviceLastActiveTime = ConcurrentHashMap<String, Long>()

    // 设备状态检查间隔
    private val connectionCheckInterval = 30000L // 30秒

    // UI更新Handler
    private val handler = Handler(Looper.getMainLooper())

    // 用于定期检查设备状态的Runnable
    private val connectionCheckRunnable = object : Runnable {
        override fun run() {
            checkDeviceConnections()
            handler.postDelayed(this, connectionCheckInterval)
        }
    }

    // 蓝牙状态广播接收器
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    try {
                        // 从广播中获取连接的蓝牙设备地址
                        extractDeviceAddressAndUpdate(intent, true)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "处理连接广播失败", e)
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    try {
                        // 从广播中获取断开连接的蓝牙设备地址
                        extractDeviceAddressAndUpdate(intent, false)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "处理断开连接广播失败", e)
                    }
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    when(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                        BluetoothAdapter.STATE_OFF -> markAllDevicesOffline()
                        BluetoothAdapter.STATE_ON -> checkDeviceConnections()
                    }
                }
            }
        }
    }

    // 从广播中提取设备地址并更新状态
    @SuppressLint("MissingPermission")
    private fun extractDeviceAddressAndUpdate(intent: Intent, isConnected: Boolean) {
        val extras = intent.extras ?: return

        // 尝试多种方式获取设备地址
        var deviceAddress: String? = null

        // 方式1: 尝试获取地址字符串
        deviceAddress = extras.getString("android.bluetooth.device.extra.ADDRESS")

        // 方式2: 尝试从BluetoothDevice对象获取
        if (deviceAddress == null) {
            try {
                val deviceObj = extras.get(BluetoothDevice.EXTRA_DEVICE)
                if (deviceObj is BluetoothDevice) {
                    deviceAddress = deviceObj.address
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "从EXTRA_DEVICE获取地址失败", e)
            }
        }

        // 方式3: 遍历所有extras寻找BluetoothDevice对象
        if (deviceAddress == null) {
            for (key in extras.keySet()) {
                try {
                    val value = extras.get(key)
                    if (value is BluetoothDevice) {
                        deviceAddress = value.address
                        break
                    }
                } catch (e: Exception) {
                    continue
                }
            }
        }

        // 如果找到设备地址，更新连接状态
        deviceAddress?.let {
            updateDeviceConnectionState(it, isConnected)
        }
    }

    // 权限请求回调
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            loadSavedDevices()
            checkDeviceConnections()
        } else {
            Toast.makeText(this, "需要蓝牙权限才能管理设备", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化蓝牙适配器
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        initViews()
        setupListeners()
        setupAdapter()
        registerBluetoothReceiver()

        // 检查权限并加载设备
        checkPermissionsAndLoadDevices()
    }

    private fun initViews() {
        buttonScan = findViewById(R.id.buttonScan)
        buttonSettings = findViewById(R.id.buttonSettings)
        recyclerViewDevices = findViewById(R.id.recyclerViewDevices)
        cardViewNoDevices = findViewById(R.id.cardViewNoDevices)
        emptyStateButton = findViewById(R.id.emptyStateButton)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        // 设置底部导航选中项
        bottomNavigation.selectedItemId = R.id.navigation_home
    }

    private fun setupListeners() {
        buttonScan.setOnClickListener {
            navigateToScanDevices()
        }

        buttonSettings.setOnClickListener {
            navigateToSettings()
        }

        emptyStateButton.setOnClickListener {
            navigateToScanDevices()
        }

        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    // 已经在首页，不需要处理
                    true
                }
                R.id.navigation_scenes -> {
                    navigateToScenes()
                    true
                }
                R.id.navigation_profile -> {
                    navigateToProfile()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupAdapter() {
        recyclerViewDevices.layoutManager = LinearLayoutManager(this)
        deviceAdapter = DeviceMainAdapter(deviceList, ::onDeviceClicked)
        recyclerViewDevices.adapter = deviceAdapter
    }

    private fun registerBluetoothReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        registerReceiver(bluetoothReceiver, filter)
    }

    private fun checkPermissionsAndLoadDevices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED -> {
                    loadSavedDevices()
                    checkDeviceConnections()
                }
                else -> {
                    requestPermissionLauncher.launch(
                        arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
                    )
                }
            }
        } else {
            loadSavedDevices()
            checkDeviceConnections()
        }
    }

    @SuppressLint("MissingPermission")
    private fun loadSavedDevices() {
        // 清除当前列表
        deviceList.clear()

        // 从数据库或SharedPreferences加载已保存的设备
        val mockDevices = listOf(
            BluetoothDeviceInfo(
                name = "ESP32_Device",
                address = "F0:F5:BD:2B:AB:D2",
                rssi = -60,
                isConnectable = true
            )
        )

        deviceList.addAll(mockDevices)

        // 获取已配对设备并添加到列表
        try {
            val pairedDevices = bluetoothAdapter.bondedDevices
            for (device in pairedDevices) {
                // 检查是否已经在列表中
                if (deviceList.none { it.address == device.address }) {
                    val deviceInfo = BluetoothDeviceInfo(
                        name = device.name ?: "未知设备",
                        address = device.address,
                        rssi = -65, // 默认值
                        isConnectable = true
                    )
                    deviceList.add(deviceInfo)
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "加载已配对设备失败", e)
        }

        updateDeviceListUI()
    }

    @SuppressLint("MissingPermission")
    private fun checkDeviceConnections() {
        if (!bluetoothAdapter.isEnabled) {
            // 蓝牙关闭，所有设备标记为离线
            markAllDevicesOffline()
            return
        }

        // 获取当前连接的设备
        try {
            // 检查已连接的设备
            val profileProxy = BluetoothAdapter::class.java.getMethod("getProfileProxy", Context::class.java, BluetoothProfile.ServiceListener::class.java, Int::class.java)

            // 使用GATT服务检查连接状态
            val serviceListener = object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    val connectedDevices = proxy.connectedDevices

                    // 先标记所有设备为未连接
                    for (device in deviceList) {
                        device.isConnected = false
                    }

                    // 更新连接状态
                    for (device in connectedDevices) {
                        updateDeviceConnectionState(device.address, true)
                    }

                    // 更新UI
                    runOnUiThread {
                        deviceAdapter.notifyDataSetChanged()
                    }

                    // 关闭代理连接
                    bluetoothAdapter.closeProfileProxy(profile, proxy)
                }

                override fun onServiceDisconnected(profile: Int) {
                    // 服务断开，不需要处理
                }
            }

            // 获取GATT服务连接
            profileProxy.invoke(bluetoothAdapter, this, serviceListener, BluetoothProfile.GATT)

        } catch (e: Exception) {
            Log.e("MainActivity", "检查设备连接状态失败", e)

            // 如果无法通过系统API检查，使用最后活动时间判断
            checkDeviceConnectionsByActivity()
        }
    }

    private fun checkDeviceConnectionsByActivity() {
        val currentTime = System.currentTimeMillis()

        // 检查每个设备的最后活动时间
        for (device in deviceList) {
            val lastActiveTime = deviceLastActiveTime[device.address] ?: 0
            val timeSinceLastActive = currentTime - lastActiveTime

            // 如果30分钟内没有活动，认为设备离线
            if (timeSinceLastActive > 30 * 60 * 1000) {
                device.isConnected = false
            }
        }

        // 更新UI
        runOnUiThread {
            deviceAdapter.notifyDataSetChanged()
        }
    }

    private fun markAllDevicesOffline() {
        for (device in deviceList) {
            device.isConnected = false
        }

        runOnUiThread {
            deviceAdapter.notifyDataSetChanged()
        }
    }

    private fun updateDeviceConnectionState(deviceAddress: String, isConnected: Boolean) {
        // 更新设备状态缓存
        deviceConnectionStates[deviceAddress] = isConnected

        // 如果连接，更新最后活动时间
        if (isConnected) {
            deviceLastActiveTime[deviceAddress] = System.currentTimeMillis()
        }

        // 更新设备列表中的连接状态
        val deviceIndex = deviceList.indexOfFirst { it.address == deviceAddress }
        if (deviceIndex != -1) {
            deviceList[deviceIndex].isConnected = isConnected

            runOnUiThread {
                deviceAdapter.notifyItemChanged(deviceIndex)
            }
        }
    }

    private fun updateDeviceListUI() {
        if (deviceList.isEmpty()) {
            recyclerViewDevices.visibility = View.GONE
            cardViewNoDevices.visibility = View.VISIBLE
        } else {
            recyclerViewDevices.visibility = View.VISIBLE
            cardViewNoDevices.visibility = View.GONE
            deviceAdapter.notifyDataSetChanged()
        }
    }

    private fun onDeviceClicked(device: BluetoothDeviceInfo) {
        navigateToDeviceControl(device)
    }

    private fun navigateToScanDevices() {
        val intent = Intent(this, ScanDevicesActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToSettings() {
        Toast.makeText(this, "设置功能正在开发中", Toast.LENGTH_SHORT).show()
        // 实际使用时应导航到设置页面
        // val intent = Intent(this, SettingsActivity::class.java)
        // startActivity(intent)
    }

    private fun navigateToDeviceControl(device: BluetoothDeviceInfo) {
        val intent = Intent(this, DeviceControlActivity::class.java).apply {
            putExtra("DEVICE_NAME", device.name)
            putExtra("DEVICE_ID", device.address)
        }
        startActivity(intent)
    }

    private fun navigateToScenes() {
        val intent = Intent(this, ScenesActivity::class.java)
        startActivity(intent)
        overridePendingTransition(0, 0) // 避免切换动画
        finish()
    }

    private fun navigateToProfile() {
        Toast.makeText(this, "个人中心功能正在开发中", Toast.LENGTH_SHORT).show()
        // 实际使用时应导航到个人中心页面
        // val intent = Intent(this, ProfileActivity::class.java)
        // startActivity(intent)
        // overridePendingTransition(0, 0)
        // finish()
    }

    override fun onResume() {
        super.onResume()
        // 页面恢复时检查设备连接状态
        checkDeviceConnections()
        // 启动定期检查
        handler.removeCallbacks(connectionCheckRunnable)
        handler.postDelayed(connectionCheckRunnable, connectionCheckInterval)
    }

    override fun onPause() {
        super.onPause()
        // 页面暂停时停止定期检查
        handler.removeCallbacks(connectionCheckRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 注销广播接收器
        try {
            unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            Log.e("MainActivity", "注销广播接收器失败", e)
        }

        // 移除所有回调
        handler.removeCallbacksAndMessages(null)
    }
}