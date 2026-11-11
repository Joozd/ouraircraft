package org.ouraircraft.auth.exceptions

/**
 * This is thrown when user and login data got mixed up. Points to a serious server problem. Should not happen.
 */
class UserCredentialsMismatchException(reason: String): AuthException(reason) {
}