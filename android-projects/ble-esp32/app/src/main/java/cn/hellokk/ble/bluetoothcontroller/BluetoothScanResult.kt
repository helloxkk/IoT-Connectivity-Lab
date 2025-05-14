package cn.hellokk.ble.bluetoothcontroller

import cn.hellokk.ble.R

/**
 * 作者: Kun on 2025/5/9.
 * 邮箱: vip@hellokk.cc.
 * 描述: 扫描蓝牙结果
 */
data class BluetoothScanResult(
    val address: String,
    val name: String,
    val rssi: Int,
    val deviceType: DeviceType,
    var isConnecting: Boolean = false,
    var isConnected: Boolean = false
) {
    fun getSignalStrengthText(): String {
        return when {
            rssi > -65 -> "强"
            rssi > -80 -> "中"
            else -> "弱"
        }
    }

    fun getIconResource(): Int {
        return when(deviceType) {
            DeviceType.LIGHT -> R.drawable.ic_bluetooth
            DeviceType.TEMPERATURE -> R.drawable.ic_bluetooth
            DeviceType.GENERIC -> R.drawable.ic_bluetooth
        }
    }
}

enum class DeviceType {
    LIGHT,
    TEMPERATURE,
    GENERIC
}