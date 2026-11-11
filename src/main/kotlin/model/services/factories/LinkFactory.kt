package org.ouraircraft.model.services.factories

import org.ouraircraft.model.auth.PendingUserCreationResult
import java.net.URI
import java.net.URLEncoder
import kotlin.text.Charsets.UTF_8

private const val EMAIL_VERIFICATION_RECEIVER = "verify-email"

private const val ID = "id"
private const val TOKEN = "token"

/**
 * Constructs account-related URLs using the configured frontend base URL.
 * @param hostName The base url (including https://) where the links will lead to. Do not add a trailing '/'.
 */
class LinkFactory(private val hostName: String = "https://ouraircraft.org") {
    /**
     * Creates the email verification URL for a pending user registration.
     *
     * @return A URI containing the registration ID and email verification token.
     */
    fun createEmailVerificationLink(pendingUserCreationResult: PendingUserCreationResult): URI{
        val idParam = Param(ID, pendingUserCreationResult.id.toHexDashString())
        val tokenParam = Param(TOKEN, pendingUserCreationResult.emailVerificationToken.toBase64UrlSafe())

        return makeUri(EMAIL_VERIFICATION_RECEIVER, idParam, tokenParam)
    }


    // Fail hard at startup, not when verification mails don't work

    private val scheme = if (hostName in listOf("127.0.0.1", "localhost")) "http://" else "https://"
    val baseUri = URI("$scheme$hostName/").also { uri ->
        require(
            !uri.host.isNullOrBlank() &&
                    (uri.scheme == "https" || (uri.scheme == "http" && (uri.host == "localhost" || uri.host == "127.0.0.1")))
        ) {
            "baseUrl must use HTTPS, except for http://localhost or 127.0.0.1"
        }
        require(uri.rawQuery == null && uri.rawFragment == null) {
            "baseUrl must not contain a query or fragment"
        }
        require(!hostName.endsWith("/")) {
            "Please read the documentation and do not end the hostname with a slash thank you very much"
        }
    }



    private fun makeUri(receiver: String, vararg parameters: Param)= URI(buildString {
        append(baseUri.toString())
        append(receiver)
        if (parameters.isNotEmpty()) append("?")
        append(parameters.joinToString("&"))
    })

    private data class Param(val key: String, val value: String){
        override fun toString() = "${URLEncoder.encode(key, UTF_8)}=${URLEncoder.encode(value, UTF_8)}"
    }
}