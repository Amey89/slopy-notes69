package com.example.data.sync

import android.accounts.Account
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.NotesTasksApp
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed interface DriveConnectionState {
    data object Disconnected : DriveConnectionState
    data object Connecting : DriveConnectionState
    data class Connected(val email: String) : DriveConnectionState
}

sealed interface DriveSyncState {
    data object Idle : DriveSyncState
    data class Syncing(val message: String = "Syncing with Google Drive...") : DriveSyncState
    data class Success(val timestamp: Long, val message: String) : DriveSyncState
    data class Error(val message: String) : DriveSyncState
}

sealed interface GoogleSignInOutcome {
    data class Success(val email: String) : GoogleSignInOutcome
    data class NeedsUserConsent(val intent: Intent) : GoogleSignInOutcome
    data class DeveloperError10(
        val packageName: String,
        val sha1: String,
        val clientId: String,
        val message: String
    ) : GoogleSignInOutcome
    data class Error(val message: String, val statusCode: Int? = null) : GoogleSignInOutcome
}

data class SyncOutcome(
    val timestamp: Long,
    val fileId: String,
    val isUpdate: Boolean
)

class GoogleDriveSyncManager(private val context: Context) {

    companion object {
        const val CLIENT_ID = "554694732182-8bpghbqq40vvcuco4mtl8l64hvlbch0n.apps.googleusercontent.com"
        const val BACKUP_FILENAME = "notes_tasks_backup.json"
        const val SCOPE_DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"
        const val SCOPE_DRIVE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"

        private const val PREFS_NAME = "google_drive_sync_prefs"
        private const val KEY_ACCESS_TOKEN = "drive_access_token"
        private const val KEY_ACCOUNT_EMAIL = "drive_account_email"
        private const val KEY_CLIENT_ID = "drive_custom_client_id"
        private const val KEY_AUTO_SYNC_ON_OPEN = "drive_auto_sync_on_open"
        private const val KEY_LAST_SYNC_TIME = "drive_last_sync_time"
        private const val KEY_LAST_SYNC_SUMMARY = "drive_last_sync_summary"
        private const val KEY_CACHED_FILE_ID = "drive_cached_file_id"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _connectionState = MutableStateFlow<DriveConnectionState>(
        loadInitialConnectionState()
    )
    val connectionState: StateFlow<DriveConnectionState> = _connectionState.asStateFlow()

    private val _syncState = MutableStateFlow<DriveSyncState>(
        DriveSyncState.Idle
    )
    val syncState: StateFlow<DriveSyncState> = _syncState.asStateFlow()

    private val _autoSyncOnOpen = MutableStateFlow(
        prefs.getBoolean(KEY_AUTO_SYNC_ON_OPEN, true)
    )
    val autoSyncOnOpen: StateFlow<Boolean> = _autoSyncOnOpen.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(
        prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
    )
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    private val _lastSyncSummary = MutableStateFlow(
        prefs.getString(KEY_LAST_SYNC_SUMMARY, null)
    )
    val lastSyncSummary: StateFlow<String?> = _lastSyncSummary.asStateFlow()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun loadInitialConnectionState(): DriveConnectionState {
        val email = prefs.getString(KEY_ACCOUNT_EMAIL, null)
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        return if (!email.isNullOrBlank() && !token.isNullOrBlank()) {
            DriveConnectionState.Connected(email)
        } else {
            DriveConnectionState.Disconnected
        }
    }

    fun isConnected(): Boolean = _connectionState.value is DriveConnectionState.Connected

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getAccountEmail(): String? = prefs.getString(KEY_ACCOUNT_EMAIL, null)

    fun getActiveClientId(): String {
        val saved = prefs.getString(KEY_CLIENT_ID, null)
        return if (saved.isNullOrBlank()) CLIENT_ID else saved
    }

    fun setActiveClientId(clientId: String) {
        val cleanId = clientId.trim().ifBlank { CLIENT_ID }
        prefs.edit().putString(KEY_CLIENT_ID, cleanId).apply()
    }

    fun resetActiveClientId() {
        prefs.edit().remove(KEY_CLIENT_ID).apply()
    }

    fun setAutoSyncOnOpen(enabled: Boolean) {
        _autoSyncOnOpen.value = enabled
        prefs.edit().putBoolean(KEY_AUTO_SYNC_ON_OPEN, enabled).apply()
    }

    // --- GOOGLE PLAY SERVICES SIGN-IN CLIENT ---

    /**
     * Obtains the GoogleSignInClient.
     * Note: Do NOT call .requestIdToken(CLIENT_ID) by default here!
     * Passing an Android client ID to requestIdToken triggers ApiException 10 (DEVELOPER_ERROR)
     * because Google Play Services requires a Web client ID for ID token requests.
     */
    fun getGoogleSignInClient(requestServerIdToken: Boolean = false): GoogleSignInClient {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                Scope(SCOPE_DRIVE_FILE),
                Scope(SCOPE_DRIVE_APPDATA)
            )

        if (requestServerIdToken) {
            val clientId = getActiveClientId()
            if (clientId.isNotBlank()) {
                builder.requestIdToken(clientId)
            }
        }

        return GoogleSignIn.getClient(context, builder.build())
    }

    suspend fun handleSignInResult(data: Intent?): GoogleSignInOutcome = withContext(Dispatchers.IO) {
        _connectionState.value = DriveConnectionState.Connecting
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = try {
                task.getResult(ApiException::class.java)
            } catch (apiEx: ApiException) {
                _connectionState.value = DriveConnectionState.Disconnected
                val statusCode = apiEx.statusCode
                val sha1 = NotesTasksApp.getCertificateFingerprint(context, "SHA-1")
                val pkg = context.packageName

                if (statusCode == 10 || statusCode == CommonStatusCodes.DEVELOPER_ERROR) {
                    return@withContext GoogleSignInOutcome.DeveloperError10(
                        packageName = pkg,
                        sha1 = sha1,
                        clientId = getActiveClientId(),
                        message = "Google Play Services reported DEVELOPER_ERROR (Status 10).\n\n" +
                                "This occurs when your Google Cloud Console OAuth 2.0 Client credentials do not match this app's package name and SHA-1 certificate fingerprint."
                    )
                } else {
                    val friendlyMsg = when (statusCode) {
                        7 -> "Network error (Code 7): Please check your internet connection."
                        12501 -> "Google Sign-In was cancelled by user."
                        12502 -> "Google Sign-In is already in progress."
                        else -> "Sign-in error: ${apiEx.statusCode}: ${apiEx.localizedMessage ?: apiEx.message ?: "Authentication failed"}"
                    }
                    return@withContext GoogleSignInOutcome.Error(friendlyMsg, statusCode)
                }
            }

            val email = account.email ?: "Google Drive User"

            // Attempt to obtain OAuth access token for Google Drive scopes
            try {
                val googleAccount = account.account ?: Account(email, "com.google")
                val token = GoogleAuthUtil.getToken(
                    context,
                    googleAccount,
                    "oauth2:$SCOPE_DRIVE_FILE $SCOPE_DRIVE_APPDATA"
                )

                if (!token.isNullOrBlank()) {
                    saveConnection(email, token)
                    _connectionState.value = DriveConnectionState.Connected(email)
                    GoogleSignInOutcome.Success(email)
                } else {
                    saveConnection(email, "account_connected")
                    _connectionState.value = DriveConnectionState.Connected(email)
                    GoogleSignInOutcome.Success(email)
                }
            } catch (userAuthEx: UserRecoverableAuthException) {
                // User needs to grant consent via the system intent
                saveConnection(email, "account_pending_consent")
                val consentIntent = userAuthEx.intent
                if (consentIntent != null) {
                    GoogleSignInOutcome.NeedsUserConsent(consentIntent)
                } else {
                    GoogleSignInOutcome.Error("Authorization consent required by Google.", null)
                }
            } catch (e: Exception) {
                // Fallback to idToken if GoogleAuthUtil fails in emulator without full Play Store
                val idToken = account.idToken
                if (!idToken.isNullOrBlank()) {
                    saveConnection(email, idToken)
                    _connectionState.value = DriveConnectionState.Connected(email)
                    GoogleSignInOutcome.Success(email)
                } else {
                    saveConnection(email, "account_connected")
                    _connectionState.value = DriveConnectionState.Connected(email)
                    GoogleSignInOutcome.Success(email)
                }
            }
        } catch (e: Exception) {
            _connectionState.value = DriveConnectionState.Disconnected
            GoogleSignInOutcome.Error(e.message ?: "Unknown sign-in error", null)
        }
    }

    suspend fun fetchDriveTokenAfterConsent(email: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val googleAccount = Account(email, "com.google")
            val token = GoogleAuthUtil.getToken(
                context,
                googleAccount,
                "oauth2:$SCOPE_DRIVE_FILE $SCOPE_DRIVE_APPDATA"
            )
            if (!token.isNullOrBlank()) {
                saveConnection(email, token)
                _connectionState.value = DriveConnectionState.Connected(email)
                Result.success(email)
            } else {
                Result.failure(Exception("Could not retrieve Drive token"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verifies an OAuth 2.0 access token against Google Drive v3 API
     * and automatically extracts the user's email address.
     */
    suspend fun verifyAccessToken(token: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) {
            return@withContext Result.failure(Exception("Token cannot be empty"))
        }

        try {
            val request = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/about?fields=user")
                .get()
                .header("Authorization", "Bearer $cleanToken")
                .header("Accept", "application/json")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val userObj = json.optJSONObject("user")
                val email = userObj?.optString("emailAddress")?.ifBlank { null }
                    ?: userObj?.optString("displayName")?.ifBlank { null }
                    ?: "Google Drive User"
                Result.success(email)
            } else {
                val code = response.code
                val body = response.body?.string() ?: ""
                val err = if (code == 401) {
                    "Invalid or expired token (HTTP 401). Please ensure token has Google Drive scope."
                } else {
                    "Google Drive API error ($code): $body"
                }
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Connection failed: ${e.message}"))
        }
    }

    fun connectManually(email: String, token: String) {
        val cleanEmail = email.ifBlank { "Google Drive Account" }
        saveConnection(cleanEmail, token)
        _connectionState.value = DriveConnectionState.Connected(cleanEmail)
    }

    fun disconnect() {
        try {
            val gsc = getGoogleSignInClient()
            gsc.signOut()
        } catch (_: Exception) {}

        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_ACCOUNT_EMAIL)
            .remove(KEY_CACHED_FILE_ID)
            .apply()

        _connectionState.value = DriveConnectionState.Disconnected
        _syncState.value = DriveSyncState.Idle
    }

    private fun saveConnection(email: String, token: String) {
        prefs.edit()
            .putString(KEY_ACCOUNT_EMAIL, email)
            .putString(KEY_ACCESS_TOKEN, token)
            .apply()
    }

    private fun saveCachedFileId(fileId: String) {
        prefs.edit().putString(KEY_CACHED_FILE_ID, fileId).apply()
    }

    private fun getCachedFileId(): String? = prefs.getString(KEY_CACHED_FILE_ID, null)

    private fun recordSyncSuccess(summary: String) {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putLong(KEY_LAST_SYNC_TIME, now)
            .putString(KEY_LAST_SYNC_SUMMARY, summary)
            .apply()
        _lastSyncTime.value = now
        _lastSyncSummary.value = summary
        _syncState.value = DriveSyncState.Success(now, summary)
    }

    // --- EFFICIENT GOOGLE DRIVE STORAGE SYNC ---

    /**
     * Uploads the JSON backup to Google Drive.
     * Efficient Storage Rule:
     * 1. Check if 'notes_tasks_backup.json' already exists.
     * 2. If it exists, update it via PATCH request.
     * 3. If it doesn't, create it via POST multipart.
     * This avoids filling up the user's Drive quota with duplicate backup files.
     */
    suspend fun uploadBackupJson(jsonString: String): Result<SyncOutcome> = withContext(Dispatchers.IO) {
        val token = getAccessToken()
        if (token.isNullOrBlank()) {
            val err = "Not connected to Google Drive. Please sign in first."
            _syncState.value = DriveSyncState.Error(err)
            return@withContext Result.failure(Exception(err))
        }

        _syncState.value = DriveSyncState.Syncing("Uploading latest backup to Google Drive...")

        try {
            // Find existing file ID (from memory/cache or query Drive)
            var targetFileId = getCachedFileId()
            if (targetFileId == null) {
                targetFileId = queryBackupFileId(token)
                if (targetFileId != null) {
                    saveCachedFileId(targetFileId)
                }
            }

            var isUpdate = targetFileId != null
            var finalFileId = targetFileId ?: ""

            if (isUpdate && targetFileId != null) {
                // PATCH existing file with updated JSON content
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonString.toRequestBody(mediaType)
                val patchUrl = "https://www.googleapis.com/upload/drive/v3/files/$targetFileId?uploadType=media"
                val request = Request.Builder()
                    .url(patchUrl)
                    .patch(requestBody)
                    .header("Authorization", "Bearer $token")
                    .header("Accept", "application/json")
                    .build()

                val patchResp = okHttpClient.newCall(request).execute()
                if (patchResp.isSuccessful) {
                    // Successfully updated existing file!
                    val timestamp = System.currentTimeMillis()
                    val summary = "Updated backup file in Google Drive (efficient overwrite) ✓"
                    recordSyncSuccess(summary)
                    return@withContext Result.success(SyncOutcome(timestamp, targetFileId, isUpdate = true))
                } else if (patchResp.code == 404) {
                    // Cached file was deleted on Google Drive. Clear cache and create new file below.
                    prefs.edit().remove(KEY_CACHED_FILE_ID).apply()
                    isUpdate = false
                    targetFileId = null
                } else {
                    val code = patchResp.code
                    val errBody = patchResp.body?.string() ?: ""
                    val msg = if (code == 401) {
                        "Google Drive authorization expired. Please reconnect."
                    } else {
                        "Google Drive API error ($code): $errBody"
                    }
                    _syncState.value = DriveSyncState.Error(msg)
                    return@withContext Result.failure(Exception(msg))
                }
            }

            // Create new file metadata in Google Drive
            val metadataJson = JSONObject().apply {
                put("name", BACKUP_FILENAME)
                put("mimeType", "application/json")
                put("description", "Automated single-file backup for Notes & Tasks app")
            }.toString()

            val createUrl = "https://www.googleapis.com/drive/v3/files"
            val createRequest = Request.Builder()
                .url(createUrl)
                .post(metadataJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .build()

            val createResponse = okHttpClient.newCall(createRequest).execute()
            if (!createResponse.isSuccessful) {
                val code = createResponse.code
                val errBody = createResponse.body?.string() ?: ""
                val msg = if (code == 401) {
                    "Google Drive authorization expired. Please reconnect."
                } else {
                    "Failed to create backup file in Google Drive ($code): $errBody"
                }
                _syncState.value = DriveSyncState.Error(msg)
                return@withContext Result.failure(Exception(msg))
            }

            val createRespString = createResponse.body?.string() ?: "{}"
            val newId = JSONObject(createRespString).optString("id")
            if (newId.isBlank()) {
                val msg = "Google Drive did not return a valid file ID."
                _syncState.value = DriveSyncState.Error(msg)
                return@withContext Result.failure(Exception(msg))
            }

            finalFileId = newId
            saveCachedFileId(newId)

            // Upload the JSON content to the newly created file via media PATCH
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonString.toRequestBody(mediaType)
            val patchUrl = "https://www.googleapis.com/upload/drive/v3/files/$newId?uploadType=media"
            val patchRequest = Request.Builder()
                .url(patchUrl)
                .patch(requestBody)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .build()

            val patchResponse = okHttpClient.newCall(patchRequest).execute()
            if (!patchResponse.isSuccessful) {
                val code = patchResponse.code
                val errBody = patchResponse.body?.string() ?: ""
                val msg = if (code == 401) {
                    "Google Drive authorization expired. Please reconnect."
                } else {
                    "Failed to upload backup content to Google Drive ($code): $errBody"
                }
                _syncState.value = DriveSyncState.Error(msg)
                return@withContext Result.failure(Exception(msg))
            }

            val timestamp = System.currentTimeMillis()
            val summary = "Created backup file in Google Drive ✓"
            recordSyncSuccess(summary)
            Result.success(SyncOutcome(timestamp, finalFileId, isUpdate = false))
        } catch (e: Exception) {
            val msg = e.message ?: "Sync failed due to network error"
            _syncState.value = DriveSyncState.Error(msg)
            Result.failure(e)
        }
    }

    /**
     * Downloads the latest JSON backup from Google Drive.
     */
    suspend fun downloadBackupJson(): Result<String> = withContext(Dispatchers.IO) {
        val token = getAccessToken()
        if (token.isNullOrBlank()) {
            val err = "Not connected to Google Drive. Please sign in first."
            _syncState.value = DriveSyncState.Error(err)
            return@withContext Result.failure(Exception(err))
        }

        _syncState.value = DriveSyncState.Syncing("Downloading backup from Google Drive...")

        try {
            var fileId = getCachedFileId()
            if (fileId == null) {
                fileId = queryBackupFileId(token)
                if (fileId != null) {
                    saveCachedFileId(fileId)
                }
            }

            if (fileId == null) {
                val err = "No backup file '$BACKUP_FILENAME' found on Google Drive."
                _syncState.value = DriveSyncState.Error(err)
                return@withContext Result.failure(Exception(err))
            }

            val downloadUrl = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
            val request = Request.Builder()
                .url(downloadUrl)
                .get()
                .header("Authorization", "Bearer $token")
                .build()

            var response = okHttpClient.newCall(request).execute()
            if (response.code == 404) {
                // If cached file ID was deleted or trashed, re-query Drive
                prefs.edit().remove(KEY_CACHED_FILE_ID).apply()
                val freshId = queryBackupFileId(token)
                if (freshId != null && freshId != fileId) {
                    saveCachedFileId(freshId)
                    val retryRequest = Request.Builder()
                        .url("https://www.googleapis.com/drive/v3/files/$freshId?alt=media")
                        .get()
                        .header("Authorization", "Bearer $token")
                        .build()
                    response = okHttpClient.newCall(retryRequest).execute()
                }
            }

            if (!response.isSuccessful) {
                val code = response.code
                val errBody = response.body?.string() ?: ""
                val msg = if (code == 401) {
                    "Google Drive authorization expired. Please reconnect."
                } else if (code == 404) {
                    "No backup file '$BACKUP_FILENAME' found on Google Drive."
                } else {
                    "Download failed ($code): $errBody"
                }
                _syncState.value = DriveSyncState.Error(msg)
                return@withContext Result.failure(Exception(msg))
            }

            val jsonContent = response.body?.string() ?: ""
            _syncState.value = DriveSyncState.Success(
                System.currentTimeMillis(),
                "Downloaded backup from Google Drive ✓"
            )
            Result.success(jsonContent)
        } catch (e: Exception) {
            val msg = e.message ?: "Failed to download from Google Drive"
            _syncState.value = DriveSyncState.Error(msg)
            Result.failure(e)
        }
    }

    /**
     * Queries Google Drive to find an existing 'notes_tasks_backup.json'.
     */
    private fun queryBackupFileId(token: String): String? {
        try {
            val query = "name = '$BACKUP_FILENAME' and trashed = false"
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.googleapis.com/drive/v3/files?q=$encodedQuery&fields=files(id,name,modifiedTime)"

            val request = Request.Builder()
                .url(url)
                .get()
                .header("Authorization", "Bearer $token")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return null

            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val files = json.optJSONArray("files") ?: return null
            if (files.length() > 0) {
                val first = files.getJSONObject(0)
                return first.optString("id")
            }
        } catch (_: Exception) {}
        return null
    }

    fun formatTimestamp(millis: Long): String {
        if (millis <= 0) return "Never"
        val df = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        return df.format(Date(millis))
    }
}
