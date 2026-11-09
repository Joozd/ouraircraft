package org.ouraircraft.util

import kotlinx.io.bytestring.ByteString

fun ByteArray.toByteString(): ByteString = ByteString(this)