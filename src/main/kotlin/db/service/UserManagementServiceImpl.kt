package org.ouraircraft.db.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ouraircraft.auth.EmailVerificationToken
import org.ouraircraft.auth.RecoveryToken
import org.ouraircraft.auth.exceptions.TokenMismatchException
import org.ouraircraft.db.exceptions.UserNotFoundByIDException
import org.ouraircraft.model.services.utils.runCatchingAndMapToDomainException
import org.ouraircraft.model.auth.PendingUserCreationResult
import org.ouraircraft.model.auth.User
import org.ouraircraft.model.dto.EmailVerificationData
import org.ouraircraft.model.dto.NewUserRegistrationData
import org.ouraircraft.model.dto.ScreenNameAvailable
import org.ouraircraft.db.repository.UserRepository
import org.ouraircraft.model.services.users.UserManagementService
import org.ouraircraft.model.services.factories.LinkFactory
import java.net.URI
import kotlin.uuid.Uuid

internal class UserManagementServiceImpl(
    private val userRepository: UserRepository,
    private val linkFactory: LinkFactory
): UserManagementService {
    /**
     * Checks whether a screen name is available for registration.
     *
     * This check is informational only and does not reserve the screen name.
     * Availability must be verified again during registration, with uniqueness
     * ultimately enforced by the database.
     *
     * @param screenName The screen name to check.
     * @return A [Result] containing the availability status, or a failure
     * if the check could not be completed.
     */
    override suspend fun isScreenNameAvailable(screenName: String): Result<ScreenNameAvailable> = runCatchingAndMapToDomainException {
        val isAvailable = withContext(Dispatchers.IO){ userRepository.isScreenNameAvailable(screenName) }
        ScreenNameAvailable(screenName, isAvailable)
    }

    override suspend fun createNewPendingUserRegistration(registrationData: NewUserRegistrationData): Result<URI> = runCatchingAndMapToDomainException {
        /*
        TODO check if email available, if not, send an email to the email address wout the result of the check,
         but with a message that the email is already registered and they can use account recovery if needed,
         or ignore this if they did not request this. This is to prevent enumeration of registered emails.
         Also log this in server logs for monitoring and security purposes.
         */
        val emailVerificationToken = EmailVerificationToken.generate()
        val uuid = withContext(Dispatchers.IO) {
            userRepository.createPendingUserRegistration(registrationData.screenName, registrationData.emailAddress, emailVerificationToken)
        }
        //result:
        linkFactory.createEmailVerificationLink(PendingUserCreationResult(uuid, emailVerificationToken))
        //TODO send email with verification token
    }

    /**
     * Verifies the email confirmation token and promotes a pending registration to a user.
     *
     * On success, creates the user account, removes the pending registration,
     * and generates a recovery token that can be used to register a passkey,
     * using the same flow as account recovery.
     *
     * @param verificationData The data used to verify ownership of the email address.
     *
     * @return A [Result] containing the generated [RecoveryToken] on success,
     * or a failure if verification or account creation fails.
     *
     * @throws TokenMismatchException if the token in [verificationData] does not match the stored token hash for the user with the expected Uuid.
     *  This includes any instance where user with that Uuid does not exist.
     */
    override suspend fun promotePendingUserToUser(
        verificationData: EmailVerificationData
    ): Result<RecoveryToken> = runCatchingAndMapToDomainException {
        val token = EmailVerificationToken.fromBase64UrlSafe(verificationData.emailVerificationTokenBase64)
        val promotedUser = userRepository.elevatePendingToUser(verificationData.id, token)
        //this does not need to be in the same transaction; user can recover using "create new passkey"

        // insert a recovery token, and use that as the result of this function
        userRepository.createRecoveryToken(promotedUser)
    }.onFailure {
        //catch Domain `UserNotFoundByIDException`, rethrow that as RuntimeException as it means ID was mangled during the process. (server error)
        if (it is UserNotFoundByIDException)
            throw RuntimeException("UserNotFoundByIDException during promotePendingUserToUser. This is a server error.", it)
    }

    /**
     * Gets a [User] from its [Uuid]
     */
    override suspend fun getUserById(id: Uuid): Result<User> = runCatchingAndMapToDomainException {
        userRepository.getUserById(id)
    }
}