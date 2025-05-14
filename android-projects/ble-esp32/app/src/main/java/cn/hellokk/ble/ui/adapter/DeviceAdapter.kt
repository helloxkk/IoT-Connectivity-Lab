package cn.hellokk.ble.ui.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import cn.hellokk.ble.R
import cn.hellokk.ble.bluetoothcontroller.BluetoothDeviceInfo

/**
 * 作者: Kun on 2025/5/9.
 * 邮箱: vip@hellokk.cc.
 * 描述: 设备列表适配器
 */
class DeviceAdapter(
    private var devices: List<BluetoothDeviceInfo>,
    private val onDeviceClicked: (BluetoothDeviceInfo) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.bind(device)
    }

    override fun getItemCount(): Int = devices.size

    fun updateDevices(newDevices: List<BluetoothDeviceInfo>) {
        devices = newDevices
        notifyDataSetChanged()
    }

    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardViewDevice: CardView = itemView.findViewById(R.id.cardViewDevice)
        private val textViewDeviceName: TextView = itemView.findViewById(R.id.textViewDeviceName)
        private val textViewDeviceAddress: TextView = itemView.findViewById(R.id.textViewDeviceAddress)
        private val textViewSignalStrength: TextView = itemView.findViewById(R.id.textViewSignalStrength)
        private val buttonConnect: Button = itemView.findViewById(R.id.buttonConnect)

        fun bind(device: BluetoothDeviceInfo) {
            textViewDeviceName.text = device.name
            textViewDeviceAddress.text = device.address
            textViewSignalStrength.text = device.getSignalStrengthText()

            // 设置信号强度背景颜色
            val drawable = textViewSignalStrength.background as GradientDrawable
            drawable.setColor(device.getSignalStrengthColor())

            // 设置按钮状态
            buttonConnect.isEnabled = device.isConnectable
            buttonConnect.alpha = if (device.isConnectable) 1.0f else 0.5f

            // 设置点击事件（整个卡片和连接按钮）
            cardViewDevice.setOnClickListener {
                if (device.isConnectable) {
                    onDeviceClicked(device)
                }
            }

            buttonConnect.setOnClickListener {
                if (device.isConnectable) {
                    onDeviceClicked(device)
                }
            }

            // 添加连接按钮的点击动画
            buttonConnect.setOnTouchListener { v, event ->
                v.onTouchEvent(event)
                v.isPressed = true
                // 在按下后延迟复位，以便看到按下效果
                v.postDelayed({ v.isPressed = false }, 100)
                false
            }
        }
    }
}