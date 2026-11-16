package com.example.screenextract

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CropActivity : AppCompatActivity() {

    companion object { const val EXTRA_PATH = "path" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop)

        val cropView = findViewById<CropView>(R.id.cropView)
        val path = intent.getStringExtra(EXTRA_PATH)
        if (path == null) { finish(); return }

        val bitmap = BitmapFactory.decodeFile(path)
        if (bitmap == null) { Toast.makeText(this, "读取截屏失败", Toast.LENGTH_SHORT).show(); finish(); return }
        cropView.setBitmap(bitmap)

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            val cropped = cropView.getCropBitmap()
            if (cropped == null) { Toast.makeText(this, "选区无效", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            if (ImageSaver.saveToGallery(this, cropped) != null) {
                Toast.makeText(this, "已保存到相册 Pictures/ScreenExtract", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
        findViewById<Button>(R.id.btnCancel).setOnClickListener { finish() }
    }
}
