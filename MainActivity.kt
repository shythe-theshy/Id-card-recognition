package com.example.tryagian

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.tryagian.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var selectedBitmap: Bitmap? = null
    private var currentPhotoPath: String? = null

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
            if (selectedBitmap == null) {
                Toast.makeText(this, R.string.please_select_image, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            recognizeIdCard()
        }
    }

    // ---------- 拍照 ----------
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

        val photoFile = createImageFile()
        currentPhotoPath = photoFile.absolutePath
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir("Pictures") ?: filesDir
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    // ---------- 从相册选择 ----------
    private fun selectImage() {
        // Android 13+ 不再需要 READ_EXTERNAL_STORAGE
        if (android.os.Build.VERSION.SDK_INT <= 32) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_STORAGE_PERMISSION
                )
                return
            }
        }
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return

        when (requestCode) {
            REQUEST_IMAGE_CAPTURE -> {
                currentPhotoPath?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        val bitmap = ImageUtils.decodeSampledBitmapFromFile(path, 1024, 1024)
                        selectedBitmap = bitmap
                        binding.ivIdCard.setImageBitmap(bitmap)
                        binding.btnRecognize.isEnabled = true
                    } else {
                        Toast.makeText(this, R.string.photo_save_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            REQUEST_PICK_IMAGE -> {
                data?.data?.let { uri ->
                    try {
                        val original = ImageUtils.getBitmapFromUri(this, uri)
                        val compressed = original?.let { ImageUtils.compressBitmap(it, 1024) }
                        selectedBitmap = compressed
                        binding.ivIdCard.setImageBitmap(compressed)
                        binding.btnRecognize.isEnabled = true
                    } catch (e: IOException) {
                        Toast.makeText(this, R.string.image_load_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // ---------- 识别 ----------
    private fun recognizeIdCard() {
        if (!TencentCloudV3Signer.isConfigured()) {
            Toast.makeText(this, R.string.config_missing, Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnRecognize.isEnabled = false
        binding.progressBar.visibility = android.view.View.VISIBLE

        lifecycleScope.launch {
            try {
                val bitmap = selectedBitmap ?: throw IllegalStateException("Bitmap is null")
                val base64Image = ImageUtils.bitmapToBase64(bitmap)

                val requestJson = JSONObject().apply {
                    put("ImageBase64", base64Image)
                    put("CardSide", "FRONT")
                }

                val payload = requestJson.toString()
                val headers = TencentCloudV3Signer.getHeaders(payload)
                val response = HttpUtil.sendPostRequest(
                    "https://ocr.tencentcloudapi.com/",
                    payload,
                    headers
                )

                withContext(Dispatchers.Main) {
                    Intent(this@MainActivity, ResultActivity::class.java).apply {
                        putExtra("ocr_result", response)
                        startActivity(this)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.recognition_failed, e.message),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.btnRecognize.isEnabled = true
                    binding.progressBar.visibility = android.view.View.GONE
                }
            }
        }
    }

    // ---------- 权限和配置检查 ----------
    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        }
        // 仅 Android 12 及以下需要 READ_EXTERNAL_STORAGE
        if (android.os.Build.VERSION.SDK_INT <= 32) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_STORAGE_PERMISSION
                )
            }
        }
    }

    private fun checkTencentConfig() {
        if (!TencentCloudV3Signer.isConfigured()) {
            AlertDialog.Builder(this)
                .setTitle(R.string.config_missing)
                .setMessage(
                    "请按以下步骤配置腾讯云密钥：\n\n" +
                            "1. 在项目根目录创建 local.properties 文件\n" +
                            "2. 添加以下内容：\n" +
                            "   TENCENT_SECRET_ID=你的SecretId\n" +
                            "   TENCENT_SECRET_KEY=你的SecretKey\n" +
                            "3. 重新同步项目"
                )
                .setPositiveButton(android.R.string.ok, null)
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
                    Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
