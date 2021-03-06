package com.example.cryptguard.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import kotlinx.coroutines.InternalCoroutinesApi
import java.time.LocalDateTime

class DatabaseUtils {
    companion object {
        private var instance: DatabaseUtils? = null

        fun getInstance(): DatabaseUtils {
            if (instance == null) {
                instance = DatabaseUtils()
            }
            return instance!!
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @InternalCoroutinesApi
    suspend fun createDatabaseBackup(context: Context) {
        // creates values of a to be file
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(
                MediaStore.MediaColumns.DISPLAY_NAME,
                LocalDateTime.now().toString() + ".cryptguard.db"
            )
        }

        // creates the file and writes the data into it
        val uri = resolver?.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            val passwordDataRepository = PasswordDataDatabase.getRepository(context)
            resolver.openOutputStream(uri)?.writer().use {
                passwordDataRepository?.getAllEncryptedData()?.forEach { encryptedData ->
                    it?.write(encryptedData?.id.toString() + "," + encryptedData?.encryptedPasswordData?.replace("\n", ".") + "\n")
                }
            }
        }
    }
}