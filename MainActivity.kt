package com.example.tryagian

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.tryagian.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var selectedBitmap: Bitmap? = null
    private var isRecognizing = false

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_PICK_IMAGE = 2
        private const val REQUEST_CAMERA_PERMISSION = 100
        private const val REQUEST_STORAGE_PERMISSION = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        checkPermissions()
        checkTencentConfig()
    }

    private fun setupClickListeners() {
        binding.btnTakePhoto.setOnClickListener { takePhoto() }
        binding.btnSelectImage.setOnClickListener { selectImage() }
        binding.btnRecognize.setOnClickListener {
            if (selectedBitmap != null) {
                recognizeIdCard()
            } else {
                Toast.makeText(this, "请先选择身份证图片", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun takePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
            return
        }

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    private fun selectImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_STORAGE_PERMISSION
            )
            return
        }

        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    data?.extras?.let { extras ->
                        selectedBitmap = extras.get("data") as Bitmap?
                        selectedBitmap?.let {
                            binding.ivIdCard.setImageBitmap(it)
                            binding.btnRecognize.isEnabled = true
                        }
                    }
                }
                REQUEST_PICK_IMAGE -> {
                    data?.data?.let { imageUri ->
                        try {
                            selectedBitmap = ImageUtils.getBitmapFromUri(this, imageUri)
                            selectedBitmap?.let {
                                binding.ivIdCard.setImageBitmap(it)
                                binding.btnRecognize.isEnabled = true
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                            Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun recognizeIdCard() {
        if (!TencentCloudV3Signer.isConfigured()) {
            Toast.makeText(this, "请先配置腾讯云密钥", Toast.LENGTH_SHORT).show()
            return
        }

        isRecognizing = true
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnRecognize.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                selectedBitmap?.let { bitmap ->
                    val base64Image = ImageUtils.bitmapToBase64(bitmap)

                    val requestJson = JSONObject().apply {
                        put("ImageBase64", base64Image)
                        put("CardSide", "FRONT")
                    }

                    val payload = requestJson.toString()
                    val headers = TencentCloudV3Signer.getHeaders(payload)

                    val url = "https://ocr.tencentcloudapi.com/"
                    val response = HttpUtil.sendPostRequest(url, payload, headers)

                    val responseJson = JSONObject(response)

                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.btnRecognize.isEnabled = true
                        isRecognizing = false

                        Intent(this@MainActivity, ResultActivity::class.java).apply {
                            putExtra("ocr_result", response)
                            startActivity(this)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.btnRecognize.isEnabled = true
                    isRecognizing = false
                    Toast.makeText(this@MainActivity,
                        "识别失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_STORAGE_PERMISSION
            )
        }
    }

    private fun checkTencentConfig() {
        if (!TencentCloudV3Signer.isConfigured()) {
            AlertDialog.Builder(this)
                .setTitle("配置提示")
                .setMessage("请按以下步骤配置腾讯云密钥：\n\n" +
                        "1. 在项目根目录创建 local.properties 文件\n" +
                        "2. 添加以下内容：\n" +
                        "   TENCENT_SECRET_ID=你的SecretId\n" +
                        "   TENCENT_SECRET_KEY=你的SecretKey\n" +
                        "3. 重新同步项目")
                .setPositiveButton("确定", null)
                .show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "相机权限已授予", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}