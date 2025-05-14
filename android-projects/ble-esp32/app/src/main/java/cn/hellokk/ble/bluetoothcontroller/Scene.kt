package cn.hellokk.ble.bluetoothcontroller

import androidx.annotation.DrawableRes

/**
 * 作者: Kun on 2025/5/9.
 * 邮箱: vip@hellokk.cc.
 * 描述: 场景数据类
 */
data class Scene(
    val id: String,
    val name: String,
    @DrawableRes val iconRes: Int,
    @DrawableRes val backgroundRes: Int,
    val isActive: Boolean = false,
    val isCustom: Boolean = false,
    val category: SceneCategory = SceneCategory.ALL,
    val devices: List<String> = emptyList()
)

enum class SceneCategory {
    ALL,
    LIGHTING,
    AMBIENT,
    OTHER
}