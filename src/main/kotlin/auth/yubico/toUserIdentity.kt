package org.ouraircraft.auth.yubico

import com.yubico.webauthn.data.UserIdentity
import org.ouraircraft.model.auth.User

fun User.toUserIdentity(): UserIdentity= UserIdentity.builder()
    .name(screenName)
    .displayName(screenName)
    .id(id.toYubicoByteArray())
    .build()