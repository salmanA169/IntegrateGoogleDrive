package com.example.social.integrategoogledrive

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.social.integrategoogledrive.auth.AuthRepository
import com.example.social.integrategoogledrive.backup.DriveBackupImage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
):ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state = _state.asStateFlow()

    private val _effect = MutableStateFlow<MainEffect?>(null)
    val effect = _effect.asStateFlow()

    private val driveBackup = DriveBackupImage(context)
    init {
        viewModelScope.launch(Dispatchers.IO) {
            authRepository.observeUserStatus().collect{user->
                if (user!=null){
                    driveBackup.drive = authRepository.getGoogleDrive()
                }
                _state.update {
                    it.copy(
                        user?.email
                    )
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun onEvent(mainEvent: MainEvent){
        when(mainEvent){
            is MainEvent.Backup -> {
                viewModelScope.launch(Dispatchers.IO) {
                    driveBackup.backup(mainEvent.imageUri)
                }
            }
            is MainEvent.OnAuthorize -> {
                viewModelScope.launch(Dispatchers.IO) {
                    authRepository.authorizeGoogleDriveResult(mainEvent.intent)
                }
            }
            is MainEvent.OnSignInResult -> {
                viewModelScope.launch (Dispatchers.IO){
                    onSignInResult(mainEvent.intent)
                }
            }
            MainEvent.GetFiles -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val getFiles = driveBackup.getFiles()
                    _state.update {
                        it.copy(
                            restoreFiles = getFiles
                        )
                    }
                }
            }

            MainEvent.SignInGoogle -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val getGoogleSignIn = authRepository.signInGoogle()
                    _effect.update {
                        MainEffect.SignIn(getGoogleSignIn)
                    }
                }
            }
            MainEvent.SignOut -> {
                viewModelScope.launch(Dispatchers.IO) {
                    authRepository.signOut()

                }
            }

            is MainEvent.Restore -> {
                viewModelScope.launch (Dispatchers.IO){
                    driveBackup.restore(mainEvent.fileId)
                }
            }
        }
    }

    private suspend fun onSignInResult(intent:Intent){
        val getResult = authRepository.getSignInResult(intent)
        _state.update {
            it.copy(
                email = getResult.email
            )
        }
        val authorizeGoogleDrive = authRepository.authorizeGoogleDrive()
        if (authorizeGoogleDrive.hasResolution()){
            _effect.update {
                MainEffect.Authorize(authorizeGoogleDrive.pendingIntent!!.intentSender)
            }
        }
    }

}

sealed class MainEffect{
    data class SignIn(val intentSender: IntentSender):MainEffect()
    data class Authorize(val intentSender: IntentSender):MainEffect()
}
sealed class MainEvent{
    data object SignInGoogle:MainEvent()
    data object SignOut:MainEvent()

    data class Backup(val imageUri:Uri):MainEvent()

    data class OnSignInResult(val intent:Intent):MainEvent()
    data class OnAuthorize(val intent:Intent):MainEvent()
    data class  Restore(val fileId:String):MainEvent()

    data object GetFiles:MainEvent()
}