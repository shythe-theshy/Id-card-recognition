package com.example.tryagian

import android.util.Log
import java.security.MessageDigest
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
    private const val REGION = "ap-guangzhou" // 注意：广州通常是 ap-guangzhou，确认官网文档
    private const val VERSION = "2018-11-19"
    private const val ALGORITHM = "TC3-HMAC-SHA256"

    init {
        loadConfig()
    }

    private fun loadConfig() {
        try {
            // 确保 BuildConfig 已正确生成
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
        return !secretId.isNullOrEmpty() && !secretKey.isNullOrEmpty()
    }

    /**
     * 获取请求头，包含签名
     * 修复点：统一生成 timestamp，并传递给 sign 方法，保证签名和 Header 中的时间戳一致
     */
    @Throws(Exception::class)
    fun getHeaders(payload: String): Map<String, String> {
        if (!isConfigured()) throw IllegalStateException("腾讯云密钥未配置")

        // 1. 在这里统一生成时间戳
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        
        // 2. 将 timestamp 传入 sign 方法
        val authorization = sign(payload, timestamp)

        return mapOf(
            "Content-Type" to "application/json; charset=utf-8",
            "Host" to ENDPOINT,
            "X-TC-Action" to ACTION,
            "X-TC-Version" to VERSION,
            "X-TC-Timestamp" to timestamp,
            "X-TC-Region" to REGION,
            "Authorization" to authorization
        )
    }

    /**
     * 生成签名
     * 修复点：接收外部传入的 timestamp，不再内部重新生成
     */
    @Throws(Exception::class)
    private fun sign(payload: String, timestamp: String): String {
        val currentSecretId = secretId ?: throw IllegalStateException("SecretId is null")
        val currentSecretKey = secretKey ?: throw IllegalStateException("SecretKey is null")

        // 日期格式化 (UTC)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        // 注意：这里需要用 timestamp 还原出 date，而不是用 new Date()，防止跨天/跨秒误差
        // 但腾讯云 V3 签名规范里，Date 是从 Timestamp 推导的，或者直接用当天 UTC 日期。
        // 为了严谨，我们用 timestamp * 1000 来生成 date，确保与 timestamp 严格对应
        val date = sdf.format(Date(timestamp.toLong() * 1000))

        // *************** 步骤 1：拼接规范请求串 ***************
        val httpRequestMethod = "POST"
        val canonicalUri = "/"
        val canonicalQueryString = ""
        
        // 注意：Header Value 建议去除多余空格，保持紧凑，防止服务端 Trim 导致不一致
        val canonicalHeaders = "content-type:application/json;charset=utf-8\nhost:$ENDPOINT\n"
        val signedHeaders = "content-type;host"
        val hashedPayload = sha256Hex(payload)

        val canonicalRequest = "$httpRequestMethod\n$canonicalUri\n$canonicalQueryString\n$canonicalHeaders\n$signedHeaders\n$hashedPayload"

        // *************** 步骤 2：拼接待签名字符串 ***************
        val credentialScope = "$date/$SERVICE/tc3_request"
        val hashedCanonicalRequest = sha256Hex(canonicalRequest)

        val stringToSign = "$ALGORITHM\n$timestamp\n$credentialScope\n$hashedCanonicalRequest"

        // *************** 步骤 3：计算签名 ***************
        val secretDate = hmacSha256(("TC3$currentSecretKey").toByteArray(Charsets.UTF_8), date)
        val secretService = hmacSha256(secretDate, SERVICE)
        val secretSigning = hmacSha256(secretService, "tc3_request")
        val signatureBytes = hmacSha256(secretSigning, stringToSign)
        val signature = bytesToHex(signatureBytes)

        // *************** 步骤 4：拼接 Authorization ***************
        return "$ALGORITHM Credential=$currentSecretId/$credentialScope, SignedHeaders=$signedHeaders, Signature=$signature"
    }

    private fun sha256Hex(data: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(data.toByteArray(Charsets.UTF_8))
        return bytesToHex(hash)
    }

    private fun hmacSha256(key: ByteArray, data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(key, "HmacSHA256")
        mac.init(secretKeySpec)
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
