package cn.hellokk.ble.ui.adapter

import androidx.recyclerview.widget.RecyclerView
import cn.hellokk.ble.bluetoothcontroller.ColorOption
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import cn.hellokk.ble.R

/**
 * 作者: Kun on 2025/5/9.
 * 邮箱: vip@hellokk.cc.
 * 描述: 颜色列表适配器
 */
class ColorAdapter(
    private var colors: List<ColorOption>,
    private val onColorSelected: (Int, Int) -> Unit
) : RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_color, parent, false)
        return ColorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        val colorOption = colors[position]
        holder.bind(colorOption)
        holder.itemView.setOnClickListener {
            // 更新选择状态并通知回调
            updateSelection(position)
            onColorSelected(position, colorOption.color)
        }
    }

    override fun getItemCount(): Int = colors.size

    fun updateSelection(selectedPosition: Int) {
        // 先创建新的列表，以免修改原始列表
        val updatedColors = colors.mapIndexed { index, colorOption ->
            colorOption.copy(isSelected = index == selectedPosition)
        }

        // 只有在真正有变化时才更新和刷新
        if (colors != updatedColors) {
            colors = updatedColors
            notifyDataSetChanged()
        }
    }

    inner class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewColor: View = itemView.findViewById(R.id.viewColor)
        private val viewSelectedBorder: View = itemView.findViewById(R.id.viewSelectedBorder)
        private val cardViewColor: CardView = itemView.findViewById(R.id.cardViewColor)

        fun bind(colorOption: ColorOption) {
            viewColor.setBackgroundColor(colorOption.color)
            viewSelectedBorder.visibility = if (colorOption.isSelected) View.VISIBLE else View.GONE

            // 特殊处理白色，添加边框以便在白色背景上可见
            if (colorOption.color == 0xFFFFFFFF.toInt()) {
                cardViewColor.setContentPadding(1, 1, 1, 1)
                cardViewColor.setCardBackgroundColor(0xFFE0E0E0.toInt())
            } else {
                cardViewColor.setContentPadding(0, 0, 0, 0)
                cardViewColor.setCardBackgroundColor(colorOption.color)
            }
        }
    }
}