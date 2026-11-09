package org.ouraircraft.repository.convertors

import org.ouraircraft.model.User
import org.ouraircraft.model.dto.DtoUser

fun User.toDto() = DtoUser(
    id, screenName, role, createdAt
)