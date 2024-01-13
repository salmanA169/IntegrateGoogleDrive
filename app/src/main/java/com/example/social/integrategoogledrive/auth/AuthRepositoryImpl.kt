package com.example.social.integrategoogledrive.auth

import android.accounts.Account
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) :AuthRepository{

    private val scopes = listOf(Scope(DriveScopes.DRIVE_FILE))
    private val authorizationRequest = AuthorizationRequest.builder().setRequestedScopes(scopes).build()

    private val oneTap = Identity.getSignInClient(context)
    private val signInRequest = BeginSignInRequest.builder().setGoogleIdTokenRequestOptions(
        BeginSignInRequest.GoogleIdTokenRequestOptions.builder().setSupported(true)
            .setServerClientId(
                "521870746383-i9k9ke25vpa3co2tqbmkslm4i4lo2nla.apps.googleusercontent.com"
            ).setFilterByAuthorizedAccounts(false).build()
    ).setAutoSelectEnabled(true).build()

    override fun observeUserStatus(): Flow<UserInfoResult?> {
        return callbackFlow {
            val authListener = FirebaseAuth.AuthStateListener {
                val currentUser = it.currentUser
                if (currentUser != null ){
                    trySend(UserInfoResult(currentUser.email!!))
                }else{
                    trySend(null)
                }
            }
            firebaseAuth.addAuthStateListener(authListener)
            awaitClose {
                firebaseAuth.removeAuthStateListener(authListener)
            }
        }
    }


    private val authorize = Identity.getAuthorizationClient(context)

    private val firebaseAuth = Firebase.auth

    private val credential = GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE_FILE))

    override suspend fun signInGoogle(): IntentSender {
        return oneTap.beginSignIn(signInRequest).await().pendingIntent.intentSender
    }

    override suspend fun signOut() {
        oneTap.signOut().await()
        firebaseAuth.signOut()
    }

    override suspend fun isSignedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    override suspend fun getUserInfo(): UserInfoResult? {
        val currentUser = firebaseAuth.currentUser
        return if (currentUser!=null){
            UserInfoResult(currentUser.email!!)
        }else{
            null
        }
    }

    override suspend fun getGoogleDrive(): Drive? {
        val currentUser = firebaseAuth.currentUser
        return if (currentUser!=null){
            credential.selectedAccount = Account(currentUser.email,"google.com")
            Drive.Builder(
                NetHttpTransport(), GsonFactory.getDefaultInstance(),
                credential
            ).build()
        }else{
            null
        }
    }

    override suspend fun authorizeGoogleDrive(): AuthorizationResult {
        return authorize.authorize(authorizationRequest).await()
    }

    override suspend fun authorizeGoogleDriveResult(intent: Intent): AuthorizationResult {
        return authorize.getAuthorizationResultFromIntent(intent)
    }

    override suspend fun getSignInResult(intent: Intent): UserInfoResult {
        val credential = oneTap.getSignInCredentialFromIntent(intent)
        val googleIdToken = credential.googleIdToken
        val googleCredential = GoogleAuthProvider.getCredential(googleIdToken,null)
        val authResult = firebaseAuth.signInWithCredential(googleCredential).await()
        return UserInfoResult(authResult.user!!.email!!)
    }
}