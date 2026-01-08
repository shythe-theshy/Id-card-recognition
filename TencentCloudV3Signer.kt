package com.example.tryagian

import android.util.Log
import java.security.InvalidKeyException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object TencentCloudV3Signer {

    private var secretId: String? = null
    private var secretKey: String? = null

    private const val ENDPOINT = "ocr.tencentcloudapi.com"
    private const val SERVICE = "ocr"
    private const val ACTION = "IDCardOCR"
    private const val REGION = "ap-guangzhou"
    private const val VERSION = "2018-11-19"
    private const val ALGORITHM = "TC3-HMAC-SHA256"

    init {
        loadConfig()
    }

    private fun loadConfig() {
        try {
            secretId = BuildConfig.TENCENT_SECRET_ID
            secretKey = BuildConfig.TENCENT_SECRET_KEY

            if (secretId.isNullOrEmpty() || secretKey.isNullOrEmpty()) {
                Log.w("TencentCloudV3Signer", "腾讯云密钥未配置！请检查 local.properties 文件。")
            } else {
                Log.i("TencentCloudV3Signer", "腾讯云密钥已加载")
            }
        } catch (e: Exception) {
            Log.e("TencentCloudV3Signer", "无法读取BuildConfig: ${e.message}")
        }
    }

    fun isConfigured(): Boolean {
        if (secretId == null || secretKey == null) {
            loadConfig()
        }
        return !secretId.isNullOrEmpty() && !secretKey.isNullOrEmpty()
    }

    @Throws(Exception::class)
    fun sign(payload: String): String {
        val secretId = secretId ?: throw IllegalStateException("腾讯云密钥未配置")
        val secretKey = secretKey ?: throw IllegalStateException("腾讯云密钥未配置")

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val date = sdf.format(Date())

        val canonicalRequest = buildString {
            append("POST\n")
            append("/\n")
            append("\n")
            append("content-type:application/json; charset=utf-8\n")
            append("host:$ENDPOINT\n")
            append("\n")
            append("content-type;host\n")
            append(sha256Hex(payload))
        }

        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val credentialScope = "$date/$SERVICE/tc3_request"
        val hashedCanonicalRequest = sha256Hex(canonicalRequest)

        val stringToSign = buildString {
            append("$ALGORITHM\n")
            append("$timestamp\n")
            append("$credentialScope\n")
            append(hashedCanonicalRequest)
        }

        val secretDate = hmacSha256(("TC3$secretKey").toByteArray(), date)
        val secretService = hmacSha256(secretDate, SERVICE)
        val secretSigning = hmacSha256(secretService, "tc3_request")
        val signatureBytes = hmacSha256(secretSigning, stringToSign)
        val signature = bytesToHex(signatureBytes).lowercase(Locale.getDefault())

        return "$ALGORITHM " +
                "Credential=$secretId/$credentialScope, " +
                "SignedHeaders=content-type;host, " +
                "Signature=$signature"
    }

    @Throws(Exception::class)
    fun getHeaders(payload: String): Map<String, String> {
        val timestamp = (System.currentTimeMillis() / 1000).toString()

        return mapOf(
            "Content-Type" to "application/json; charset=utf-8",
            "Host" to ENDPOINT,
            "X-TC-Action" to ACTION,
            "X-TC-Version" to VERSION,
            "X-TC-Timestamp" to timestamp,
            "X-TC-Region" to REGION,
            "Authorization" to sign(payload)
        )
    }

    @Throws(NoSuchAlgorithmException::class)
    private fun sha256Hex(data: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(data.toByteArray())
        return bytesToHex(hash).lowercase(Locale.getDefault())
    }

    @Throws(NoSuchAlgorithmException::class, InvalidKeyException::class)
    private fun hmacSha256(key: ByteArray, data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(key, "HmacSHA256")
        mac.init(secretKeySpec)
        return mac.doFinal(data.toByteArray())
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
}