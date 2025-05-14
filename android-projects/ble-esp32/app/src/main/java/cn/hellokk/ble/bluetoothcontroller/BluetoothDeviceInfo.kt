package cn.hellokk.ble.bluetoothcontroller

import java.io.Serializable

/**
 * 作者: Kun on 2025/5/10.
 * 邮箱: vip@hellokk.cc.
 * 描述: 设备数据类
 */
data class BluetoothDeviceInfo(
    val name: String,
    val address: String,
    val rssi: Int,
    val isConnectable: Boolean = true,
    var isConnected: Boolean = false,
    var lastActiveTimestamp: Long = System.currentTimeMillis()
) : Serializable {
    fun getSignalStrengthText(): String {
        return when {
            rssi > -50 -> "信号强度: 优"
            rssi > -70 -> "信号强度: 良"
            else -> "信号强度: 弱"
        }
    }

    fun getSignalStrengthColor(): Int {
        return when {
            rssi > -50 -> android.graphics.Color.parseColor("#4CAF50") // 绿色
            rssi > -70 -> android.graphics.Color.parseColor("#FFC107") // 黄色
            else -> android.graphics.Color.parseColor("#F44336") // 红色
        }
    }

    fun getStatusText(): String {
        return if (isConnected) "已连接" else "离线"
    }

    fun getStatusColor(): Int {
        return if (isConnected)
            android.graphics.Color.parseColor("#4CAF50") // 绿色
        else
            android.graphics.Color.parseColor("#8E8E8E") // 灰色
    }
}