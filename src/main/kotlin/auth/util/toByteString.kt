package org.ouraircraft.auth.util

import kotlinx.io.bytestring.ByteString

fun ByteArray.toByteString(): ByteString = ByteString(this)