package com.farukg.movievault.data.test

import com.farukg.movievault.core.time.Clock

class TestClock(private var nowMs: Long = 1_700_000_000_000L) : Clock {
    override fun now(): Long = nowMs

    fun setNow(ms: Long) {
        nowMs = ms
    }

    fun advance(ms: Long) {
        nowMs += ms
    }
}
