package cn.hellokk.ble.ui.adapter

import cn.hellokk.ble.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import cn.hellokk.ble.bluetoothcontroller.Scene

/**
 * 作者: Kun on 2025/5/9.
 * 邮箱: vip@hellokk.cc.
 * 描述: 场景适配器
 */
class SceneAdapter(
    private var scenes: List<Scene>,
    private val onSceneClicked: (Scene) -> Unit,
    private val onSceneToggled: (Scene, Boolean) -> Unit
) : RecyclerView.Adapter<SceneAdapter.SceneViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SceneViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scene, parent, false)
        return SceneViewHolder(view)
    }

    override fun onBindViewHolder(holder: SceneViewHolder, position: Int) {
        val scene = scenes[position]
        holder.bind(scene)
    }

    override fun getItemCount(): Int = scenes.size

    fun updateScenes(newScenes: List<Scene>) {
        scenes = newScenes
        notifyDataSetChanged()
    }

    fun updateSceneState(sceneId: String, isActive: Boolean) {
        val index = scenes.indexOfFirst { it.id == sceneId }
        if (index != -1) {
            scenes = scenes.toMutableList().apply {
                this[index] = this[index].copy(isActive = isActive)
            }
            notifyItemChanged(index)
        }
    }

    inner class SceneViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageViewSceneBackground: ImageView = itemView.findViewById(R.id.imageViewSceneBackground)
        private val imageViewSceneIcon: ImageView = itemView.findViewById(R.id.imageViewSceneIcon)
        private val textViewSceneName: TextView = itemView.findViewById(R.id.textViewSceneName)
        private val switchScene: SwitchCompat = itemView.findViewById(R.id.switchScene)

        fun bind(scene: Scene) {
            textViewSceneName.text = scene.name
            imageViewSceneBackground.setImageResource(scene.backgroundRes)
            imageViewSceneIcon.setImageResource(scene.iconRes)
            switchScene.isChecked = scene.isActive

            switchScene.setOnCheckedChangeListener { _, isChecked ->
                onSceneToggled(scene, isChecked)
            }

            itemView.setOnClickListener {
                onSceneClicked(scene)
            }
        }
    }
}