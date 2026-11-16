package com.example.screenextract

import android.Manifest
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val intent = Intent(this, ScreenCaptureService::class.java).apply {
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenCaptureService.EXTRA_DATA, result.data)
            }
            ContextCompat.startForegroundService(this, intent)
            Toast.makeText(this, "悬浮球已开启，去抖音点它试试", Toast.LENGTH_LONG).show()
            moveTaskToBack(true)
        } else {
            Toast.makeText(this, "未授权截屏", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            // 1. 检查悬浮窗权限
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_LONG).show()
                try {
                    // 尝试跳转到特定应用的悬浮窗设置
                    startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    )
                } catch (e: Exception) {
                    // 如果跳转失败，打开通用的悬浮窗设置页
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                }
                return@setOnClickListener
            }

            // 2. 检查通知权限 (Android 13+)
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "请允许通知权限", Toast.LENGTH_SHORT).show()
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
                return@setOnClickListener
            }

            // 3. 请求截屏权限
            Toast.makeText(this, "准备弹出截屏授权...", Toast.LENGTH_SHORT).show()
            try {
                val mpm = getSystemService(MediaProjectionManager::class.java)
                screenCaptureLauncher.launch(mpm.createScreenCaptureIntent())
            } catch (e: Exception) {
                Toast.makeText(this, "拉起截屏失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
