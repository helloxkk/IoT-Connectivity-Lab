package cn.hellokk.ble.ui

import android.os.Build
import cn.hellokk.ble.R

import android.os.Bundle
import android.view.MenuItem
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

/**
 * 作者: Kun on 2025/5/9.
 * 邮箱: vip@hellokk.cc.
 * 描述: 场景详情
 */
class SceneDetailActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var textViewSceneName: TextView
    private lateinit var buttonBack: ImageButton
    private lateinit var buttonEdit: ImageButton

    private var sceneId: String = ""
    private var sceneName: String = ""
    private var isCustom: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scene_detail)

        // 获取场景信息
        sceneId = intent.getStringExtra("SCENE_ID") ?: ""
        sceneName = intent.getStringExtra("SCENE_NAME") ?: "未知场景"
        isCustom = intent.getBooleanExtra("IS_CUSTOM", false)

        initViews()
        setupToolbar()
        loadSceneDetails()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        textViewSceneName = findViewById(R.id.textViewTitle)
        buttonBack = findViewById(R.id.buttonBack)
        buttonEdit = findViewById(R.id.buttonMore)

        buttonBack.setOnClickListener {
            finish()
        }

        buttonEdit.setOnClickListener {
            if (isCustom) {
                // 编辑自定义场景
                Toast.makeText(this, "编辑场景功能开发中", Toast.LENGTH_SHORT).show()
            } else {
                // 提示不能编辑系统场景
                Toast.makeText(this, "系统预设场景不可编辑", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        textViewSceneName.text = sceneName
    }

    private fun loadSceneDetails() {
        // 在实际应用中，这里应该从数据库或其他存储中加载场景详情
        // 例如：执行场景的设备列表、场景触发条件等
    }
}