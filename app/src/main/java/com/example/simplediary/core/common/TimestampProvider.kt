package com.example.simplediary.core.common

interface TimestampProvider {
    fun nowEpochMillis(): Long
}

object SystemTimestampProvider : TimestampProvider {
    override fun nowEpochMillis(): Long = System.currentTimeMillis()
}
