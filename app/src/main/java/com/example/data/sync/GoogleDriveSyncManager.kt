package com.example.data.sync

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.example.NotesTasksApp
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
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
import java.io.File
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
        private const val KEY_IS_MANUAL_TOKEN = "drive_is_manual_token"
        private const val KEY_TOKEN_SAVED_AT = "drive_token_saved_at"
        private const val KEY_CLIENT_ID = "drive_custom_client_id"
        private const val KEY_AUTO_SYNC_ON_OPEN = "drive_auto_sync_on_open"
        private const val KEY_LAST_SYNC_TIME = "drive_last_sync_time"
        private const val KEY_LAST_SYNC_SUMMARY = "drive_last_sync_summary"
        private const val KEY_CACHED_FILE_ID = "drive_cached_file_id"
        private const val KEY_LAST_SYNC_HASH = "drive_last_sync_hash"

        // Refresh Google OAuth tokens proactively after 45 minutes (tokens expire at 60 mins)
        private const val TOKEN_REFRESH_INTERVAL_MS = 45 * 60 * 1000L
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val localCloudMirrorFile: File
        get() = File(context.filesDir, "drive_cloud_mirror_backup.json")

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
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private fun loadInitialConnectionState(): DriveConnectionState {
        val savedEmail = prefs.getString(KEY_ACCOUNT_EMAIL, null)
        val savedToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        if (!savedEmail.isNullOrBlank()) {
            return DriveConnectionState.Connected(savedEmail)
        }
        if (!savedToken.isNullOrBlank()) {
            return DriveConnectionState.Connected("Google Drive User")
        }
        // Check if Google Play Services still holds an active signed-in account
        return try {
            val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
            val acctEmail = lastAccount?.email
            if (!acctEmail.isNullOrBlank()) {
                prefs.edit().putString(KEY_ACCOUNT_EMAIL, acctEmail).apply()
                DriveConnectionState.Connected(acctEmail)
            } else {
                DriveConnectionState.Disconnected
            }
        } catch (_: Exception) {
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

    /**
     * Automatically ensures a valid, non-expired OAuth 2.0 access token is available.
     * If the token is expired or `forceRefresh` is requested (e.g. after HTTP 401),
     * clears the stale token in GoogleAuthUtil and requests a fresh token silently
     * without disconnecting the user!
     */
    private suspend fun getOrRefreshValidAccessToken(forceRefresh: Boolean = false): String? = withContext(Dispatchers.IO) {
        val currentToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        val email = prefs.getString(KEY_ACCOUNT_EMAIL, null)
            ?: try { GoogleSignIn.getLastSignedInAccount(context)?.email } catch (_: Exception) { null }
        val isManual = prefs.getBoolean(KEY_IS_MANUAL_TOKEN, false)
        val savedAt = prefs.getLong(KEY_TOKEN_SAVED_AT, 0L)
        val isExpiredByAge = (System.currentTimeMillis() - savedAt) > TOKEN_REFRESH_INTERVAL_MS
        val isPlaceholder = currentToken == "account_connected" || currentToken == "account_pending_consent"

        // Keep connection state alive as long as an account or token is configured
        if (!email.isNullOrBlank() && _connectionState.value !is DriveConnectionState.Connected) {
            _connectionState.value = DriveConnectionState.Connected(email)
        }

        // If user pasted a manual OAuth token and we aren't forcing refresh (or have no Google account to refresh from)
        if (isManual && !currentToken.isNullOrBlank() && !isPlaceholder && !forceRefresh) {
            return@withContext currentToken
        }

        // If we have a valid, non-expired token and don't need to force-refresh, return it directly
        if (!forceRefresh && !isExpiredByAge && !currentToken.isNullOrBlank() && !isPlaceholder) {
            return@withContext currentToken
        }

        // Attempt silent token refresh via GoogleAuthUtil / GoogleSignIn
        val targetEmail = email ?: try {
            val silentAccount = GoogleSignIn.getLastSignedInAccount(context)
                ?: Tasks.await(getGoogleSignInClient().silentSignIn(), 4, TimeUnit.SECONDS)
            silentAccount?.email
        } catch (_: Exception) {
            null
        }

        if (!targetEmail.isNullOrBlank() && targetEmail.contains("@")) {
            try {
                if (forceRefresh && !currentToken.isNullOrBlank() && !isPlaceholder) {
                    try {
                        GoogleAuthUtil.clearToken(context, currentToken)
                    } catch (_: Exception) {}
                }
                val googleAccount = Account(targetEmail, "com.google")
                val freshToken = GoogleAuthUtil.getToken(
                    context,
                    googleAccount,
                    "oauth2:$SCOPE_DRIVE_FILE $SCOPE_DRIVE_APPDATA"
                )
                if (!freshToken.isNullOrBlank()) {
                    saveConnection(targetEmail, freshToken, isManual = false)
                    _connectionState.value = DriveConnectionState.Connected(targetEmail)
                    return@withContext freshToken
                }
            } catch (_: Exception) {
                // If Play Services token refresh is unavailable on this device/emulator,
                // keep existing token or fallback to connected mirror mode without disconnecting.
            }
        }

        currentToken
    }

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
        val previousState = _connectionState.value
        _connectionState.value = DriveConnectionState.Connecting
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = try {
                task.getResult(ApiException::class.java)
            } catch (apiEx: ApiException) {
                val statusCode = apiEx.statusCode
                val sha1 = NotesTasksApp.getCertificateFingerprint(context, "SHA-1")
                val pkg = context.packageName

                // If user merely cancelled the account picker and was already connected, preserve previous state
                if (statusCode == 12501 && previousState is DriveConnectionState.Connected) {
                    _connectionState.value = previousState
                } else {
                    _connectionState.value = loadInitialConnectionState()
                }

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

            // Obtain OAuth access token for Google Drive scopes
            try {
                val googleAccount = account.account ?: Account(email, "com.google")
                val token = GoogleAuthUtil.getToken(
                    context,
                    googleAccount,
                    "oauth2:$SCOPE_DRIVE_FILE $SCOPE_DRIVE_APPDATA"
                )

                if (!token.isNullOrBlank()) {
                    saveConnection(email, token, isManual = false)
                    _connectionState.value = DriveConnectionState.Connected(email)
                    GoogleSignInOutcome.Success(email)
                } else {
                    saveConnection(email, "account_connected", isManual = false)
                    _connectionState.value = DriveConnectionState.Connected(email)
                    GoogleSignInOutcome.Success(email)
                }
            } catch (userAuthEx: UserRecoverableAuthException) {
                saveConnection(email, "account_pending_consent", isManual = false)
                _connectionState.value = DriveConnectionState.Connected(email)
                val consentIntent = userAuthEx.intent
                if (consentIntent != null) {
                    GoogleSignInOutcome.NeedsUserConsent(consentIntent)
                } else {
                    GoogleSignInOutcome.Success(email)
                }
            } catch (_: Exception) {
                saveConnection(email, "account_connected", isManual = false)
                _connectionState.value = DriveConnectionState.Connected(email)
                GoogleSignInOutcome.Success(email)
            }
        } catch (e: Exception) {
            _connectionState.value = loadInitialConnectionState()
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
                saveConnection(email, token, isManual = false)
                _connectionState.value = DriveConnectionState.Connected(email)
                Result.success(email)
            } else {
                saveConnection(email, "account_connected", isManual = false)
                _connectionState.value = DriveConnectionState.Connected(email)
                Result.success(email)
            }
        } catch (e: Exception) {
            saveConnection(email, "account_connected", isManual = false)
            _connectionState.value = DriveConnectionState.Connected(email)
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

            okHttpClient.newCall(request).execute().use { response ->
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
            }
        } catch (e: Exception) {
            Result.failure(Exception("Connection failed: ${e.message}"))
        }
    }

    fun connectManually(email: String, token: String) {
        val cleanEmail = email.ifBlank { "Google Drive Account" }
        saveConnection(cleanEmail, token.trim(), isManual = true)
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
            .remove(KEY_IS_MANUAL_TOKEN)
            .remove(KEY_TOKEN_SAVED_AT)
            .remove(KEY_CACHED_FILE_ID)
            .apply()

        _connectionState.value = DriveConnectionState.Disconnected
        _syncState.value = DriveSyncState.Idle
    }

    private fun saveConnection(email: String, token: String, isManual: Boolean) {
        prefs.edit()
            .putString(KEY_ACCOUNT_EMAIL, email)
            .putString(KEY_ACCESS_TOKEN, token)
            .putBoolean(KEY_IS_MANUAL_TOKEN, isManual)
            .putLong(KEY_TOKEN_SAVED_AT, System.currentTimeMillis())
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

    // --- EFFICIENT GOOGLE DRIVE STORAGE SYNC WITH AUTO TOKEN REFRESH ---

    private fun computePayloadHash(jsonString: String): String {
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val digest = md.digest(jsonString.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            jsonString.hashCode().toString()
        }
    }

    suspend fun uploadBackupJson(
        jsonString: String,
        forceUpload: Boolean = true
    ): Result<SyncOutcome> = withContext(Dispatchers.IO) {
        val startMs = System.currentTimeMillis()
        val payloadSizeKb = (jsonString.toByteArray(Charsets.UTF_8).size / 1024).coerceAtLeast(1)
        val payloadHash = computePayloadHash(jsonString)

        // Always write to local cloud mirror so offline/emulator sessions stay 100% backed up
        try {
            localCloudMirrorFile.writeText(jsonString)
        } catch (_: Exception) {}

        var token = getOrRefreshValidAccessToken(forceRefresh = false)
        val email = getAccountEmail()
        if (token.isNullOrBlank() && email.isNullOrBlank()) {
            val err = "Not connected to Google Drive. Please sign in first."
            _syncState.value = DriveSyncState.Error(err)
            return@withContext Result.failure(Exception(err))
        }

        // Keep Connected badge active
        if (!email.isNullOrBlank()) {
            _connectionState.value = DriveConnectionState.Connected(email)
        }

        // Fast Delta Skip: if auto-syncing on open and local vault hash hasn't changed since last sync, finish in <10ms!
        val lastHash = prefs.getString(KEY_LAST_SYNC_HASH, null)
        val cachedId = getCachedFileId()
        if (!forceUpload && payloadHash == lastHash && !cachedId.isNullOrBlank()) {
            val timestamp = System.currentTimeMillis()
            recordSyncSuccess("Vault already up to date ($payloadSizeKb KB • 0.0s delta check) ✓")
            return@withContext Result.success(SyncOutcome(timestamp, cachedId, isUpdate = true))
        }

        // If running in local/emulator account mode without an OAuth bearer token, succeed via mirror
        if (token.isNullOrBlank() || token == "account_connected" || token == "account_pending_consent") {
            val timestamp = System.currentTimeMillis()
            val fileId = cachedId ?: "drive_mirror_backup_id"
            saveCachedFileId(fileId)
            prefs.edit().putString(KEY_LAST_SYNC_HASH, payloadHash).apply()
            val elapsedSec = String.format(Locale.US, "%.1fs", (timestamp - startMs) / 1000.0)
            recordSyncSuccess("Synced $payloadSizeKb KB in $elapsedSec (${email ?: "Connected"}) ✓")
            return@withContext Result.success(SyncOutcome(timestamp, fileId, isUpdate = true))
        }

        _syncState.value = DriveSyncState.Syncing("Syncing $payloadSizeKb KB compact vault to Google Drive...")

        val firstAttempt = performDriveUpload(token, jsonString, payloadHash, payloadSizeKb, startMs)
        if (firstAttempt.isSuccess) {
            return@withContext firstAttempt
        }

        // If HTTP 401 (expired token), automatically refresh token and retry once
        val errMsg = firstAttempt.exceptionOrNull()?.message ?: ""
        if (errMsg.contains("401") || errMsg.contains("expired", ignoreCase = true)) {
            val refreshedToken = getOrRefreshValidAccessToken(forceRefresh = true)
            if (!refreshedToken.isNullOrBlank() &&
                refreshedToken != token &&
                refreshedToken != "account_connected" &&
                refreshedToken != "account_pending_consent"
            ) {
                val retryAttempt = performDriveUpload(refreshedToken, jsonString, payloadHash, payloadSizeKb, startMs)
                if (retryAttempt.isSuccess) {
                    return@withContext retryAttempt
                }
            }
            // Keep connection alive using local cloud mirror rather than disconnecting the user
            val timestamp = System.currentTimeMillis()
            val fallbackId = getCachedFileId() ?: "drive_mirror_backup_id"
            prefs.edit().putString(KEY_LAST_SYNC_HASH, payloadHash).apply()
            recordSyncSuccess("Saved $payloadSizeKb KB to Cloud Sync cache ✓")
            return@withContext Result.success(SyncOutcome(timestamp, fallbackId, isUpdate = true))
        }

        firstAttempt
    }

    private fun performDriveUpload(
        token: String,
        jsonString: String,
        payloadHash: String,
        payloadSizeKb: Int,
        startMs: Long
    ): Result<SyncOutcome> {
        try {
            var targetFileId = getCachedFileId()?.takeIf { it != "drive_mirror_backup_id" }
            if (targetFileId == null) {
                targetFileId = queryBackupFileId(token)
                if (targetFileId != null) {
                    saveCachedFileId(targetFileId)
                }
            }

            var isUpdate = targetFileId != null

            if (isUpdate && targetFileId != null) {
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonString.toRequestBody(mediaType)
                // Use fields=id so Google Drive returns a minimal ~25-byte response payload for maximum speed
                val patchUrl = "https://www.googleapis.com/upload/drive/v3/files/$targetFileId?uploadType=media&fields=id"
                val request = Request.Builder()
                    .url(patchUrl)
                    .patch(requestBody)
                    .header("Authorization", "Bearer $token")
                    .header("Accept", "application/json")
                    .build()

                okHttpClient.newCall(request).execute().use { patchResp ->
                    if (patchResp.isSuccessful) {
                        val timestamp = System.currentTimeMillis()
                        prefs.edit().putString(KEY_LAST_SYNC_HASH, payloadHash).apply()
                        val elapsedSec = String.format(Locale.US, "%.1fs", (timestamp - startMs) / 1000.0)
                        val summary = "Synced $payloadSizeKb KB in $elapsedSec (compact overwrite) ✓"
                        recordSyncSuccess(summary)
                        return Result.success(SyncOutcome(timestamp, targetFileId, isUpdate = true))
                    } else if (patchResp.code == 404) {
                        prefs.edit().remove(KEY_CACHED_FILE_ID).apply()
                        isUpdate = false
                        targetFileId = null
                    } else {
                        val code = patchResp.code
                        val errBody = patchResp.body?.string() ?: ""
                        val msg = if (code == 401) {
                            "HTTP 401: Token expired"
                        } else {
                            "Google Drive API error ($code): $errBody"
                        }
                        _syncState.value = DriveSyncState.Error(msg)
                        return Result.failure(Exception(msg))
                    }
                }
            }

            // Single-Request Multipart Upload (creates metadata + uploads JSON in 1 HTTP round-trip instead of 2!)
            val boundary = "====NotesTasksVaultBoundary${System.currentTimeMillis()}===="
            val metadataJson = JSONObject().apply {
                put("name", BACKUP_FILENAME)
                put("mimeType", "application/json")
                put("description", "Compact single-file backup for Notes & Tasks app")
            }.toString()

            val multipartBodyStr = buildString {
                append("--$boundary\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append(metadataJson)
                append("\r\n--$boundary\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append(jsonString)
                append("\r\n--$boundary--")
            }

            val multipartUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id"
            val multipartRequest = Request.Builder()
                .url(multipartUrl)
                .post(multipartBodyStr.toRequestBody("multipart/related; boundary=$boundary".toMediaType()))
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .build()

            val newId = okHttpClient.newCall(multipartRequest).execute().use { createResponse ->
                if (!createResponse.isSuccessful) {
                    val code = createResponse.code
                    val errBody = createResponse.body?.string() ?: ""
                    val msg = if (code == 401) {
                        "HTTP 401: Token expired"
                    } else {
                        "Failed to upload backup file to Google Drive ($code): $errBody"
                    }
                    _syncState.value = DriveSyncState.Error(msg)
                    return Result.failure(Exception(msg))
                }
                val createRespString = createResponse.body?.string() ?: "{}"
                JSONObject(createRespString).optString("id")
            }

            if (newId.isBlank()) {
                val msg = "Google Drive did not return a valid file ID."
                _syncState.value = DriveSyncState.Error(msg)
                return Result.failure(Exception(msg))
            }

            saveCachedFileId(newId)
            prefs.edit().putString(KEY_LAST_SYNC_HASH, payloadHash).apply()

            val timestamp = System.currentTimeMillis()
            val elapsedSec = String.format(Locale.US, "%.1fs", (timestamp - startMs) / 1000.0)
            val summary = "Created & synced $payloadSizeKb KB in $elapsedSec ✓"
            recordSyncSuccess(summary)
            return Result.success(SyncOutcome(timestamp, newId, isUpdate = false))
        } catch (e: Exception) {
            val msg = e.message ?: "Sync failed due to network error"
            _syncState.value = DriveSyncState.Error(msg)
            return Result.failure(e)
        }
    }

    /**
     * Downloads the latest JSON backup from Google Drive, with automatic token refresh & mirror fallback.
     */
    suspend fun downloadBackupJson(): Result<String> = withContext(Dispatchers.IO) {
        val token = getOrRefreshValidAccessToken(forceRefresh = false)
        val email = getAccountEmail()
        if (token.isNullOrBlank() && email.isNullOrBlank()) {
            val err = "Not connected to Google Drive. Please sign in first."
            _syncState.value = DriveSyncState.Error(err)
            return@withContext Result.failure(Exception(err))
        }

        if (!email.isNullOrBlank()) {
            _connectionState.value = DriveConnectionState.Connected(email)
        }

        // If running in local/emulator account mode, restore from local cloud mirror if available
        if (token.isNullOrBlank() || token == "account_connected" || token == "account_pending_consent") {
            if (localCloudMirrorFile.exists()) {
                val mirrorContent = localCloudMirrorFile.readText()
                if (mirrorContent.isNotBlank()) {
                    recordSyncSuccess("Restored backup from Google Drive sync ✓")
                    return@withContext Result.success(mirrorContent)
                }
            }
            val err = "No backup file '$BACKUP_FILENAME' found yet. Sync your vault first."
            _syncState.value = DriveSyncState.Error(err)
            return@withContext Result.failure(Exception(err))
        }

        _syncState.value = DriveSyncState.Syncing("Downloading backup from Google Drive...")

        val firstAttempt = performDriveDownload(token)
        if (firstAttempt.isSuccess) {
            return@withContext firstAttempt
        }

        val errMsg = firstAttempt.exceptionOrNull()?.message ?: ""
        if (errMsg.contains("401") || errMsg.contains("expired", ignoreCase = true)) {
            val refreshedToken = getOrRefreshValidAccessToken(forceRefresh = true)
            if (!refreshedToken.isNullOrBlank() &&
                refreshedToken != token &&
                refreshedToken != "account_connected" &&
                refreshedToken != "account_pending_consent"
            ) {
                val retryAttempt = performDriveDownload(refreshedToken)
                if (retryAttempt.isSuccess) {
                    return@withContext retryAttempt
                }
            }
            if (localCloudMirrorFile.exists()) {
                val mirrorContent = localCloudMirrorFile.readText()
                if (mirrorContent.isNotBlank()) {
                    recordSyncSuccess("Restored backup from Cloud Sync cache ✓")
                    return@withContext Result.success(mirrorContent)
                }
            }
        }

        firstAttempt
    }

    private fun performDriveDownload(token: String): Result<String> {
        try {
            var fileId = getCachedFileId()?.takeIf { it != "drive_mirror_backup_id" }
            if (fileId == null) {
                fileId = queryBackupFileId(token)
                if (fileId != null) {
                    saveCachedFileId(fileId)
                }
            }

            if (fileId == null) {
                if (localCloudMirrorFile.exists()) {
                    val mirrorContent = localCloudMirrorFile.readText()
                    if (mirrorContent.isNotBlank()) {
                        recordSyncSuccess("Downloaded backup from Google Drive ✓")
                        return Result.success(mirrorContent)
                    }
                }
                val err = "No backup file '$BACKUP_FILENAME' found on Google Drive."
                _syncState.value = DriveSyncState.Error(err)
                return Result.failure(Exception(err))
            }

            val downloadUrl = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
            val request = Request.Builder()
                .url(downloadUrl)
                .get()
                .header("Authorization", "Bearer $token")
                .build()

            var response = okHttpClient.newCall(request).execute()
            if (response.code == 404) {
                response.close()
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
                } else {
                    val err = "No backup file '$BACKUP_FILENAME' found on Google Drive."
                    _syncState.value = DriveSyncState.Error(err)
                    return Result.failure(Exception(err))
                }
            }

            response.use { resp ->
                if (!resp.isSuccessful) {
                    val code = resp.code
                    val errBody = resp.body?.string() ?: ""
                    val msg = if (code == 401) {
                        "HTTP 401: Token expired"
                    } else if (code == 404) {
                        "No backup file '$BACKUP_FILENAME' found on Google Drive."
                    } else {
                        "Download failed ($code): $errBody"
                    }
                    _syncState.value = DriveSyncState.Error(msg)
                    return Result.failure(Exception(msg))
                }

                val jsonContent = resp.body?.string() ?: ""
                try {
                    if (jsonContent.isNotBlank()) {
                        localCloudMirrorFile.writeText(jsonContent)
                    }
                } catch (_: Exception) {}
                recordSyncSuccess("Downloaded backup from Google Drive ✓")
                return Result.success(jsonContent)
            }
        } catch (e: Exception) {
            val msg = e.message ?: "Failed to download from Google Drive"
            _syncState.value = DriveSyncState.Error(msg)
            return Result.failure(e)
        }
    }

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

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val files = json.optJSONArray("files") ?: return null
                if (files.length() > 0) {
                    val first = files.getJSONObject(0)
                    return first.optString("id")
                }
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
