package cn.hellokk.ble.ui.main

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import cn.hellokk.ble.R
import cn.hellokk.ble.databinding.FragmentBluetoothBinding
import java.util.*

@SuppressLint("MissingPermission")
@RequiresApi(Build.VERSION_CODES.M)
class BluetoothFragment : Fragment() {

    private lateinit var pageViewModel: PageViewModel
    private var _binding: FragmentBluetoothBinding? = null
    private val binding get() = _binding!!

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var targetDevice: BluetoothDevice? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var colorCharacteristic: BluetoothGattCharacteristic? = null

    private var redValue: Byte = 0
    private var greenValue: Byte = 0
    private var blueValue: Byte = 0

    // UUID 定义
    private val SERVICE_UUID = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb")
    private val CHARACTERISTIC_UUID = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProvider(this).get(PageViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBluetoothBinding.inflate(inflater, container, false)
        initBluetoothManager()
        requestBluetoothPermissions()
        initListener()
        return binding.root
    }

    companion object {
        @JvmStatic
        fun newInstance(): BluetoothFragment {
            return BluetoothFragment()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bluetoothGatt?.close() // 关闭 GATT 连接
        _binding = null
    }

    private fun initBluetoothManager() {
        val bluetoothManager = activity?.getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager?.adapter
    }

    private fun requestBluetoothPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    android.Manifest.permission.BLUETOOTH,
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.BLUETOOTH_ADMIN,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                binding.textViewStatus.text = "权限已授予"
            } else {
                binding.textViewStatus.text = "权限被拒绝，无法查找设备"
            }
        }
    }

    private fun initListener() {
        binding.buttonFindDevices.setOnClickListener {
            startDiscovery()
        }

        binding.buttonConnect.setOnClickListener {
            targetDevice?.let { device -> connectToDevice(device) }
        }

        binding.buttonDisconnect.setOnClickListener {
            disconnectDevice()
        }

        binding.buttonSendColor.setOnClickListener {
            sendColor(redValue, greenValue, blueValue) // 发送选定的颜色
        }

        // 进度条设置
        binding.seekBarRed.setOnSeekBarChangeListener(colorSeekBarChangeListener)
        binding.seekBarGreen.setOnSeekBarChangeListener(colorSeekBarChangeListener)
        binding.seekBarBlue.setOnSeekBarChangeListener(colorSeekBarChangeListener)
    }

    private val colorSeekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            when (seekBar?.id) {
                R.id.seekBarRed -> {
                    redValue = progress.toByte()
                    binding.textViewRedValue.text = "红色: $progress"
                }
                R.id.seekBarGreen -> {
                    greenValue = progress.toByte()
                    binding.textViewGreenValue.text = "绿色: $progress"
                }
                R.id.seekBarBlue -> {
                    blueValue = progress.toByte()
                    binding.textViewBlueValue.text = "蓝色: $progress"
                }
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    }

    private fun startDiscovery() {
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter?.cancelDiscovery() // 如果正在发现，先取消
        }

        binding.textViewStatus.text = "开始查找设备..."
        bluetoothAdapter?.startDiscovery()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                if (BluetoothDevice.ACTION_FOUND == action) {
                    val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                    val deviceName = device.name ?: "未知设备"
                    val deviceAddress = device.address

                    binding.textViewStatus.append("\n找到设备: $deviceName ($deviceAddress)")

                    if (deviceName == "ESP32_Device") {
                        targetDevice = device
                        binding.textViewStatus.append("\n找到目标设备: $deviceName")
                        bluetoothAdapter?.cancelDiscovery() // 找到后取消发现
                    }
                }
            }
        }

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        activity?.registerReceiver(receiver, filter)

        Handler(Looper.getMainLooper()).postDelayed({
            bluetoothAdapter?.cancelDiscovery()
            activity?.unregisterReceiver(receiver)
            binding.textViewStatus.append("\n设备查找结束")
        }, 12000) // 12秒后停止发现
    }

    private fun connectToDevice(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(activity, false, gattCallback)
        binding.textViewStatus.append("\n连接中...")
    }

    private fun disconnectDevice() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        binding.textViewStatus.append("\n设备已断开连接")
    }

    private fun sendColor(red: Byte, green: Byte, blue: Byte) {
        if (bluetoothGatt != null && colorCharacteristic != null) {
            val colorString = String(charArrayOf(red.toChar(), green.toChar(), blue.toChar()))
            colorCharacteristic?.setValue(colorString)
            bluetoothGatt?.writeCharacteristic(colorCharacteristic)
            binding.textViewStatus.append("\n发送颜色: R=$red G=$green B=$blue")
        } else {
            binding.textViewStatus.append("\n无法发送颜色，设备未连接或特征未找到")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                activity?.runOnUiThread {
                    binding.textViewStatus.append("\n连接成功")
                    // 发现服务
                    gatt.discoverServices()
                }
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                activity?.runOnUiThread {
                    binding.textViewStatus.append("\n设备已断开连接")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // 获取特征
                val service = gatt.getService(SERVICE_UUID)
                colorCharacteristic = service.getCharacteristic(CHARACTERISTIC_UUID)
                activity?.runOnUiThread {
                    binding.textViewStatus.append("\n特征已找到，准备发送颜色")
                }
            }
        }
    }
}