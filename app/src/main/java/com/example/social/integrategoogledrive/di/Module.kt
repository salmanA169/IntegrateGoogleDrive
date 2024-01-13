package com.example.social.integrategoogledrive.di

import com.example.social.integrategoogledrive.auth.AuthRepository
import com.example.social.integrategoogledrive.auth.AuthRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped

@Module
@InstallIn(ActivityRetainedComponent::class)
abstract class Module {

    @Binds
    @ActivityRetainedScoped
     abstract fun bindAuthRepo(
        authRepository: AuthRepositoryImpl
    ):AuthRepository
}