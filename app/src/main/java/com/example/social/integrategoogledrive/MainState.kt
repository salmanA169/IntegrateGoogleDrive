package com.example.social.integrategoogledrive

import com.example.social.integrategoogledrive.backup.DriveBackupImage
import com.example.social.integrategoogledrive.backup.DriveFileInfo

data class MainState(
    val email:String? = null,
    val restoreFiles:List<DriveFileInfo> = emptyList()
)
