package com.example.social.integrategoogledrive.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.UUID

class StorageManager(private val context: Context) {

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun saveImageToLocalStorage(imageByteArray: ByteArray){
        val btm = BitmapFactory.decodeByteArray(imageByteArray,0,imageByteArray.size)
        context.openFileOutput(UUID.randomUUID().toString().plus(".webp"),Context.MODE_PRIVATE).use {
            btm.compress(Bitmap.CompressFormat.WEBP_LOSSY,80,it)
        }
    }
}