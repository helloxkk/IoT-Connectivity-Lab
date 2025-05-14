package cn.hellokk.ble.ui.adapter

/**
 * 作者: Kun on 2025/5/9.
 * 邮箱: vip@hellokk.cc.
 * 描述: 首页设备列表适配器
 */
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import cn.hellokk.ble.R
import cn.hellokk.ble.bluetoothcontroller.BluetoothDeviceInfo

class DeviceMainAdapter(
    private var devices: List<BluetoothDeviceInfo>,
    private val onDeviceClicked: (BluetoothDeviceInfo) -> Unit
) : RecyclerView.Adapter<DeviceMainAdapter.DeviceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_main_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.bind(device)
    }

    override fun getItemCount(): Int = devices.size

    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardViewDevice: CardView = itemView.findViewById(R.id.cardViewDevice)
        private val imageViewDeviceIcon: ImageView = itemView.findViewById(R.id.imageViewDeviceIcon)
        private val textViewDeviceName: TextView = itemView.findViewById(R.id.textViewDeviceName)
        private val textViewDeviceStatus: TextView = itemView.findViewById(R.id.textViewDeviceStatus)
        private val textViewDeviceType: TextView = itemView.findViewById(R.id.textViewDeviceType)

        fun bind(device: BluetoothDeviceInfo) {
            textViewDeviceName.text = device.name
            textViewDeviceStatus.text = device.getStatusText()
            textViewDeviceStatus.setTextColor(device.getStatusColor())

            // 根据设备名称确定设备类型和图标
            val deviceType = when {
                device.name.contains("灯", ignoreCase = true) -> {
                    imageViewDeviceIcon.setImageResource(R.drawable.ic_bluetooth)
                    "智能灯"
                }
                device.name.contains("插座", ignoreCase = true) -> {
                    imageViewDeviceIcon.setImageResource(R.drawable.ic_bluetooth)
                    "智能插座"
                }
                device.name.contains("温湿度", ignoreCase = true)
                        || device.name.contains("传感器", ignoreCase = true) -> {
                    imageViewDeviceIcon.setImageResource(R.drawable.ic_bluetooth)
                    "传感器"
                }
                else -> {
                    imageViewDeviceIcon.setImageResource(R.drawable.ic_bluetooth)
                    "蓝牙设备"
                }
            }

            textViewDeviceType.text = deviceType

            // 设置点击事件
            cardViewDevice.setOnClickListener {
                onDeviceClicked(device)
            }

            // 根据连接状态调整透明度
            val alpha = if (device.isConnected) 1.0f else 0.7f
            cardViewDevice.alpha = alpha
        }
    }
}