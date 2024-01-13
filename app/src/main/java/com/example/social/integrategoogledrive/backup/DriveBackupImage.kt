package com.example.social.integrategoogledrive.backup

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.graphics.decodeBitmap
import com.example.social.integrategoogledrive.storage.StorageManager
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.Drive.Files.Get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import kotlin.math.log10

class DriveBackupImage(private val context: Context):BackupRepository<Uri> {
     var drive: Drive? = null
    private val storageManager = StorageManager(context)
    @RequiresApi(Build.VERSION_CODES.R)
    override suspend fun backup(data: Uri) {
        val decodeImage = decodeImage(data,context)
        backupImage(drive!!,decodeImage,"testImage1${System.currentTimeMillis()}.png")
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override suspend fun restore(fileId: String) {
        val getFile = getDriveFileById(drive!!,fileId)
        storageManager.saveImageToLocalStorage(getFile)
    }

    override suspend fun getFiles(): List<DriveFileInfo> {
        return getAllBackupFiles(drive!!)
    }

}
suspend fun getAllBackupFiles(drive: Drive): List<DriveFileInfo> {
    val files = mutableListOf<DriveFileInfo>()
    return withContext(Dispatchers.IO) {
            val result =
                drive.files().list().setSpaces("drive").setFields("*")
                    .execute()
            files.addAll(result.files.map { DriveFileInfo(it.id, it.name, getFileSize(it.getSize()),it.thumbnailLink) })
        files
    }
}
suspend fun getDriveFileById(drive: Drive,fileId: String):ByteArray{
    val byteArray = ByteArrayOutputStream()
    drive.files().get(fileId).executeMediaAndDownloadTo(byteArray)
    return byteArray.toByteArray()
}
fun getFileSize(size: Long): String {
    if (size <= 0) return "0"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(
        size / Math.pow(
            1024.0,
            digitGroups.toDouble()
        )
    ) + " " + units[digitGroups]
}
@RequiresApi(Build.VERSION_CODES.R)
suspend fun decodeImage(uri: Uri, context: Context):File{
    return withContext(Dispatchers.IO){
        val contentResolver = context.contentResolver
        val file = File(context.cacheDir,"testImage.png")
        val fileOutput = FileOutputStream(file)
        val btm = ImageDecoder.createSource(contentResolver,uri).decodeBitmap { info, source ->
        }
        btm.compress(Bitmap.CompressFormat.WEBP_LOSSY,80,fileOutput)
        file
    }
}
data class DriveFileInfo(
    val id: String,
    val nameFile: String,
    val size:String,
    val thumbnailFileLink:String
)
suspend fun backupImage(
    drive: Drive,
    file: File,
    fileName:String
){
    withContext(Dispatchers.IO){
        val fileG = com.google.api.services.drive.model.File().apply {
            name = fileName
        }

        val mediaContent = FileContent("image/jpeg",file)
        drive.files().create(fileG,mediaContent).execute()
    }
}