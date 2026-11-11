package org.ouraircraft.db.repository.convertors

import org.ouraircraft.model.auth.User
import org.ouraircraft.model.dto.DtoUser

fun User.toDto() = DtoUser(
    id, screenName, createdAt
)