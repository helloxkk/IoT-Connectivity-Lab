package cn.hellokk.ble.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.hellokk.ble.R
import cn.hellokk.ble.bluetoothcontroller.ColorOption
import cn.hellokk.ble.ui.adapter.ColorAdapter
import java.util.UUID
import kotlin.math.roundToInt

/**
 * 作者: Kun on 2025/5/9.
 * 邮箱: vip@hellokk.cc.
 * 描述: 设备控制页面
 */
@SuppressLint("MissingPermission")
class DeviceControlActivity : AppCompatActivity() {

    private lateinit var deviceName: String
    private lateinit var deviceId: String

    private lateinit var buttonBack: ImageButton
    private lateinit var buttonMore: ImageButton
    private lateinit var textViewDeviceTitle: TextView
    private lateinit var viewLedPreview: View
    private lateinit var recyclerViewColorPicker: RecyclerView
    private lateinit var seekBarColor: SeekBar
    private lateinit var seekBarBrightness: SeekBar
    private lateinit var textViewColorValue: TextView
    private lateinit var textViewBrightnessValue: TextView
    private lateinit var editTextR: EditText
    private lateinit var editTextG: EditText
    private lateinit var editTextB: EditText
    private lateinit var tvStatus: TextView
    private lateinit var layoutPower: LinearLayout
    private lateinit var layoutBlink: LinearLayout
    private lateinit var layoutRandom: LinearLayout
    private lateinit var layoutMore: LinearLayout

    private var currentColor = Color.RED
    private var currentBrightness = 80
    private var isPowerOn = true
    private var isUpdatingUI = false // 防止循环更新标志
    private var hasForcedInitialUpdate = false // 强制初始化标记

    // 颜色更新延迟处理器
    private val handler = Handler(Looper.getMainLooper())
    private var pendingColorUpdate = false

    // 预设颜色列表
    private val presetColors = listOf(
        ColorOption(Color.RED, true),
        ColorOption(Color.GREEN),
        ColorOption(Color.BLUE),
        ColorOption(Color.YELLOW),
        ColorOption(Color.MAGENTA),
        ColorOption(Color.CYAN),
        ColorOption(Color.WHITE),
        ColorOption(Color.parseColor("#FF8000"))
    )

    // 颜色名称映射
    private val colorNames = mapOf(
        0 to "红色",
        60 to "黄色",
        120 to "绿色",
        180 to "青色",
        240 to "蓝色",
        300 to "紫色",
        360 to "红色"
    )

    // 蓝牙相关
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var rgbCharacteristic: BluetoothGattCharacteristic? = null

    // 示例 UUID，实际应用中需要替换为设备的真实 UUID
    private val SERVICE_UUID = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb")
    private val RGB_CHARACTERISTIC_UUID = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_control)

        // 获取传入的设备信息
        deviceName = intent.getStringExtra("DEVICE_NAME") ?: "ESP32 智能灯"
        deviceId = intent.getStringExtra("DEVICE_ID") ?: ""

        initViews()
        setupUI()
        setupListeners()
        connectToDevice()

        // 在主线程上延迟执行强制初始更新，确保所有视图都已布局完成
        handler.postDelayed({
            forceInitialUIUpdate()
        }, 100)
    }

    // 强制初始UI更新，确保所有控件都显示正确的初始值
    private fun forceInitialUIUpdate() {
        if (hasForcedInitialUpdate) return

        hasForcedInitialUpdate = true
        isUpdatingUI = true

        // 确保RGB编辑框显示初始值
        updateRgbInputs()

        // 更新所有显示
        updateColorDisplay()
        updateColorControls()

        isUpdatingUI = false
    }

    private fun initViews() {
        buttonBack = findViewById(R.id.buttonBack)
        buttonMore = findViewById(R.id.buttonMore)
        textViewDeviceTitle = findViewById(R.id.textViewDeviceTitle)
        viewLedPreview = findViewById(R.id.viewLedPreview)
        recyclerViewColorPicker = findViewById(R.id.recyclerViewColorPicker)
        seekBarColor = findViewById(R.id.seekBarColor)
        seekBarBrightness = findViewById(R.id.seekBarBrightness)
        textViewColorValue = findViewById(R.id.textViewColorValue)
        textViewBrightnessValue = findViewById(R.id.textViewBrightnessValue)
        editTextR = findViewById(R.id.editTextR)
        editTextG = findViewById(R.id.editTextG)
        editTextB = findViewById(R.id.editTextB)
        tvStatus = findViewById(R.id.tv_status)
        layoutPower = findViewById(R.id.layoutPower)
        layoutBlink = findViewById(R.id.layoutBlink)
        layoutRandom = findViewById(R.id.layoutRandom)
        layoutMore = findViewById(R.id.layoutMore)

        // 设置SeekBar的颜色条（创建彩虹渐变背景）
        createColorGradientForSeekBar()
    }

    private fun createColorGradientForSeekBar() {
        // 创建彩虹渐变色的drawable用于色相SeekBar
        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(
                Color.RED, Color.YELLOW, Color.GREEN,
                Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED
            )
        ).apply {
            cornerRadius = resources.displayMetrics.density * 8 // 8dp圆角
            setSize(0, (resources.displayMetrics.density * 4).toInt()) // 4dp高度
            // 添加左右padding防止thumb溢出
            setBounds(8, 0, 0, 0)
        }

        // 应用自定义drawable，保持xml中的padding设置
        val originalDrawable = seekBarColor.progressDrawable
        seekBarColor.progressDrawable = gradientDrawable
    }

    private fun setupUI() {
        textViewDeviceTitle.text = deviceName

        // 设置LED预览的初始外观
        updateLedPreviewShape()

        // 设置颜色选择器
        recyclerViewColorPicker.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerViewColorPicker.adapter = ColorAdapter(presetColors) { position, color ->
            if (!isUpdatingUI) {
                currentColor = color
                updateColorDisplay()
                updateColorControls()
                updateRgbInputs() // 重要：确保更新RGB输入框
                sendColorToDevice()
            }
        }

        // 设置初始值
        updateRgbInputs() // 先设置RGB输入框
        updateColorDisplay() // 然后更新显示
        seekBarColor.progress = colorToHue(currentColor)
        seekBarBrightness.progress = currentBrightness
        textViewBrightnessValue.text = "$currentBrightness%"
        textViewColorValue.text = getColorName(colorToHue(currentColor))
    }

    private fun setupListeners() {
        buttonBack.setOnClickListener {
            finish()
        }

        buttonMore.setOnClickListener {
            val intent = Intent(this, DeviceDetailsActivity::class.java).apply {
                putExtra("DEVICE_ID", deviceId)
                putExtra("DEVICE_NAME", deviceName)
            }
            startActivity(intent)
        }

        seekBarColor.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && !isUpdatingUI) {
                    isUpdatingUI = true

                    currentColor = hueToColor(progress)
                    textViewColorValue.text = getColorName(progress)
                    updateColorDisplay()

                    // 修复：确保SeekBar变化时也更新RGB输入框
                    updateRgbInputs()

                    isUpdatingUI = false

                    // 添加调试日志
                    Log.d("DeviceControlActivity", "SeekBar色相变化: $progress -> RGB(${Color.red(currentColor)},${Color.green(currentColor)},${Color.blue(currentColor)})")
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (!pendingColorUpdate) {
                    pendingColorUpdate = true
                    handler.postDelayed({
                        sendColorToDevice()
                        pendingColorUpdate = false
                    }, 100) // 短暂延迟，避免拖动时频繁发送
                }
            }
        })

        seekBarBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && !isUpdatingUI) {
                    isUpdatingUI = true

                    currentBrightness = progress
                    textViewBrightnessValue.text = "$progress%"
                    updateColorDisplay()

                    isUpdatingUI = false
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (!pendingColorUpdate) {
                    pendingColorUpdate = true
                    handler.postDelayed({
                        sendColorToDevice()
                        pendingColorUpdate = false
                    }, 100) // 短暂延迟，避免拖动时频繁发送
                }
            }
        })

        // RGB输入框的监听器
        val rgbTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingUI) return

                try {
                    // 获取三个输入框的值，空值默认为0
                    val r = editTextR.text.toString().let { if (it.isNotEmpty()) it.toInt() else 0 }
                    val g = editTextG.text.toString().let { if (it.isNotEmpty()) it.toInt() else 0 }
                    val b = editTextB.text.toString().let { if (it.isNotEmpty()) it.toInt() else 0 }

                    // 确保RGB值在0-127范围内
                    val validR = r.coerceIn(0, 127)
                    val validG = g.coerceIn(0, 127)
                    val validB = b.coerceIn(0, 127)

                    // 开始UI更新，防止循环调用
                    isUpdatingUI = true

                    // 修正：如果输入框为空，保持为空而不是设为0
                    if (editTextR.text.toString().isEmpty() && validR == 0) {
                        // 保持为空
                    } else if (validR != r) {
                        editTextR.setText(validR.toString())
                        editTextR.setSelection(editTextR.text.length)
                    }

                    if (editTextG.text.toString().isEmpty() && validG == 0) {
                        // 保持为空
                    } else if (validG != g) {
                        editTextG.setText(validG.toString())
                        editTextG.setSelection(editTextG.text.length)
                    }

                    if (editTextB.text.toString().isEmpty() && validB == 0) {
                        // 保持为空
                    } else if (validB != b) {
                        editTextB.setText(validB.toString())
                        editTextB.setSelection(editTextB.text.length)
                    }

                    // 更新当前颜色
                    currentColor = Color.rgb(validR, validG, validB)

                    // 更新其他UI
                    updateColorDisplay()
                    updateColorControls()

                    // 结束UI更新
                    isUpdatingUI = false

                    // 避免频繁发送，添加延迟
                    if (!pendingColorUpdate) {
                        pendingColorUpdate = true
                        handler.postDelayed({
                            sendColorToDevice()
                            pendingColorUpdate = false
                        }, 500) // 延迟500ms
                    }
                } catch (e: Exception) {
                    Log.e("DeviceControlActivity", "RGB转换错误", e)
                    isUpdatingUI = false
                }
            }
        }

        // 为三个输入框添加文本变化监听器
        editTextR.addTextChangedListener(rgbTextWatcher)
        editTextG.addTextChangedListener(rgbTextWatcher)
        editTextB.addTextChangedListener(rgbTextWatcher)

        // 快速控制按钮
        layoutPower.setOnClickListener {
            togglePower()
        }

        layoutBlink.setOnClickListener {
            toggleBlinkMode()
        }

        layoutRandom.setOnClickListener {
            setRandomColor()
        }

        layoutMore.setOnClickListener {
            toggleMarqueeMode()
//            toggleBreathingMode()
        }
    }

    private fun connectToDevice() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Toast.makeText(this, "请开启蓝牙", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val device = bluetoothAdapter?.getRemoteDevice(deviceId.uppercase())
            if (device != null) {
                bluetoothGatt = device.connectGatt(this, false, gattCallback)
                Log.i("DeviceControlActivity", "正在连接设备...")
            } else {
                Toast.makeText(this, "未找到设备", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("DeviceControlActivity", "连接设备失败", e)
            Toast.makeText(this, "连接设备失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread {
                    Log.i("DeviceControlActivity", "设备已连接")
                }
                gatt.discoverServices() // 确保发现服务
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runOnUiThread {
                    Toast.makeText(this@DeviceControlActivity, "设备已断开连接", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(SERVICE_UUID)
                if (service != null) {
                    rgbCharacteristic = service.getCharacteristic(RGB_CHARACTERISTIC_UUID)
                    if (rgbCharacteristic != null) {
                        runOnUiThread {
                            sendColorToDevice() // 发送初始颜色
                        }
                    } else {
                        Log.e("DeviceControlActivity", "RGB特征未找到")
                    }
                } else {
                    Log.e("DeviceControlActivity", "服务未找到")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendColorToDevice() {
        val r = Color.red(currentColor)
        val g = Color.green(currentColor)
        val b = Color.blue(currentColor)

        Log.i("DeviceControlActivity", "R: $r G: $g B: $b")

        val brightness = currentBrightness / 100.0f
        val adjustedR = (r * brightness).roundToInt().coerceIn(0, 127)
        val adjustedG = (g * brightness).roundToInt().coerceIn(0, 127)
        val adjustedB = (b * brightness).roundToInt().coerceIn(0, 127)

        Log.i("DeviceControlActivity", "adjustedR: $adjustedR adjustedG: $adjustedG adjustedB: $adjustedB")

        val finalR = if (isPowerOn) adjustedR else 0
        val finalG = if (isPowerOn) adjustedG else 0
        val finalB = if (isPowerOn) adjustedB else 0

        val rgbData = String(charArrayOf(finalR.toChar(), finalG.toChar(), finalB.toChar()))

        if (rgbCharacteristic != null && bluetoothGatt != null) {
            try {
                rgbCharacteristic!!.setValue(rgbData)
                val success = bluetoothGatt!!.writeCharacteristic(rgbCharacteristic)
                if (success) {
//                    tvStatus.text = "\n发送颜色: R=$finalR G=$finalG B=$finalB"
                    // 使用 StringBuilder 创建十六进制字符串
                    // 生成十六进制字符串
                    val rgbHexString = "%02X%02X%02X".format(finalR, finalG, finalB)
                    // 显示在 TextView 上
                    tvStatus.text = rgbHexString
                    Log.i("DeviceControlActivity", "颜色已发送")
                } else {
                    Log.e("DeviceControlActivity", "发送颜色失败: 写入特征失败")
                }
            } catch (e: Exception) {
                Log.e("DeviceControlActivity", "发送颜色失败", e)
                Toast.makeText(this, "发送颜色失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.e("DeviceControlActivity", "设备未连接或特征未找到")
        }
    }

    private fun updateColorDisplay() {
        // 应用亮度到预览颜色
        val brightness = currentBrightness / 100.0f
        val r = (Color.red(currentColor) * brightness).roundToInt().coerceIn(0, 127)
        val g = (Color.green(currentColor) * brightness).roundToInt().coerceIn(0, 127)
        val b = (Color.blue(currentColor) * brightness).roundToInt().coerceIn(0, 127)

        val displayColor = if (isPowerOn) Color.rgb(r, g, b) else Color.BLACK

        // 更新LED预览颜色
        val shapeDrawable = viewLedPreview.background as GradientDrawable
        shapeDrawable.setColor(displayColor)

        // 根据是否开启调整阴影效果
        viewLedPreview.elevation = if (isPowerOn) 8f else 0f
    }

    private fun updateLedPreviewShape() {
        // 创建圆形drawable
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.RED)
        }
        viewLedPreview.background = drawable
    }

    private fun updateRgbInputs() {
        // 更新RGB输入框的值
        if (!isUpdatingUI) {
            val r = Color.red(currentColor)
            val g = Color.green(currentColor)
            val b = Color.blue(currentColor)

            // 确保三个输入框都有值
            if (editTextR.text.toString() != r.toString()) {
                editTextR.setText(r.toString())
            }

            if (editTextG.text.toString() != g.toString()) {
                editTextG.setText(g.toString())
            }

            if (editTextB.text.toString() != b.toString()) {
                editTextB.setText(b.toString())
            }

            // 打印日志，调试问题
            Log.d("DeviceControlActivity", "更新RGB输入框: R=$r, G=$g, B=$b")
        }
    }

    private fun updateColorControls() {
        // 更新色相滑块位置
        if (!isUpdatingUI) {
            isUpdatingUI = true

            val hue = colorToHue(currentColor)
            seekBarColor.progress = hue
            textViewColorValue.text = getColorName(hue)

            // 更新颜色选择器中的选中状态
            val adapter = recyclerViewColorPicker.adapter as ColorAdapter
            val selectedIndex = presetColors.indexOfFirst {
                isSimilarColor(it.color, currentColor)
            }

            if (selectedIndex != -1) {
                adapter.updateSelection(selectedIndex)
            }

            isUpdatingUI = false
        }
    }

    private fun togglePower() {
        isPowerOn = !isPowerOn
        updateColorDisplay()
        sendColorToDevice()
        Log.i("DeviceControlActivity", if (isPowerOn) "已开启" else "已关闭")
    }

    private var isBlinking = false // 用于跟踪闪烁状态

    private fun toggleBlinkMode() {
        if (isBlinking) {
            // 停止闪烁
            handler.removeCallbacksAndMessages(null)
            currentBrightness = 80 // 恢复原来的亮度
            sendColorToDevice() // 恢复颜色
            Log.i("DeviceControlActivity", "闪烁模式已停止")
        } else {
            // 启动闪烁模式
            Log.i("DeviceControlActivity", "闪烁模式已启动")
            val blinkInterval = 500L // 闪烁间隔时间，单位毫秒
            var isOn = false

            val blinkRunnable = object : Runnable {
                override fun run() {
                    currentBrightness = if (isOn) {
                        0 // 关闭灯光
                    } else {
                        80 // 恢复亮度
                    }
                    sendColorToDevice() // 发送颜色数据
                    updateColorDisplay()
                    isOn = !isOn
                    handler.postDelayed(this, blinkInterval) // 继续下一个闪烁
                }
            }

            // 开始闪烁
            handler.post(blinkRunnable)
        }
        isBlinking = !isBlinking // 切换状态
    }

    private fun setRandomColor() {
        // 设置随机颜色
        val r = (0..127).random()
        val g = (0..127).random()
        val b = (0..127).random()

        // 开始UI更新，防止循环调用
        isUpdatingUI = true

        currentColor = Color.rgb(r, g, b)
        updateColorDisplay()
        updateRgbInputs()
        updateColorControls()

        // 结束UI更新
        isUpdatingUI = false

        sendColorToDevice()

    }

    private var isMarqueeRunning = false // 跑马灯状态

    private fun toggleMarqueeMode() {
        if (isMarqueeRunning) {
            // 停止跑马灯
            handler.removeCallbacksAndMessages(null)
            Log.i("DeviceControlActivity", "跑马灯模式已停止")
        } else {
            // 启动跑马灯
            Log.i("DeviceControlActivity", "跑马灯模式已启动")

            val marqueeInterval = 500L // 跑马灯变化的时间间隔
            val marqueeRunnable = object : Runnable {
                override fun run() {
                    setRandomColor() // 设置随机颜色
                    handler.postDelayed(this, marqueeInterval) // 继续下一个更新
                }
            }

            // 开始跑马灯
            handler.post(marqueeRunnable)
        }
        isMarqueeRunning = !isMarqueeRunning // 切换状态
    }

    private var isBreathing = false // 用于跟踪呼吸灯状态

    private fun toggleBreathingMode() {
        if (isBreathing) {
            // 停止呼吸灯模式
            handler.removeCallbacksAndMessages(null)
            currentBrightness = 80 // 恢复原来的亮度
            sendColorToDevice() // 恢复颜色
            Log.i("DeviceControlActivity", "呼吸灯模式已停止")
        } else {
            // 启动呼吸灯模式
            Log.i("DeviceControlActivity", "呼吸灯模式已启动")

            val breathDuration = 4000L // 每次呼吸的总时间
            val steps = 100 // 渐变的步数
            val stepDelay = breathDuration / (steps * 2) // 每步的延迟时间，往返

            val maxBrightness = 80 // 最大亮度
            val minBrightness = 0 // 最小亮度

            val breathingRunnable = object : Runnable {
                var brightness = minBrightness // 从最小亮度开始
                var increasing = true // 当前是否在增加亮度

                override fun run() {
                    currentBrightness = brightness
                    sendColorToDevice() // 发送颜色数据
                    updateColorDisplay()

                    // 更新亮度
                    if (increasing) {
                        brightness += (maxBrightness / steps)
                        if (brightness >= maxBrightness) {
                            brightness = maxBrightness
                            increasing = false // 转为减少亮度
                            setRandomColor() // 每次呼吸时设置随机颜色
                        }
                    } else {
                        brightness -= (maxBrightness / steps)
                        if (brightness <= minBrightness) {
                            brightness = minBrightness
                            increasing = true // 转为增加亮度
                        }
                    }

                    // 继续下一个更新
                    handler.postDelayed(this, stepDelay)
                }
            }

            // 开始呼吸灯模式
            handler.post(breathingRunnable)
        }
        isBreathing = !isBreathing // 切换状态
    }

    private fun hueToColor(hue: Int): Int {
        // 将色相转换为RGB颜色
        val hsv = floatArrayOf(hue.toFloat(), 1f, 1f)
        return Color.HSVToColor(hsv)
    }

    private fun colorToHue(color: Int): Int {
        // 将RGB颜色转换为色相值
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        return hsv[0].roundToInt()
    }

    private fun getColorName(hue: Int): String {
        // 找到最接近的颜色名称
        return colorNames.entries.minByOrNull { Math.abs(it.key - hue) }?.value ?: "自定义"
    }

    private fun isSimilarColor(color1: Int, color2: Int): Boolean {
        // 判断两个颜色是否相似（用于自动选中预设颜色）
        val hsv1 = FloatArray(3)
        val hsv2 = FloatArray(3)
        Color.colorToHSV(color1, hsv1)
        Color.colorToHSV(color2, hsv2)

        // 判断色相和饱和度是否接近
        val hueDiff = Math.abs(hsv1[0] - hsv2[0])
        return (hueDiff < 10 || hueDiff > 350) && Math.abs(hsv1[1] - hsv2[1]) < 0.1
    }

    override fun onResume() {
        super.onResume()
        // 确保RGB输入框有值
        if (!hasForcedInitialUpdate) {
            handler.postDelayed({
                forceInitialUIUpdate()
            }, 100)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        // 断开蓝牙连接
        bluetoothGatt?.close()
        // 移除所有回调
        handler.removeCallbacksAndMessages(null)
    }
}