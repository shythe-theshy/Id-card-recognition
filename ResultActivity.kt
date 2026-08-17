package com.example.tryagian

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tryagian.databinding.ActivityResultBinding
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
            Toast.makeText(this, R.string.no_result, Toast.LENGTH_LONG).show()
            return
        }

        try {
            val jsonObject = JSONObject(resultJson)
            if (!jsonObject.has("Response")) {
                Toast.makeText(this, R.string.response_format_error, Toast.LENGTH_LONG).show()
                return
            }

            val response = jsonObject.getJSONObject("Response")
            if (response.has("Error")) {
                val error = response.getJSONObject("Error")
                val errorMsg = error.optString("Message", getString(R.string.unknown_error))
                Toast.makeText(this, getString(R.string.error_prefix, errorMsg), Toast.LENGTH_LONG).show()
                return
            }

            binding.tvName.text = response.optString("Name", getString(R.string.not_recognized))
            binding.tvGender.text = response.optString("Sex", getString(R.string.not_recognized))
            binding.tvNation.text = response.optString("Nation", getString(R.string.not_recognized))
            binding.tvBirthDate.text = response.optString("Birth", getString(R.string.not_recognized))
            binding.tvAddress.text = response.optString("Address", getString(R.string.not_recognized))
            binding.tvIdNumber.text = response.optString("IdNum", getString(R.string.not_recognized))

        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.parse_error, e.message), Toast.LENGTH_LONG).show()
            binding.tvName.text = "原始响应:\n$resultJson"
        }
    }
}
