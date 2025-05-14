package cn.hellokk.ble.ui

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.hellokk.ble.MainActivity
import cn.hellokk.ble.R
import cn.hellokk.ble.bluetoothcontroller.Scene
import cn.hellokk.ble.bluetoothcontroller.SceneCategory
import cn.hellokk.ble.ui.adapter.SceneAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout

/**
 * 作者: Kun on 2025/5/9.
 * 邮箱: vip@hellokk.cc.
 * 描述: 场景页面
 */
class ScenesActivity : AppCompatActivity() {

    private lateinit var searchView: SearchView
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerViewMyScenes: RecyclerView
    private lateinit var recyclerViewRecommendedScenes: RecyclerView
    private lateinit var textViewCreateScene: TextView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var buttonCreateScene: CardView
    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var mySceneAdapter: SceneAdapter
    private lateinit var recommendedSceneAdapter: SceneAdapter

    private val myScenes = mutableListOf<Scene>()
    private val recommendedScenes = mutableListOf<Scene>()
    private var currentCategory = SceneCategory.ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scenes)

        initViews()
        setupTabLayout()
        setupAdapters()
        loadScenes()
        setupListeners()
        updateEmptyState()
        setStatusBarColor(R.color.black, R.color.black, true)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setStatusBarColor(statusBarColor: Int, navigationBarColor: Int, lightStatusBars: Boolean){
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = resources.getColor(statusBarColor, theme)
        window.navigationBarColor = resources.getColor(navigationBarColor, theme)
    }

    private fun initViews() {
        searchView = findViewById(R.id.searchView)
        tabLayout = findViewById(R.id.tabLayout)
        recyclerViewMyScenes = findViewById(R.id.recyclerViewMyScenes)
        recyclerViewRecommendedScenes = findViewById(R.id.recyclerViewRecommendedScenes)
        textViewCreateScene = findViewById(R.id.textViewCreateScene)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        buttonCreateScene = findViewById(R.id.buttonCreateScene)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        // 设置底部导航栏选中项
        bottomNavigation.selectedItemId = R.id.navigation_scenes
    }

    private fun setupTabLayout() {
        // 设置初始选中标签的背景
        setSelectedTabBackground(0)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                setSelectedTabBackground(tab.position)

                // 更新当前类别
                currentCategory = when (tab.position) {
                    0 -> SceneCategory.ALL
                    1 -> SceneCategory.LIGHTING
                    2 -> SceneCategory.AMBIENT
                    3 -> SceneCategory.OTHER
                    else -> SceneCategory.ALL
                }

                // 过滤场景
                filterScenesByCategory()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                tab.view.background = ContextCompat.getDrawable(
                    this@ScenesActivity,
                    R.drawable.unselected_tab_background
                )
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                // 不需要处理
            }
        })
    }

    private fun setSelectedTabBackground(position: Int) {
        // 设置所有标签为未选中状态
        for (i in 0 until tabLayout.tabCount) {
            tabLayout.getTabAt(i)?.view?.background = ContextCompat.getDrawable(
                this,
                R.drawable.unselected_tab_background
            )
        }

        // 设置选中标签的背景
        tabLayout.getTabAt(position)?.view?.background = ContextCompat.getDrawable(
            this,
            R.drawable.selected_tab_background
        )
    }

    private fun setupAdapters() {
        // 设置我的场景适配器
        recyclerViewMyScenes.layoutManager = GridLayoutManager(this, 2)
        mySceneAdapter = SceneAdapter(
            myScenes,
            onSceneClicked = { scene ->
                navigateToSceneDetail(scene)
            },
            onSceneToggled = { scene, isActive ->
                toggleScene(scene, isActive)
            }
        )
        recyclerViewMyScenes.adapter = mySceneAdapter

        // 设置推荐场景适配器
        recyclerViewRecommendedScenes.layoutManager = GridLayoutManager(this, 2)
        recommendedSceneAdapter = SceneAdapter(
            recommendedScenes,
            onSceneClicked = { scene ->
                navigateToSceneDetail(scene)
            },
            onSceneToggled = { scene, isActive ->
                toggleScene(scene, isActive)
            }
        )
        recyclerViewRecommendedScenes.adapter = recommendedSceneAdapter
    }

    private fun loadScenes() {
        // 加载我的场景数据
        myScenes.addAll(
            listOf(
                Scene(
                    id = "custom1",
                    name = "阅读模式",
                    iconRes = R.drawable.ic_bluetooth,
                    backgroundRes = R.drawable.edit_text_background,
                    isActive = true,
                    isCustom = true,
                    category = SceneCategory.LIGHTING,
                    devices = listOf("device1")
                ),
                Scene(
                    id = "custom2",
                    name = "晚安模式",
                    iconRes = R.drawable.ic_bluetooth,
                    backgroundRes = R.drawable.edit_text_background,
                    isActive = false,
                    isCustom = true,
                    category = SceneCategory.AMBIENT,
                    devices = listOf("device1", "device2")
                )
            )
        )

        // 加载推荐场景数据
        recommendedScenes.addAll(
            listOf(
                Scene(
                    id = "rec1",
                    name = "派对模式",
                    iconRes = R.drawable.ic_bluetooth,
                    backgroundRes = R.drawable.edit_text_background,
                    category = SceneCategory.LIGHTING
                ),
                Scene(
                    id = "rec2",
                    name = "电影模式",
                    iconRes = R.drawable.ic_bluetooth,
                    backgroundRes = R.drawable.edit_text_background,
                    category = SceneCategory.AMBIENT
                ),
                Scene(
                    id = "rec3",
                    name = "工作模式",
                    iconRes = R.drawable.ic_bluetooth,
                    backgroundRes = R.drawable.edit_text_background,
                    category = SceneCategory.LIGHTING
                ),
                Scene(
                    id = "rec4",
                    name = "浪漫模式",
                    iconRes = R.drawable.ic_bluetooth,
                    backgroundRes = R.drawable.edit_text_background,
                    category = SceneCategory.AMBIENT
                )
            )
        )

        mySceneAdapter.notifyDataSetChanged()
        recommendedSceneAdapter.notifyDataSetChanged()
    }

    private fun setupListeners() {
        // 搜索监听
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterScenesByQuery(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterScenesByQuery(newText)
                return true
            }
        })

        // 创建场景按钮点击
        textViewCreateScene.setOnClickListener {
            navigateToCreateScene()
        }

        buttonCreateScene.setOnClickListener {
            navigateToCreateScene()
        }

        // 底部导航栏点击
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    navigateToMainActivity()
                    true
                }
                R.id.navigation_scenes -> {
                    // 已经在场景页面，不需要处理
                    true
                }
                R.id.navigation_profile -> {
                    navigateToProfileActivity()
                    true
                }
                else -> false
            }
        }
    }

    private fun filterScenesByCategory() {
        if (currentCategory == SceneCategory.ALL) {
            mySceneAdapter.updateScenes(myScenes)
            recommendedSceneAdapter.updateScenes(recommendedScenes)
        } else {
            val filteredMyScenes = myScenes.filter { it.category == currentCategory }
            val filteredRecommendedScenes = recommendedScenes.filter { it.category == currentCategory }

            mySceneAdapter.updateScenes(filteredMyScenes)
            recommendedSceneAdapter.updateScenes(filteredRecommendedScenes)
        }

        updateEmptyState()
    }

    private fun filterScenesByQuery(query: String?) {
        if (query.isNullOrBlank()) {
            filterScenesByCategory() // 恢复分类过滤
            return
        }

        val lowercaseQuery = query.lowercase()

        val filteredMyScenes = myScenes.filter {
            it.name.lowercase().contains(lowercaseQuery) &&
                    (currentCategory == SceneCategory.ALL || it.category == currentCategory)
        }

        val filteredRecommendedScenes = recommendedScenes.filter {
            it.name.lowercase().contains(lowercaseQuery) &&
                    (currentCategory == SceneCategory.ALL || it.category == currentCategory)
        }

        mySceneAdapter.updateScenes(filteredMyScenes)
        recommendedSceneAdapter.updateScenes(filteredRecommendedScenes)

        updateEmptyState()
    }

    private fun updateEmptyState() {
        val hasMyScenes = mySceneAdapter.itemCount > 0
        val hasRecommendedScenes = recommendedSceneAdapter.itemCount > 0

        // 如果同时没有我的场景和推荐场景，显示空状态
        layoutEmptyState.visibility = if (!hasMyScenes && !hasRecommendedScenes) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // 隐藏/显示RecyclerView标题
        recyclerViewMyScenes.visibility = if (hasMyScenes) View.VISIBLE else View.GONE
        recyclerViewRecommendedScenes.visibility = if (hasRecommendedScenes) View.VISIBLE else View.GONE
    }

    private fun toggleScene(scene: Scene, isActive: Boolean) {
        // 更新UI状态
        if (scene.isCustom) {
            // 更新我的场景
            mySceneAdapter.updateSceneState(scene.id, isActive)
        } else {
            // 更新推荐场景
            recommendedSceneAdapter.updateSceneState(scene.id, isActive)
        }

        // 实际应用中，这里应该发送命令到设备执行场景
        val message = if (isActive) "已启用: ${scene.name}" else "已停用: ${scene.name}"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        // 如果激活一个场景，可能需要停用其他冲突的场景
        if (isActive) {
            deactivateConflictingScenes(scene)
        }
    }

    private fun deactivateConflictingScenes(activeScene: Scene) {
        // 在实际应用中，您需要定义场景之间的冲突规则
        // 这里简单示例：一次只能激活一个场景

        // 停用我的场景中其他场景
        myScenes.forEach { scene ->
            if (scene.id != activeScene.id && scene.isActive) {
                mySceneAdapter.updateSceneState(scene.id, false)
            }
        }

        // 停用推荐场景中的场景
        recommendedScenes.forEach { scene ->
            if (scene.id != activeScene.id && scene.isActive) {
                recommendedSceneAdapter.updateSceneState(scene.id, false)
            }
        }
    }

    private fun navigateToSceneDetail(scene: Scene) {
        val intent = Intent(this, SceneDetailActivity::class.java).apply {
            putExtra("SCENE_ID", scene.id)
            putExtra("SCENE_NAME", scene.name)
            putExtra("IS_CUSTOM", scene.isCustom)
        }
        startActivity(intent)
    }

    private fun navigateToCreateScene() {
//        val intent = Intent(this, CreateSceneActivity::class.java)
//        startActivity(intent)
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(0, 0) // 避免动画闪烁
        finish()
    }

    private fun navigateToProfileActivity() {
        val intent = Intent(this, ProfileActivity::class.java)
        startActivity(intent)
        overridePendingTransition(0, 0) // 避免动画闪烁
        finish()
    }

    override fun onResume() {
        super.onResume()
        // 刷新场景列表
        // 在实际应用中，这里应该重新加载最新的场景数据
    }
}