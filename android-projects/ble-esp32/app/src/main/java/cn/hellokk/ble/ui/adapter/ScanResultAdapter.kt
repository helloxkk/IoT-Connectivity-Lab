package cn.hellokk.ble.ui.adapter

import cn.hellokk.ble.R
import cn.hellokk.ble.bluetoothcontroller.BluetoothScanResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

/**
 * 作者: Kun on 2025/5/9.
 * 邮箱: vip@hellokk.cc.
 * 描述: 扫描结果适配器
 */
class ScanResultAdapter(
    private var devices: List<BluetoothScanResult>,
    private val onConnectClickListener: (BluetoothScanResult) -> Unit
) : RecyclerView.Adapter<ScanResultAdapter.DeviceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_found_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.bind(device)
    }

    override fun getItemCount(): Int = devices.size

    fun updateDevices(newDevices: List<BluetoothScanResult>) {
        devices = newDevices
        notifyDataSetChanged()
    }

    fun updateDeviceStatus(address: String, isConnecting: Boolean, isConnected: Boolean) {
        val index = devices.indexOfFirst { it.address == address }
        if (index != -1) {
            devices = devices.toMutableList().apply {
                this[index] = this[index].copy(
                    isConnecting = isConnecting,
                    isConnected = isConnected
                )
            }
            notifyItemChanged(index)
        }
    }

    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageViewDeviceIcon: ImageView = itemView.findViewById(R.id.imageViewDeviceIcon)
        private val textViewDeviceName: TextView = itemView.findViewById(R.id.textViewDeviceName)
        private val textViewSignalStrength: TextView = itemView.findViewById(R.id.textViewSignalStrength)
        private val buttonConnect: CardView = itemView.findViewById(R.id.buttonConnect)
        private val textViewConnectButton: TextView = itemView.findViewById(R.id.textViewConnectButton)

        fun bind(device: BluetoothScanResult) {
            textViewDeviceName.text = device.name
            textViewSignalStrength.text = device.getSignalStrengthText()
            imageViewDeviceIcon.setImageResource(device.getIconResource())

            // 设置连接按钮状态
            when {
                device.isConnected -> {
                    buttonConnect.setCardBackgroundColor(itemView.context.getColor(R.color.colorOnline))
                    textViewConnectButton.text = "已连接"
                }
                device.isConnecting -> {
                    buttonConnect.setCardBackgroundColor(itemView.context.getColor(R.color.colorGray))
                    textViewConnectButton.text = "连接中..."
                }
                else -> {
                    buttonConnect.setCardBackgroundColor(itemView.context.getColor(R.color.colorPrimary))
                    textViewConnectButton.text = "连接"
                    buttonConnect.setOnClickListener {
                        onConnectClickListener(device)
                    }
                }
            }
        }
    }
}