package com.wellpaid.security

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppSecurityEntryPoint {
    fun appSecurityManager(): AppSecurityManager
}
