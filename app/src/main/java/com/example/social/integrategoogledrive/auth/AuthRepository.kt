package com.example.social.integrategoogledrive.auth

import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.api.services.drive.Drive
import java.util.concurrent.Flow

interface AuthRepository {
    suspend fun signInGoogle():IntentSender
    suspend fun signOut()
    suspend fun isSignedIn():Boolean
    suspend fun getGoogleDrive():Drive?
    suspend fun authorizeGoogleDrive():AuthorizationResult

    fun observeUserStatus():kotlinx.coroutines.flow.Flow<UserInfoResult?>
    suspend fun authorizeGoogleDriveResult(intent: Intent):AuthorizationResult
    suspend fun getSignInResult(intent:Intent):UserInfoResult

    suspend fun getUserInfo():UserInfoResult?

}


data class UserInfoResult(
    val email:String
)