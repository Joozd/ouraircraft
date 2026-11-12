package org.ouraircraft.model.services.users

import org.ouraircraft.auth.RecoveryToken
import org.ouraircraft.auth.Token
import org.ouraircraft.model.auth.User
import org.ouraircraft.model.dto.EmailVerificationData
import org.ouraircraft.model.dto.NewUserRegistrationData
import org.ouraircraft.model.dto.ScreenNameAvailable
import java.net.URI
import kotlin.uuid.Uuid

/**
 * Provides operations for user registration and username availability.
 *
 * New users are initially created as pending registrations and must activate
 * their accounts using a verification token before registration is completed.
 *
 * All operations return [Result] to distinguish successful outcomes from
 * domain or infrastructure failures.
 */
interface UserManagementService {
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
    suspend fun isScreenNameAvailable(screenName: String): Result<ScreenNameAvailable>

    /**
     * Creates a pending user registration.
     *
     * The registration remains inactive until the user verifies their email
     * address using the generated activation token.
     *
     * The activation token is intended to be delivered by email. Returning
     * it to the frontend is acceptable while email verification is not set up yet.
     *
     * @param registrationData The information required to register a new user.
     * @return A [Result] containing a [URI] with the activation [Token] on success, or a
     * failure if registration could not be created, such as when the screen
     * name is already taken.
     */
    suspend fun createNewPendingUserRegistration(
        registrationData: NewUserRegistrationData
    ): Result<URI>


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
     *  or a failure if verification or account creation fails.
     */
    suspend fun promotePendingUserToUser(
        verificationData: EmailVerificationData
    ): Result<RecoveryToken>

    suspend fun getUserById(id: Uuid): Result<User>
}