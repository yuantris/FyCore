@file:JvmName("CryptoKt")

package io.core.common.helper

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

sealed class EncryptionResult {
    abstract val data: ByteArray?
    abstract val exception: Exception?

    data class Success(override val data: ByteArray) : EncryptionResult() {
        override val exception: Exception? = null

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            return data.contentEquals((other as Success).data)
        }

        override fun hashCode(): Int = data.contentHashCode()
    }

    data class Error(override val exception: Exception) : EncryptionResult() {
        override val data: ByteArray? = null
    }

    @JvmName("isSuccess")
    fun isSuccess() = this is Success

    @JvmName("isError")
    fun isError() = this is Error
}

interface CryptoProvider {
    @Throws(Exception::class)
    fun encrypt(data: ByteArray): EncryptionResult

    @Throws(Exception::class)
    fun decrypt(data: ByteArray): EncryptionResult

    /** Java友好API */
    @JvmDefault
    fun encryptOrThrow(data: ByteArray): ByteArray {
        return when (val result = encrypt(data)) {
            is EncryptionResult.Success -> result.data
            is EncryptionResult.Error -> throw result.exception
        }
    }

    /** Java友好API */
    @JvmDefault
    fun decryptOrThrow(data: ByteArray): ByteArray {
        return when (val result = decrypt(data)) {
            is EncryptionResult.Success -> result.data
            is EncryptionResult.Error -> throw result.exception
        }
    }
}

object Crypto {
    @JvmStatic
    fun sha256(data: ByteArray) = data.sha256Hash()

    @JvmStatic
    fun md5(data: ByteArray) = CryptoGT.md5Hash(data)

    @JvmStatic
    fun base64Encode(data: ByteArray) = CryptoGT.base64Encode(data)

    @JvmStatic
    fun base64Decode(data: String) = CryptoGT.base64Decode(data)
}

/** Kotlin扩展函数 */
fun ByteArray.sha256Hash(): ByteArray = MessageDigest.getInstance("SHA-256").digest(this)

class AesCrypto private constructor(
    private val secretKey: SecretKey,
    private val transformation: String = "AES/GCM/NoPadding"
) : CryptoProvider {

    companion object {
        private const val KEY_ALGORITHM = "AES"
        private const val GCM_TAG_LENGTH = 128

        @JvmStatic
        fun generateKey(keySize: Int = 256): SecretKey {
            val keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM)
            keyGenerator.init(keySize)
            return keyGenerator.generateKey()
        }

        @JvmStatic
        fun getInstance(secretKey: SecretKey) = AesCrypto(secretKey)

        @JvmStatic
        fun generateKeyFromString(password: String): SecretKey {
            require(password.length >= 8) { "Password must be at least 8 characters" }
            val keyBytes = password.toByteArray().sha256Hash()
            return SecretKeySpec(keyBytes, KEY_ALGORITHM)
        }

        @JvmStatic
        fun create(password: String) = AesCrypto(generateKeyFromString(password))
    }

    override fun encrypt(data: ByteArray): EncryptionResult = try {
        val cipher = Cipher.getInstance(transformation).apply {
            init(Cipher.ENCRYPT_MODE, secretKey)
        }
        EncryptionResult.Success(cipher.iv + cipher.doFinal(data))
    } catch (e: Exception) {
        EncryptionResult.Error(e)
    }

    override fun decrypt(data: ByteArray): EncryptionResult = try {
        val cipher = Cipher.getInstance(transformation).apply {
            init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, data.copyOfRange(0, 12)))
        }
        EncryptionResult.Success(cipher.doFinal(data.copyOfRange(12, data.size)))
    } catch (e: Exception) {
        EncryptionResult.Error(e)
    }
}

class RsaCrypto private constructor(
    private val keyAlias: String
) : CryptoProvider {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val TRANSFORMATION = "RSA/ECB/PKCS1Padding"
        private const val KEY_SIZE = 2048

        @JvmStatic
        @Throws(Exception::class)
        fun generateKeyPair(keyAlias: String) {
            KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
                load(null)
                if (!containsAlias(keyAlias)) {
                    KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_RSA,
                        KEYSTORE_PROVIDER
                    ).apply {
                        init(
                            KeyGenParameterSpec.Builder(
                                keyAlias,
                                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                            )
                                .setKeySize(KEY_SIZE)
                                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                                .build()
                        )
                        generateKey()
                    }
                }
            }
        }

        @JvmStatic
        fun create(keyAlias: String) = RsaCrypto(keyAlias)
    }

    override fun encrypt(data: ByteArray): EncryptionResult = try {
        val publicKey = KeyStore.getInstance(KEYSTORE_PROVIDER).run {
            load(null)
            getCertificate(keyAlias).publicKey
        }

        Cipher.getInstance(TRANSFORMATION).run {
            init(Cipher.ENCRYPT_MODE, publicKey)
            EncryptionResult.Success(processInBlocks(data, blockSize = 214))
        }
    } catch (e: Exception) {
        EncryptionResult.Error(e)
    }

    override fun decrypt(data: ByteArray): EncryptionResult = try {
        val privateKey = KeyStore.getInstance(KEYSTORE_PROVIDER).run {
            load(null)
            getEntry(keyAlias, null) as KeyStore.PrivateKeyEntry
        }.privateKey

        Cipher.getInstance(TRANSFORMATION).run {
            init(Cipher.DECRYPT_MODE, privateKey)
            EncryptionResult.Success(processInBlocks(data, blockSize = 256))
        }
    } catch (e: Exception) {
        EncryptionResult.Error(e)
    }

    private fun Cipher.processInBlocks(data: ByteArray, blockSize: Int): ByteArray {
        return data.toList()
            .chunked(blockSize)
            .flatMap { doFinal(it.toByteArray()).toList() }
            .toByteArray()
    }
}

object CryptoGT {
    @JvmStatic
    fun base64Encode(data: ByteArray, flags: Int = Base64.NO_WRAP) =
        Base64.encodeToString(data, flags)

    @JvmStatic
    fun base64Decode(data: String, flags: Int = Base64.NO_WRAP) =
        Base64.decode(data, flags)

    @JvmStatic
    fun md5Hash(input: ByteArray): ByteArray =
        MessageDigest.getInstance("MD5").digest(input)

    @JvmStatic
    fun md5Hex(input: String): String =
        BigInteger(1, md5Hash(input.toByteArray())).toString(16).padStart(32, '0')

    @JvmStatic
    fun encryptFile(
        input: InputStream,
        output: OutputStream,
        crypto: CryptoProvider
    ) = processFile(input, output) { data -> crypto.encryptOrThrow(data) }

    @JvmStatic
    fun decryptFile(
        input: InputStream,
        output: OutputStream,
        crypto: CryptoProvider
    ) = processFile(input, output) { data -> crypto.decryptOrThrow(data) }

    private fun processFile(
        input: InputStream,
        output: OutputStream,
        processor: (ByteArray) -> ByteArray
    ): Boolean {
        return input.use { i ->
            output.use { o ->
                val buffer = ByteArray(1024 * 4)
                generateSequence { i.read(buffer).takeIf { it != -1 } }
                    .forEach { read ->
                        o.write(processor(buffer.copyOf(read)))
                    }
                true
            }
        }
    }
}