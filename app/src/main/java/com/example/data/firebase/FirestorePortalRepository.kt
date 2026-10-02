package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.PortalEvent
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestorePortalRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google to access cloud data.")
    }

    suspend fun saveUserProfile(user: FirebaseUser) {
        val uid = user.uid
        val profileData = mapOf(
            "userId" to uid,
            "email" to (user.email ?: ""),
            "displayName" to (user.displayName ?: ""),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            db.collection("users").document(uid).set(profileData).await()
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Failed to save user profile", e)
        }
    }

    suspend fun syncEventToCloud(event: PortalEvent) {
        val uid = auth.currentUser?.uid ?: return
        val docId = if (event.id > 0) "evt_${event.id}_${event.timestamp}" else "evt_${System.currentTimeMillis()}"
        val payload = mapOf(
            "id" to docId,
            "userId" to uid,
            "eventType" to event.eventType,
            "networkType" to event.networkType,
            "redirectUrl" to event.redirectUrl,
            "statusCode" to event.statusCode,
            "probeUrl" to event.probeUrl,
            "details" to event.details,
            "timestamp" to FieldValue.serverTimestamp()
        )
        try {
            db.collection("users")
                .document(uid)
                .collection("portal_events")
                .document(docId)
                .set(payload)
                .await()
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Failed to sync event to cloud", e)
        }
    }

    fun observeCloudEvents(): Flow<List<CloudPortalEvent>> = flow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            emit(emptyList())
            return@flow
        }
        val query = db.collection("users")
            .document(uid)
            .collection("portal_events")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)

        emitAll(
            query.snapshots()
                .map { snapshot -> snapshot.toObjects(CloudPortalEvent::class.java) }
                .catch { e ->
                    Log.e("FirestoreRepo", "Error listening to cloud events: ${e.message}", e)
                    emit(emptyList())
                }
        )
    }

    suspend fun saveUserSettings(probeUrl: String, autoOpen: Boolean, notifications: Boolean) {
        val uid = requireUserId()
        val payload = mapOf(
            "userId" to uid,
            "preferredProbeUrl" to probeUrl,
            "autoOpenBrowser" to autoOpen,
            "notificationsEnabled" to notifications,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            db.collection("users")
                .document(uid)
                .collection("settings")
                .document("preferences")
                .set(payload)
                .await()
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Failed to save user settings", e)
        }
    }

    fun observeUserSettings(): Flow<CloudUserSettings?> = flow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            emit(null)
            return@flow
        }
        val docRef = db.collection("users")
            .document(uid)
            .collection("settings")
            .document("preferences")

        emitAll(
            docRef.snapshots()
                .map { snapshot -> snapshot.toObject(CloudUserSettings::class.java) }
                .catch { e ->
                    Log.e("FirestoreRepo", "Error observing settings: ${e.message}", e)
                    emit(null)
                }
        )
    }
}
