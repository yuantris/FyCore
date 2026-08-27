package io.core.common.util.tools

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import androidx.core.net.toUri
import io.core.appCtx
import java.io.File
import java.lang.reflect.Array

@Suppress("unused")
object RealPathUtil {

    fun getPath(uri: Uri): String? {
        Log.d("UriUtils", uri.toString())
        val authority = uri.authority
        val scheme = uri.scheme
        val path = uri.path
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && path != null) {
            val externals = arrayOf("/external/", "/external_path/")
            var file: File? = null
            for (external in externals) {
                if (path.startsWith(external)) {
                    file = File(
                        Environment.getExternalStorageDirectory().absolutePath
                                + path.replace(external, "/")
                    )
                    if (file.exists()) {
                        Log.d("UriUtils", "$uri -> $external")
                        return file.absolutePath
                    }
                }
            }
            file = null
            if (path.startsWith("/files_path/")) {
                file = File(
                    appCtx.filesDir.absolutePath
                            + path.replace("/files_path/", "/")
                )
            } else if (path.startsWith("/cache_path/")) {
                file = File(
                    appCtx.cacheDir.absolutePath
                            + path.replace("/cache_path/", "/")
                )
            } else if (path.startsWith("/external_files_path/")) {
                file = File(
                    appCtx.getExternalFilesDir(null)?.absolutePath
                            + path.replace("/external_files_path/", "/")
                )
            } else if (path.startsWith("/external_cache_path/")) {
                file = File(
                    appCtx.externalCacheDir?.absolutePath
                            + path.replace("/external_cache_path/", "/")
                )
            }
            if (file != null && file.exists()) {
                Log.d("UriUtils", "$uri -> $path")
                return file.absolutePath
            }
        }
        if (ContentResolver.SCHEME_FILE == scheme) {
            if (path != null) return path
            Log.d("UriUtils", "$uri parse failed. -> 0")
            return null
        } // end 0
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
            && DocumentsContract.isDocumentUri(appCtx, uri)
        ) {
            if ("com.android.externalstorage.documents" == authority) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]
                if ("primary".equals(type, ignoreCase = true)) {
                    return File(
                        Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                    ).absolutePath
                } else {
                    // Below logic is how External Storage provider build URI for documents
                    // http://stackoverflow.com/questions/28605278/android-5-sd-card-label
                    val mStorageManager = appCtx.getSystemService(Context.STORAGE_SERVICE)
                    try {
                        val storageVolumeClazz = Class.forName("android.os.storage.StorageVolume")
                        val getVolumeList = mStorageManager.javaClass.getMethod("getVolumeList")
                        val getUuid = storageVolumeClazz.getMethod("getUuid")
                        val getState = storageVolumeClazz.getMethod("getState")
                        val getPath = storageVolumeClazz.getMethod("getPath")
                        val isPrimary = storageVolumeClazz.getMethod("isPrimary")
                        val isEmulated = storageVolumeClazz.getMethod("isEmulated")

                        val result = getVolumeList.invoke(mStorageManager)

                        val length = Array.getLength(result)
                        for (i in 0 until length) {
                            val storageVolumeElement = Array.get(result, i)

                            //String uuid = (String) getUuid.invoke(storageVolumeElement);
                            val mounted =
                                Environment.MEDIA_MOUNTED == getState.invoke(storageVolumeElement)
                                        || Environment.MEDIA_MOUNTED_READ_ONLY == getState.invoke(
                                    storageVolumeElement
                                )

                            //if the media is not mounted, we need not get the volume details
                            if (!mounted) continue

                            //Primary storage is already handled.
                            if (isPrimary.invoke(storageVolumeElement) as Boolean
                                && isEmulated.invoke(storageVolumeElement) as Boolean
                            ) {
                                continue
                            }

                            val uuid = getUuid.invoke(storageVolumeElement) as String

                            if (uuid != null && uuid == type) {
                                return File(
                                    getPath.invoke(storageVolumeElement).toString() + "/" + split[1]
                                ).absolutePath
                            }
                        }
                    } catch (ex: Exception) {
                        Log.d("UriUtils", "$uri parse failed. $ex -> 1_0")
                    }
                }
                Log.d("UriUtils", "$uri parse failed. -> 1_0")
                return null
            } // end 1_0
            else if ("com.android.providers.downloads.documents" == authority) {
                var id = DocumentsContract.getDocumentId(uri)
                if (TextUtils.isEmpty(id)) {
                    Log.d("UriUtils", "$uri parse failed(id is null). -> 1_1")
                    return null
                }
                if (id.startsWith("raw:")) {
                    return File(id.substring(4)).absolutePath
                } else if (id.startsWith("msf:")) {
                    id = id.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                }

                var availableId: Long = 0
                try {
                    availableId = id.toLong()
                } catch (e: Exception) {
                    return null
                }

                val contentUriPrefixesToTry = arrayOf(
                    "content://downloads/public_downloads",
                    "content://downloads/all_downloads",
                    "content://downloads/my_downloads"
                )

                for (contentUriPrefix in contentUriPrefixesToTry) {
                    val contentUri =
                        ContentUris.withAppendedId(contentUriPrefix.toUri(), availableId)
                    try {
                        val file: File? = getFileFromUri(contentUri, "1_1")
                        if (file != null) {
                            return file.absolutePath
                        }
                    } catch (ignore: Exception) {
                    }
                }

                Log.d("UriUtils", "$uri parse failed. -> 1_1")
                return null
            } // end 1_1
            else if ("com.android.providers.media.documents" == authority) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]
                val contentUri = if ("image" == type) {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                } else {
                    Log.d("UriUtils", "$uri parse failed. -> 1_2")
                    return null
                }
                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])
                return getFileFromUri(contentUri, "1_2", selection, selectionArgs)?.absolutePath
            } // end 1_2
            else if (ContentResolver.SCHEME_CONTENT == scheme) {
                return getFileFromUri(uri, "1_3")?.absolutePath
            } // end 1_3
            else {
                Log.d("UriUtils", "$uri parse failed. -> 1_4")
                return null
            } // end 1_4
        } // end 1
        else if (ContentResolver.SCHEME_CONTENT == scheme) {
            return getFileFromUri(uri, "2")?.absolutePath
        } // end 2
        else {
            Log.d("UriUtils", "$uri parse failed. -> 3")
            return null
        } // end 3
    }

    private fun getFileFromUri(
        uri: Uri,
        code: String,
        selection: String? = null,
        selectionArgs: kotlin.Array<String>? = null
    ): File? {
        if ("com.google.android.apps.photos.content" == uri.authority) {
            if (!TextUtils.isEmpty(uri.lastPathSegment)) {
                return File(uri.lastPathSegment)
            }
        } else if ("com.tencent.mtt.fileprovider" == uri.authority) {
            val path = uri.path
            if (!TextUtils.isEmpty(path)) {
                val fileDir = Environment.getExternalStorageDirectory()
                return File(fileDir, path!!.substring("/QQBrowser".length, path.length))
            }
        } else if ("com.huawei.hidisk.fileprovider" == uri.authority) {
            val path = uri.path
            if (!TextUtils.isEmpty(path)) {
                return File(path!!.replace("/root", ""))
            }
        }

        val cursor: Cursor? = appCtx.contentResolver.query(
            uri, arrayOf<String>("_data"), selection, selectionArgs, null
        )
        if (cursor == null) {
            Log.d("UriUtils", "$uri parse failed(cursor is null). -> $code")
            return null
        }
        try {
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex("_data")
                if (columnIndex > -1) {
                    return File(cursor.getString(columnIndex))
                } else {
                    Log.d(
                        "UriUtils",
                        "$uri parse failed(columnIndex: $columnIndex is wrong). -> $code"
                    )
                    return null
                }
            } else {
                Log.d("UriUtils", "$uri parse failed(moveToFirst return false). -> $code")
                return null
            }
        } catch (e: java.lang.Exception) {
            Log.d("UriUtils", "$uri parse failed. -> $code")
            return null
        } finally {
            cursor.close()
        }
    }
}