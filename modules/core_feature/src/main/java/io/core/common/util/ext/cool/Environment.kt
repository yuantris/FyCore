package io.core.common.util.ext.cool

import android.content.Context
import android.os.Environment
import java.io.File

val externalDcim: File
    get() = getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)

val externalDocuments: File
    get() = getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)

val externalDownloads: File
    get() = getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

val externalPictures: File
    get() = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)

val externalMusic: File
    get() = getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)

fun getExternalStoragePublicDirectory(dirType: String): File {
    return Environment.getExternalStoragePublicDirectory(dirType)
}

/*----/storage/emulated/0/Android/data/com.core.fy.android/files----*/
val Context.externalFiles: File
    get() = this.getExternalFilesDir(null) ?: this.filesDir

/*----/storage/emulated/0/Android/data/com.core.fy.android/cache----*/
val Context.externalCache: File
    get() = this.externalCacheDir ?: this.cacheDir
