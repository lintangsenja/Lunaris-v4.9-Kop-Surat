package com.example.data.network

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage

/**
 * Centralized Manager for Firebase initialization and database connections.
 * Provides safe getters and error handled operations for Firestore, Auth, and Storage.
 */
object FirebaseManager {

    private const val TAG = "FirebaseManager"

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var isFirebaseInitialized = false

    /**
     * Initializes FirebaseApp and configures Firestore settings safely.
     * Should be called in Application.onCreate() or Activity.onCreate().
     */
    fun initialize(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }

        val currentContext = appContext ?: context.applicationContext

        if (isFirebaseInitialized && FirebaseApp.getApps(currentContext).isNotEmpty()) return

        synchronized(this) {
            if (isFirebaseInitialized && FirebaseApp.getApps(currentContext).isNotEmpty()) return

            try {
                if (FirebaseApp.getApps(currentContext).isEmpty()) {
                    try {
                        val app = FirebaseApp.initializeApp(currentContext)
                        if (app != null) {
                            Log.i(TAG, "FirebaseApp initialized successfully via auto-config for ${app.name}")
                        } else {
                            Log.w(TAG, "FirebaseApp initialization returned null")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Auto FirebaseApp initialization failed, trying fallback options: ${e.message}")
                    }

                    if (FirebaseApp.getApps(currentContext).isEmpty()) {
                        try {
                            val fallbackOptions = FirebaseOptions.Builder()
                                .setApplicationId("1:48370294902:android:63d01244318c4fd8c3bf57")
                                .setApiKey("AIzaSyBrAN62B3KDrpGQjIi-kVZ2TQWYpcsSt_w")
                                .setGcmSenderId("48370294902")
                                .setProjectId("lunaris-app-c6400")
                                .setStorageBucket("lunaris-app-c6400.firebasestorage.app")
                                .build()
                            FirebaseApp.initializeApp(currentContext, fallbackOptions)
                            Log.i(TAG, "FirebaseApp initialized successfully with fallback FirebaseOptions for lunaris-app-c6400")
                        } catch (fallbackException: Exception) {
                            Log.e(TAG, "Fallback FirebaseApp initialization failed: ${fallbackException.message}")
                        }
                    }
                } else {
                    Log.i(TAG, "FirebaseApp already initialized")
                }

                if (FirebaseApp.getApps(currentContext).isNotEmpty()) {
                    isFirebaseInitialized = true
                    // Configure Firestore persistence and settings safely
                    try {
                        val firestore = FirebaseFirestore.getInstance()
                        val settings = FirebaseFirestoreSettings.Builder()
                            .setPersistenceEnabled(true)
                            .build()
                        firestore.firestoreSettings = settings
                        Log.i(TAG, "Firestore offline persistence configured successfully")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to configure Firestore settings: ${e.message}")
                    }
                } else {
                    isFirebaseInitialized = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing Firebase: ${e.message}", e)
                isFirebaseInitialized = false
            }
        }
    }

    /**
     * Returns the FirebaseFirestore instance safely, or null if initialization fails.
     */
    fun getFirestore(): FirebaseFirestore? {
        val ctx = appContext
        if (!isFirebaseInitialized || (ctx != null && FirebaseApp.getApps(ctx).isEmpty())) {
            ctx?.let { initialize(it) }
        }
        return try {
            val checkCtx = appContext
            if (checkCtx != null && FirebaseApp.getApps(checkCtx).isNotEmpty()) {
                FirebaseFirestore.getInstance()
            } else if (FirebaseApp.getApps(checkCtx ?: return null).isNotEmpty()) {
                FirebaseFirestore.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to obtain FirebaseFirestore instance: ${e.message}")
            null
        }
    }

    /**
     * Returns the FirebaseAuth instance safely, or null if initialization fails.
     */
    fun getAuth(): FirebaseAuth? {
        val ctx = appContext
        if (!isFirebaseInitialized || (ctx != null && FirebaseApp.getApps(ctx).isEmpty())) {
            ctx?.let { initialize(it) }
        }
        return try {
            val checkCtx = appContext
            if (checkCtx != null && FirebaseApp.getApps(checkCtx).isNotEmpty()) {
                FirebaseAuth.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to obtain FirebaseAuth instance: ${e.message}")
            null
        }
    }

    /**
     * Returns the FirebaseStorage instance safely, or null if initialization fails.
     */
    fun getStorage(): FirebaseStorage? {
        val ctx = appContext
        if (!isFirebaseInitialized || (ctx != null && FirebaseApp.getApps(ctx).isEmpty())) {
            ctx?.let { initialize(it) }
        }
        return try {
            val checkCtx = appContext
            if (checkCtx != null && FirebaseApp.getApps(checkCtx).isNotEmpty()) {
                FirebaseStorage.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to obtain FirebaseStorage instance: ${e.message}")
            null
        }
    }

    /**
     * Writes or merges document data safely in Firestore.
     */
    fun setDocument(
        collection: String,
        documentId: String,
        data: Any,
        merge: Boolean = true,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((Exception) -> Unit)? = null
    ) {
        try {
            val firestore = getFirestore()
            if (firestore == null) {
                onFailure?.invoke(IllegalStateException("Firestore instance unavailable"))
                return
            }

            val docRef = firestore.collection(collection).document(documentId)
            val task = if (merge) docRef.set(data, SetOptions.merge()) else docRef.set(data)
            
            task.addOnSuccessListener {
                Log.d(TAG, "Document $documentId saved to $collection")
                onSuccess?.invoke()
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to save document $documentId to $collection: ${e.message}", e)
                onFailure?.invoke(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in setDocument: ${e.message}", e)
            onFailure?.invoke(e)
        }
    }

    /**
     * Deletes a document safely from Firestore.
     */
    fun deleteDocument(
        collection: String,
        documentId: String,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((Exception) -> Unit)? = null
    ) {
        try {
            val firestore = getFirestore()
            if (firestore == null) {
                onFailure?.invoke(IllegalStateException("Firestore instance unavailable"))
                return
            }

            firestore.collection(collection).document(documentId).delete()
                .addOnSuccessListener {
                    Log.d(TAG, "Document $documentId deleted from $collection")
                    onSuccess?.invoke()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to delete document $documentId from $collection: ${e.message}", e)
                    onFailure?.invoke(e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in deleteDocument: ${e.message}", e)
            onFailure?.invoke(e)
        }
    }
}
