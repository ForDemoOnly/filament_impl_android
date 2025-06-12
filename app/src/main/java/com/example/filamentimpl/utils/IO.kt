package com.example.filamentimpl.utils

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun readIntLE(input: InputStream): Int {
    return input.read() and 0xff or (
            input.read() and 0xff shl 8) or (
            input.read() and 0xff shl 16) or (
            input.read() and 0xff shl 24)
}

fun readFloat32LE(input: InputStream): Float {
    val bytes = ByteArray(4)
    input.read(bytes, 0, 4)
    return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).float
}

fun readUIntLE(input: InputStream): Long {
    return readIntLE(input).toLong() and 0xFFFFFFFFL
}