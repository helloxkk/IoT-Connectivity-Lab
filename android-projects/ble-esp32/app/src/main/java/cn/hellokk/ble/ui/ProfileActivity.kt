package cn.hellokk.ble.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import cn.hellokk.ble.MainActivity
import cn.hellokk.ble.R
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * 作者: Kun on 2025/5/9.
 * 邮箱: vip@hellokk.cc.
 * 描述: 我的页面
 */
class ProfileActivity : AppCompatActivity() {

    private lateinit var textViewUsername: TextView
    private lateinit var textViewUserInfo: TextView
    private lateinit var cardViewAvatar: CardView
    private lateinit var layoutNotifications: LinearLayout
    private lateinit var layoutFavoriteScenes: LinearLayout
    private lateinit var layoutHistory: LinearLayout
    private lateinit var layoutSettings: LinearLayout
    private lateinit var layoutHelp: LinearLayout
    private lateinit var layoutAbout: LinearLayout
    private lateinit var buttonLogout: CardView
    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        initViews()
        setupUserInfo()
        setupListeners()
        setStatusBarColor(R.color.black, R.color.black, true)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setStatusBarColor(statusBarColor: Int, navigationBarColor: Int, lightStatusBars: Boolean){
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = resources.getColor(statusBarColor, theme)
        window.navigationBarColor = resources.getColor(navigationBarColor, theme)
    }

    private fun initViews() {
        textViewUsername = findViewById(R.id.textViewUsername)
        textViewUserInfo = findViewById(R.id.textViewUserInfo)
        cardViewAvatar = findViewById(R.id.cardViewAvatar)
        layoutNotifications = findViewById(R.id.layoutNotifications)
        layoutFavoriteScenes = findViewById(R.id.layoutFavoriteScenes)
        layoutHistory = findViewById(R.id.layoutHistory)
        layoutSettings = findViewById(R.id.layoutSettings)
        layoutHelp = findViewById(R.id.layoutHelp)
        layoutAbout = findViewById(R.id.layoutAbout)
        buttonLogout = findViewById(R.id.buttonLogout)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        // 设置底部导航栏选中项
        bottomNavigation.selectedItemId = R.id.navigation_profile
    }

    private fun setupUserInfo() {
        // 在实际应用中，这里应该从SharedPreferences或其他存储中获取用户信息
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        val username = sharedPreferences.getString("username", "智能家居用户")
        val email = sharedPreferences.getString("email", "user@example.com")

        textViewUsername.text = username
        textViewUserInfo.text = "账号: $email"
    }

    private fun setupListeners() {
        // 设置头像点击事件
        cardViewAvatar.setOnClickListener {
            showProfileEditDialog()
        }

        // 设置菜单项点击事件
        layoutNotifications.setOnClickListener {
            navigateTo(NotificationsActivity::class.java)
        }

        layoutFavoriteScenes.setOnClickListener {
//            navigateTo(FavoriteScenesActivity::class.java)
        }

        layoutHistory.setOnClickListener {
//            navigateTo(HistoryActivity::class.java)
        }

        layoutSettings.setOnClickListener {
//            navigateTo(SettingsActivity::class.java)
        }

        layoutHelp.setOnClickListener {
//            navigateTo(HelpActivity::class.java)
        }

        layoutAbout.setOnClickListener {
            showAboutDialog()
        }

        // 设置退出登录按钮点击事件
        buttonLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        // 设置底部导航栏点击事件
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    navigateToMainActivity()
                    true
                }
                R.id.navigation_scenes -> {
                    navigateToScenesActivity()
                    true
                }
                R.id.navigation_profile -> {
                    // 已经在个人中心页面，不需要处理
                    true
                }
                else -> false
            }
        }
    }

    private fun showProfileEditDialog() {
        // 在实际应用中，这里应该跳转到编辑个人资料页面
        // 这里使用对话框简单演示
        val currentUsername = textViewUsername.text.toString()
        val currentEmail = textViewUserInfo.text.toString().replace("账号: ", "")

        val items = arrayOf("修改用户名", "修改头像", "修改邮箱")

        AlertDialog.Builder(this)
            .setTitle("编辑个人资料")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> showEditUsernameDialog(currentUsername)
                    1 -> Toast.makeText(this, "修改头像功能开发中", Toast.LENGTH_SHORT).show()
                    2 -> showEditEmailDialog(currentEmail)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showEditUsernameDialog(currentUsername: String) {
        val view = layoutInflater.inflate(R.layout.dialog_edit_text, null)
        val editText = view.findViewById<TextView>(R.id.editText)
        editText.text = currentUsername

        AlertDialog.Builder(this)
            .setTitle("修改用户名")
            .setView(view)
            .setPositiveButton("保存") { _, _ ->
                val newUsername = editText.text.toString()
                if (newUsername.isNotEmpty()) {
                    textViewUsername.text = newUsername

                    // 保存到SharedPreferences
                    sharedPreferences.edit().putString("username", newUsername).apply()

                    Toast.makeText(this, "用户名已更新", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showEditEmailDialog(currentEmail: String) {
        val view = layoutInflater.inflate(R.layout.dialog_edit_text, null)
        val editText = view.findViewById<TextView>(R.id.editText)
        editText.text = currentEmail

        AlertDialog.Builder(this)
            .setTitle("修改邮箱")
            .setView(view)
            .setPositiveButton("保存") { _, _ ->
                val newEmail = editText.text.toString()
                if (newEmail.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                    textViewUserInfo.text = "账号: $newEmail"

                    // 保存到SharedPreferences
                    sharedPreferences.edit().putString("email", newEmail).apply()

                    Toast.makeText(this, "邮箱已更新", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "请输入有效的邮箱地址", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("关于应用")
            .setMessage("智能蓝牙控制 App\n\n版本: 1.0.0\n\n该应用用于控制ESP32蓝牙设备，实现RGB LED灯和其他设备的远程控制。\n\n© 2023 智能蓝牙控制")
            .setPositiveButton("确定", null)
            .show()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("退出登录")
            .setMessage("确定要退出当前账号吗？")
            .setPositiveButton("退出") { _, _ ->
                performLogout()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun performLogout() {
        // 在实际应用中，这里应该清除用户登录状态和相关数据
//        Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show()
//
//        // 跳转到登录页面
//        val intent = Intent(this, LoginActivity::class.java)
//        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        startActivity(intent)
//        finish()
    }

    private fun navigateTo(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(0, 0) // 避免动画闪烁
        finish()
    }

    private fun navigateToScenesActivity() {
        val intent = Intent(this, ScenesActivity::class.java)
        startActivity(intent)
        overridePendingTransition(0, 0) // 避免动画闪烁
        finish()
    }
}