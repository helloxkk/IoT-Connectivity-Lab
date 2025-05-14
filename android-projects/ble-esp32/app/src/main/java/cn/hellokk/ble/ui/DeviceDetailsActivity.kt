package cn.hellokk.ble.ui

import android.annotation.SuppressLint
import android.bluetooth.*
import cn.hellokk.ble.R
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import java.text.SimpleDateFormat
import java.util.*

/**
 * 作者: Kun on 2025/5/9.
 * 邮箱: vip@hellokk.cc.
 * 描述: 设备详情页面
 */
@SuppressLint("MissingPermission")
class DeviceDetailsActivity : AppCompatActivity() {

    private lateinit var deviceId: String
    private lateinit var deviceName: String

    private lateinit var buttonBack: ImageButton
    private lateinit var buttonMore: ImageButton
    private lateinit var textViewDeviceName: TextView
    private lateinit var textViewDeviceType: TextView
    private lateinit var textViewMacAddress: TextView
    private lateinit var textViewSignalStrength: TextView
    private lateinit var textViewConnectionStatus: TextView
    private lateinit var textViewFirmwareVersion: TextView
    private lateinit var textViewLastConnected: TextView
    private lateinit var switchAutoConnect: SwitchCompat
    private lateinit var layoutDeviceShare: LinearLayout
    private lateinit var layoutFirmwareUpdate: LinearLayout
    private lateinit var buttonModifyName: CardView
    private lateinit var buttonResetDevice: CardView
    private lateinit var buttonDisconnect: CardView
    private lateinit var buttonDeleteDevice: CardView

    private var bluetoothGatt: BluetoothGatt? = null

    // 示例 UUID，实际应用中需要替换为设备的真实 UUID
    private val SERVICE_UUID = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb")
    private val RGB_CHARACTERISTIC_UUID = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb")

    // 蓝牙相关
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_details)

        // 获取传入的设备信息
        deviceId = intent.getStringExtra("DEVICE_ID") ?: ""
        deviceName = intent.getStringExtra("DEVICE_NAME") ?: "ESP32 智能灯"

        val device = bluetoothAdapter?.getRemoteDevice(deviceId.uppercase())
        if (device != null) {
            bluetoothGatt = device.connectGatt(this, false, gattCallback)
            Toast.makeText(this, "正在连接设备...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "未找到设备", Toast.LENGTH_SHORT).show()
        }

        initViews()
        setupDeviceInfo()
        setupListeners()
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread {
                    Toast.makeText(this@DeviceDetailsActivity, "设备已连接", Toast.LENGTH_SHORT).show()
                }
                gatt.discoverServices() // 确保发现服务
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runOnUiThread {
                    Toast.makeText(this@DeviceDetailsActivity, "设备已断开连接", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        }
    }

    private fun initViews() {
        buttonBack = findViewById(R.id.buttonBack)
        buttonMore = findViewById(R.id.buttonMore)
        textViewDeviceName = findViewById(R.id.textViewDeviceName)
        textViewDeviceType = findViewById(R.id.textViewDeviceType)
        textViewMacAddress = findViewById(R.id.textViewMacAddress)
        textViewSignalStrength = findViewById(R.id.textViewSignalStrength)
        textViewConnectionStatus = findViewById(R.id.textViewConnectionStatus)
        textViewFirmwareVersion = findViewById(R.id.textViewFirmwareVersion)
        textViewLastConnected = findViewById(R.id.textViewLastConnected)
        switchAutoConnect = findViewById(R.id.switchAutoConnect)
        layoutDeviceShare = findViewById(R.id.layoutDeviceShare)
        layoutFirmwareUpdate = findViewById(R.id.layoutFirmwareUpdate)
        buttonModifyName = findViewById(R.id.buttonModifyName)
        buttonResetDevice = findViewById(R.id.buttonResetDevice)
        buttonDisconnect = findViewById(R.id.buttonDisconnect)
        buttonDeleteDevice = findViewById(R.id.buttonDeleteDevice)
    }

    private fun setupDeviceInfo() {
        textViewDeviceName.text = deviceName
        textViewDeviceType.text = "RGB LED灯"

        // 如果有真实设备ID，则显示实际MAC地址，否则使用模拟数据
        val macAddress = if (deviceId.isNotEmpty() && deviceId.length == 17) {
            deviceId.uppercase()
        } else {
            "11:22:33:44:55:66"
        }
        textViewMacAddress.text = macAddress

        // 其他设备信息使用模拟数据
        textViewSignalStrength.text = "-65 dBm (强)"
        textViewConnectionStatus.text = "已连接"
        textViewFirmwareVersion.text = "v1.2.3"

        // 获取当前时间作为最后连接时间
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val currentTime = sdf.format(Date())
        textViewLastConnected.text = currentTime

        // 设置自动连接开关状态
        switchAutoConnect.isChecked = true
    }

    private fun setupListeners() {
        buttonBack.setOnClickListener {
            finish()
        }

        buttonMore.setOnClickListener {
            Toast.makeText(this, "设备详情更多选项", Toast.LENGTH_SHORT).show()
        }

        switchAutoConnect.setOnCheckedChangeListener { _, isChecked ->
            val message = if (isChecked) "已开启自动连接" else "已关闭自动连接"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

            // 实际应用中，这里应该保存用户的设置到本地存储
            // 例如：saveDeviceAutoConnectPreference(deviceId, isChecked)
        }

        layoutDeviceShare.setOnClickListener {
            showShareDeviceDialog()
        }

        layoutFirmwareUpdate.setOnClickListener {
            checkFirmwareUpdate()
        }

        buttonModifyName.setOnClickListener {
            showModifyNameDialog()
        }

        buttonResetDevice.setOnClickListener {
            showResetDeviceConfirmation()
        }

        buttonDisconnect.setOnClickListener {
            disconnectDevice()
        }

        buttonDeleteDevice.setOnClickListener {
            showDeleteDeviceConfirmation()
        }
    }

    private fun showShareDeviceDialog() {
        val shareOptions = arrayOf("通过二维码分享", "通过链接分享", "通过蓝牙分享")

        AlertDialog.Builder(this)
            .setTitle("分享设备")
            .setItems(shareOptions) { _, which ->
                val message = "选择了: ${shareOptions[which]}"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

                // 实际应用中，这里应该实现对应的分享功能
                // 例如生成二维码、创建分享链接或启动蓝牙分享
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun checkFirmwareUpdate() {
        // 显示进度对话框
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("固件检查")
            .setMessage("正在检查固件更新...")
            .setCancelable(false)
            .create()

        progressDialog.show()

        // 模拟网络请求延迟
        progressDialog.window?.decorView?.postDelayed({
            progressDialog.dismiss()

            // 随机决定是否有更新可用
            val hasUpdate = Math.random() > 0.5

            if (hasUpdate) {
                showFirmwareUpdateAvailable()
            } else {
                Toast.makeText(this, "当前固件已是最新版本", Toast.LENGTH_SHORT).show()
            }
        }, 2000) // 延迟2秒
    }

    private fun showFirmwareUpdateAvailable() {
        AlertDialog.Builder(this)
            .setTitle("发现新固件")
            .setMessage("新版本: v1.3.0\n\n更新内容:\n1. 优化连接稳定性\n2. 新增呼吸灯特效\n3. 修复已知问题")
            .setPositiveButton("立即更新") { _, _ ->
                simulateFirmwareUpdate()
            }
            .setNegativeButton("稍后再说", null)
            .show()
    }

    private fun simulateFirmwareUpdate() {
        // 显示更新进度对话框
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("固件更新")
            .setMessage("更新进度: 0%")
            .setCancelable(false)
            .create()

        progressDialog.show()

        // 模拟更新进度
        var progress = 0
        val handler = progressDialog.window?.decorView?.handler
        val updateRunnable = object : Runnable {
            override fun run() {
                progress += 10
                progressDialog.setMessage("更新进度: $progress%")

                if (progress < 100) {
                    handler?.postDelayed(this, 500) // 每0.5秒更新一次进度
                } else {
                    progressDialog.dismiss()
                    Toast.makeText(this@DeviceDetailsActivity,
                        "固件更新成功，设备将重启", Toast.LENGTH_LONG).show()

                    // 更新UI上的固件版本
                    textViewFirmwareVersion.text = "v1.3.0"

                    // 模拟重启设备
                    disconnectDevice()
                }
            }
        }

        handler?.post(updateRunnable)
    }

    private fun showModifyNameDialog() {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(deviceName)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(50, 20, 50, 20)
        input.layoutParams = params
        layout.addView(input)

        AlertDialog.Builder(this)
            .setTitle("修改设备名称")
            .setView(layout)
            .setPositiveButton("确定") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    deviceName = newName
                    textViewDeviceName.text = newName
                    Toast.makeText(this, "设备名称已修改", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "设备名称不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showResetDeviceConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("恢复默认设置")
            .setMessage("确定要将设备恢复到出厂设置吗？这将清除所有自定义配置。")
            .setPositiveButton("确定") { _, _ ->
                resetDevice()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun resetDevice() {
        // 显示进度对话框
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("恢复设置")
            .setMessage("正在恢复设备到出厂设置...")
            .setCancelable(false)
            .create()

        progressDialog.show()

        // 模拟操作延迟
        progressDialog.window?.decorView?.postDelayed({
            progressDialog.dismiss()
            Toast.makeText(this, "设备已重置为出厂设置", Toast.LENGTH_SHORT).show()

            // 恢复默认设备名称
            deviceName = "ESP32 智能灯"
            textViewDeviceName.text = deviceName

            // 更新连接时间
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val currentTime = sdf.format(Date())
            textViewLastConnected.text = currentTime
        }, 2000) // 延迟2秒
    }

    private fun disconnectDevice() {
        // 显示进度对话框
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("断开连接")
            .setMessage("正在断开与设备的连接...")
            .setCancelable(false)
            .create()

        progressDialog.show()

        // 尝试断开蓝牙连接
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
            Toast.makeText(this, "已断开与设备的连接", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // 忽略异常
        }

        // 模拟操作延迟
        progressDialog.window?.decorView?.postDelayed({
            progressDialog.dismiss()
            Toast.makeText(this, "已断开与设备的连接", Toast.LENGTH_SHORT).show()

            // 更新连接状态
            textViewConnectionStatus.text = "离线"
            textViewConnectionStatus.setTextColor(getColor(R.color.colorOffline))

            // 返回到主页
            finish()
        }, 1500) // 延迟1.5秒
    }

    private fun showDeleteDeviceConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("删除设备")
            .setMessage("确定要删除此设备吗？这将断开连接并从已保存设备列表中移除。")
            .setPositiveButton("删除") { _, _ ->
                deleteDevice()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteDevice() {
        // 显示进度对话框
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("删除设备")
            .setMessage("正在删除设备...")
            .setCancelable(false)
            .create()

        progressDialog.show()

        // 尝试断开蓝牙连接
        try {
            // 实际应用中，这里应该调用蓝牙断开连接的代码
            // 例如：bluetoothGatt?.close()
        } catch (e: Exception) {
            // 忽略异常
        }

        // 模拟操作延迟
        progressDialog.window?.decorView?.postDelayed({
            progressDialog.dismiss()
            Toast.makeText(this, "设备已从列表中移除", Toast.LENGTH_SHORT).show()

            // 实际应用中，这里应该从本地存储中删除设备信息
            // 例如：deviceRepository.deleteDevice(deviceId)

            // 返回到主页
            finish()
        }, 1500) // 延迟1.5秒
    }
}