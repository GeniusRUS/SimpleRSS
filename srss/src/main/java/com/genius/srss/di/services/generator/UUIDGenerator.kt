package com.genius.srss.di.services.generator

import java.util.*
import javax.inject.Inject

class UUIDGenerator @Inject constructor() : IGenerator {

    override fun generateRandomId(): String {
        return UUID.randomUUID().toString()
    }
}