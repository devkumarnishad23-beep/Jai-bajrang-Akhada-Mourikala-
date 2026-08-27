package com.example.data.cloud

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Cloud Authentication Manager handles Firebase Auth, role resolution, and credentials.
 * Designed with safe fallback mechanisms for offline use or local development without cloud config.
 */
class CloudAuthManager(
    private val auth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull(),
    private val firestore: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull()
) {
    private val TAG = "CloudAuthManager"

    private val _authState = MutableStateFlow<CloudAuthState>(CloudAuthState.Unauthenticated)
    val authState: StateFlow<CloudAuthState> = _authState.asStateFlow()

    init {
        checkCurrentAuthState()
    }

    /**
     * Checks if a Firebase user is already logged in on device.
     */
    fun checkCurrentAuthState() {
        val currentUser = auth?.currentUser
        if (currentUser != null) {
            _authState.value = CloudAuthState.Authenticated(
                uid = currentUser.uid,
                email = currentUser.email,
                displayName = currentUser.displayName ?: currentUser.email?.substringBefore("@"),
                role = FirestoreConstants.ROLE_STUDENT,
                studentId = null,
                isEmailVerified = currentUser.isEmailVerified
            )
            // Asynchronously resolve role and linked student ID from Firestore
            resolveUserCloudProfile(currentUser.uid)
        } else {
            _authState.value = CloudAuthState.Unauthenticated
        }
    }

    /**
     * Sign in with Email and Password.
     */
    suspend fun signInWithEmail(email: String, pinOrPass: String): Result<CloudAuthState.Authenticated> = withContext(Dispatchers.IO) {
        val authInstance = auth ?: return@withContext Result.failure(
            IllegalStateException("Firebase Auth उपलब्ध नहीं है (Firebase Auth is not initialized)")
        )
        try {
            _authState.value = CloudAuthState.Authenticating("लॉगिन किया जा रहा है...")
            val result = authInstance.signInWithEmailAndPassword(email.trim(), pinOrPass).await()
            val user = result.user ?: throw IllegalStateException("User authentication returned empty")
            
            val profile = fetchOrCreateUserProfile(user)
            val authResult = CloudAuthState.Authenticated(
                uid = user.uid,
                email = user.email,
                displayName = user.displayName ?: profile.displayName.ifEmpty { user.email?.substringBefore("@") },
                role = profile.role,
                studentId = profile.linkedStudentId.ifEmpty { null },
                isEmailVerified = user.isEmailVerified
            )
            _authState.value = authResult
            Result.success(authResult)
        } catch (e: Exception) {
            Log.e(TAG, "signInWithEmail error: ${e.message}", e)
            val errorMsg = formatAuthErrorMessage(e)
            _authState.value = CloudAuthState.AuthError(errorMsg)
            Result.failure(Exception(errorMsg, e))
        }
    }

    /**
     * Register a new Student or Admin account in Firebase Auth & Firestore.
     */
    suspend fun registerUser(
        email: String,
        password: String,
        fullName: String,
        role: String = FirestoreConstants.ROLE_STUDENT,
        studentId: String = ""
    ): Result<CloudAuthState.Authenticated> = withContext(Dispatchers.IO) {
        val authInstance = auth ?: return@withContext Result.failure(
            IllegalStateException("Firebase Auth उपलब्ध नहीं है (Firebase Auth is not initialized)")
        )
        try {
            _authState.value = CloudAuthState.Authenticating("नया खाता बनाया जा रहा है...")
            val result = authInstance.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = result.user ?: throw IllegalStateException("User registration returned empty")

            val newProfile = CloudUserProfile(
                uid = user.uid,
                email = email.trim(),
                displayName = fullName.trim(),
                role = role,
                linkedStudentId = studentId.trim(),
                createdAt = System.currentTimeMillis(),
                lastLoginAt = System.currentTimeMillis()
            )

            // Save user profile to Firestore
            firestore?.collection(FirestoreConstants.COLLECTION_USERS)
                ?.document(user.uid)
                ?.set(newProfile.toMap())
                ?.await()

            val authResult = CloudAuthState.Authenticated(
                uid = user.uid,
                email = user.email,
                displayName = fullName.trim(),
                role = role,
                studentId = studentId.ifEmpty { null },
                isEmailVerified = user.isEmailVerified
            )
            _authState.value = authResult
            Result.success(authResult)
        } catch (e: Exception) {
            Log.e(TAG, "registerUser error: ${e.message}", e)
            val errorMsg = formatAuthErrorMessage(e)
            _authState.value = CloudAuthState.AuthError(errorMsg)
            Result.failure(Exception(errorMsg, e))
        }
    }

    /**
     * Fetch user profile from Firestore or create initial fallback profile.
     */
    private suspend fun fetchOrCreateUserProfile(user: FirebaseUser): CloudUserProfile {
        val db = firestore ?: return CloudUserProfile(uid = user.uid, email = user.email ?: "")
        return try {
            val doc = db.collection(FirestoreConstants.COLLECTION_USERS).document(user.uid).get().await()
            if (doc.exists() && doc.data != null) {
                CloudUserProfile.fromMap(doc.data!!)
            } else {
                val initial = CloudUserProfile(
                    uid = user.uid,
                    email = user.email ?: "",
                    displayName = user.displayName ?: user.email?.substringBefore("@") ?: "",
                    role = FirestoreConstants.ROLE_STUDENT
                )
                db.collection(FirestoreConstants.COLLECTION_USERS).document(user.uid).set(initial.toMap()).await()
                initial
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch user profile, using fallback", e)
            CloudUserProfile(uid = user.uid, email = user.email ?: "")
        }
    }

    /**
     * Resolve user profile in background and update StateFlow.
     */
    private fun resolveUserCloudProfile(uid: String) {
        val db = firestore ?: return
        db.collection(FirestoreConstants.COLLECTION_USERS).document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists() && doc.data != null) {
                    val profile = CloudUserProfile.fromMap(doc.data!!)
                    val current = _authState.value
                    if (current is CloudAuthState.Authenticated) {
                        _authState.value = current.copy(
                            role = profile.role,
                            studentId = profile.linkedStudentId.ifEmpty { current.studentId },
                            displayName = profile.displayName.ifEmpty { current.displayName }
                        )
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "resolveUserCloudProfile warning: ${e.message}")
            }
    }

    /**
     * Sign out current user.
     */
    fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "signOut error", e)
        }
        _authState.value = CloudAuthState.Unauthenticated
    }

    fun isFirebaseConfigured(): Boolean {
        return auth != null && firestore != null
    }

    fun getCurrentUser(): FirebaseUser? = auth?.currentUser

    private fun formatAuthErrorMessage(e: Exception): String {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("user-not-found", true) || msg.contains("invalid-credential", true) ->
                "गलत ईमेल या पासवर्ड। कृपया पुनः प्रयास करें।"
            msg.contains("wrong-password", true) ->
                "पासवर्ड गलत है।"
            msg.contains("email-already-in-use", true) ->
                "यह ईमेल पहले से पंजीकृत है।"
            msg.contains("network", true) ->
                "नेटवर्क त्रुटि: कृपया इंटरनेट कनेक्शन जांचें।"
            else -> "प्रमाणीकरण त्रुटि: ${e.localizedMessage ?: "अज्ञात त्रुटि"}"
        }
    }
}
