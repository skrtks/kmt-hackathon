package com.samex.kmt_hackathon

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform