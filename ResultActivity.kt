package com.example.tryagian

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tryagian.databinding.ActivityResultBinding
import org.json.JSONException
import org.json.JSONObject

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        displayResult()
    }

    private fun displayResult() {
        val resultJson = intent.getStringExtra("ocr_result")

        if (resultJson == null) {
            Toast.makeText(this, "未收到识别结果", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val jsonObject = JSONObject(resultJson)

            if (!jsonObject.has("Response")) {
                Toast.makeText(this, "响应格式错误", Toast.LENGTH_LONG).show()
                return
            }

            val response = jsonObject.getJSONObject("Response")

            // 移除显示完整响应的部分，直接解析字段
            // 如果有错误，显示错误
            if (response.has("Error")) {
                val error = response.getJSONObject("Error")
                val errorMsg = error.optString("Message", "未知错误")
                Toast.makeText(this, "错误: $errorMsg", Toast.LENGTH_LONG).show()
                return
            }

            // 直接解析字段（腾讯云OCR返回的字段）
            binding.tvName.text = response.optString("Name", "未识别")
            binding.tvGender.text = response.optString("Sex", "未识别")
            binding.tvNation.text = response.optString("Nation", "未识别")
            binding.tvBirthDate.text = response.optString("Birth", "未识别")
            binding.tvAddress.text = response.optString("Address", "未识别")
            binding.tvIdNumber.text = response.optString("IdNum", "未识别")


        } catch (e: Exception) {
            Toast.makeText(this, "解析失败: ${e.message}", Toast.LENGTH_LONG).show()
            // 调试：显示原始JSON
            binding.tvName.text = "原始响应:\n$resultJson"
        }
    }
}