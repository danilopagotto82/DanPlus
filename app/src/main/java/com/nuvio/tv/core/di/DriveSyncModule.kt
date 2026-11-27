package com.nuvio.tv.core.di

import com.nuvio.tv.core.backup.DriveGrantVault
import com.nuvio.tv.core.profile.ProfileScopedCredentialStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object DriveSyncModule {
    @Provides
    @IntoSet
    fun credentialStore(vault: DriveGrantVault): ProfileScopedCredentialStore = vault
}
