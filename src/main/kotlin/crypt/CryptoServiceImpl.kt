package org.ouraircraft.crypt

import kotlinx.io.bytestring.ByteString
import org.ouraircraft.auth.RecoveryToken
import org.ouraircraft.auth.Token
import org.ouraircraft.model.services.CryptoService
import java.nio.file.Path
import java.util.Locale
import javax.crypto.KDF
import javax.crypto.SecretKey
import javax.crypto.spec.HKDFParameterSpec
import kotlin.io.path.readBytes

internal class CryptoServiceImpl: CryptoService {
    private val emailHmacKey = deriveKey(EMAIL_HMAC_INFO, "HmacSHA256")
    private val emailEncryptionKey = deriveKey(EMAIL_ENCRYPTION_INFO, "AES")
    private val recoveryTokenHmacKey = deriveKey(RECOVERY_TOKEN_HMAC_INFO, "HmacSHA256")

    private val emailHasher = EmailHasher(emailHmacKey)
    private val emailCipher = AesGcm(emailEncryptionKey)
    private val recoveryTokenHasher = RecoveryTokenHasher(recoveryTokenHmacKey)

    /**
     * Hash the bytes used in a recovery token. This is meant to be used internally by a [RecoveryToken]
     */
    override fun hashToken(token: Token): HashedToken =
        RecoveryTokenHasher(recoveryTokenHmacKey)
            .hash(token.bytes.toByteArray())

    /**
     * Hashes a normalized (all lowercase) version of [email],
     * for use in database lookups. The result is a 32-byte HMAC-SHA256 digest.
     */
    override fun hashEmail(email: String): HashedEmail =
        EmailHasher(emailHmacKey)
            .hash(normalizeEmail(email))

    /**
     * Encrypts an email address for storage in the database. The result is an AEAD-encrypted payload.
     */
    override fun encryptEmail(text: String): EncryptedAesGcmText =
        AesGcm(emailEncryptionKey)
            .encryptText(text)

    /**
     * Decrypts an encrypted email address.
     */
    override fun decryptEmail(encrypted: EncryptedAesGcmText): String =
        AesGcm(emailEncryptionKey)
            .decryptText(encrypted)

    /**
     * Derives a 256-bit key from the master key using HKDF-SHA256.
     *
     * Each purpose must have a unique, stable info string.
     *
     * @param info Domain separation identifier for the derived key.
     * @param algorithm JCA algorithm name for the resulting key.
     */
    private fun deriveKey(info: String, algorithm: String): SecretKey {
        val spec = HKDFParameterSpec.ofExtract()
            .addIKM(masterKey.toByteArray())
            .thenExpand(info.toByteArray(Charsets.UTF_8), 32)

        return KDF.getInstance("HKDF-SHA256")
            .deriveKey(algorithm, spec)
    }

    private fun normalizeEmail(email: String): String {
        return email.trim().lowercase(Locale.ROOT)
    }

    companion object {
        private val masterKey: ByteString = getMasterKey()

        private const val EMAIL_HMAC_INFO = "ouraircraft/email-hmac/v1"
        private const val EMAIL_ENCRYPTION_INFO = "ouraircraft/email-encryption/v1"
        private const val RECOVERY_TOKEN_HMAC_INFO = "ouraircraft/recovery-token-hmac/v1"

        private fun getMasterKey(): ByteString{
            // LoadCredential=secret-key:/etc/ouraircraft/secret.key
            val credentialsDir = System.getenv("CREDENTIALS_DIRECTORY")
                ?: error("Not running with systemd credentials")

            val masterKey = Path.of(credentialsDir, "secret-key")
                .readBytes()
                .also { require(it.size == 32) { "Master key must be 32 bytes" } }

            return ByteString(masterKey)
        }
    }
}