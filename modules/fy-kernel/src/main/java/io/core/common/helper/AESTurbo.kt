package io.core.common.helper

import android.util.Base64
import android.util.LruCache
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object AESTurbo {

    private const val AES_CIPHER = "AES/CBC/PKCS5Padding"
    private const val IV_LENGTH = 16  // 16 bytes for AES

    private const val MAX_CACHE_SIZE = 10  // 缓存最大密钥数量
    private val keyCache = LruCache<String, SecretKeySpec>(MAX_CACHE_SIZE)
    private val cipherThreadLocal = object : ThreadLocal<Cipher>() {
        override fun initialValue(): Cipher {
            return Cipher.getInstance(AES_CIPHER)
        }
    }

    /**
     * 使用任意字符串密钥加密
     * @param plaintext 明文
     * @param key 密钥字符串
     * @return Base64编码的加密结果（包含IV）
     * @throws IllegalArgumentException 如果输入参数为空
     * @throws CryptoException 加密过程中出现错误
     */
    @JvmStatic
    @Throws(IllegalArgumentException::class, CryptoException::class)
    fun encrypt(plaintext: String, key: String): String {
        require(plaintext.isNotEmpty()) { "Plaintext cannot be empty" }
        require(key.isNotEmpty()) { "Key cannot be empty" }

        return try {
            val secretKey = generateKey(key)
            val iv = generateIv()
            val cipher = cipherThreadLocal.get()
            require(cipher != null) { "Cipher cannot be null" }
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
            val encryptedBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(iv + encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            throw CryptoException("Encryption failed", e)
        }
    }

    /**
     * 使用相同字符串密钥解密
     * @param encryptedText Base64编码的加密结果
     * @param key 加密时使用的密钥字符串
     * @return 解密后的原始字符串
     * @throws IllegalArgumentException 如果输入参数为空
     * @throws CryptoException 解密过程中出现错误
     */
    @JvmStatic
    @Throws(IllegalArgumentException::class, CryptoException::class)
    fun decrypt(encryptedText: String, key: String): String {
        require(encryptedText.isNotEmpty()) { "Encrypted text cannot be empty" }
        require(key.isNotEmpty()) { "Key cannot be empty" }

        return try {
            val (iv, cipherText) = extractIvAndCipherText(encryptedText)
            val secretKey = generateKey(key)
            val cipher = cipherThreadLocal.get()
            require(cipher != null) { "Cipher cannot be null" }
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (e: Exception) {
            throw CryptoException("Decryption failed", e)
        }
    }

    /**
     * 安全加密方法
     * @param plaintext 明文
     * @param key 密钥字符串
     * @return Base64编码的加密结果（包含IV），加密失败时返回null
     */
    @JvmStatic
    fun encryptOrNull(plaintext: String, key: String): String? {
        return try {
            encrypt(plaintext, key)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 安全解密方法，解密失败时返回null
     * @param encryptedText Base64编码的加密结果
     * @param key 加密时使用的密钥字符串
     * @return 解密后的原始字符串，解密失败时返回null
     */
    @JvmStatic
    fun decryptOrNull(encryptedText: String, key: String): String? {
        return try {
            decrypt(encryptedText, key)
        } catch (e: Exception) {
            null
        }
    }

    private fun extractIvAndCipherText(encryptedText: String): Pair<ByteArray, ByteArray> {
        val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, IV_LENGTH)
        val cipherText = combined.copyOfRange(IV_LENGTH, combined.size)
        return Pair(iv, cipherText)
    }

    /**
     * 通过任意长度密钥生成或获取缓存的AES密钥
     * @param key 原始密钥字符串
     * @param iterations 哈希迭代次数(默认10000次)
     */
    private fun generateKey(key: String, iterations: Int = 10000): SecretKeySpec {
        return keyCache.get(key) ?: run {
            val digest = MessageDigest.getInstance("SHA-256")
            var keyBytes = key.toByteArray(Charsets.UTF_8)
            repeat(iterations) {
                keyBytes = digest.digest(keyBytes)
            }
            SecretKeySpec(keyBytes, "AES").also {
                keyCache.put(key, it)
            }
        }
    }

    /**
     * 生成安全的随机IV
     */
    private fun generateIv(): ByteArray {
        val iv = ByteArray(IV_LENGTH)
        SecureRandom().nextBytes(iv)
        return iv
    }

    class CryptoException(message: String, cause: Throwable) : Exception(message, cause)
}