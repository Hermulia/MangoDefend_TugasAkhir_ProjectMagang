package com.riset.mangodefendd.auth

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resumeWithException

data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String? = null
)

@Singleton
class AuthenticationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth
) {
    private lateinit var googleSignInClient: GoogleSignInClient

    init {
        setupGoogleSignIn()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("875859066408-8cjc47okivi3mdbdcd9ug9korj91rn2b.apps.googleusercontent.com")
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    fun getCurrentUser(): AuthUser? {
        val firebaseUser = firebaseAuth.currentUser
        return if (firebaseUser != null) {
            AuthUser(
                uid = firebaseUser.uid,
                email = firebaseUser.email,
                displayName = firebaseUser.displayName,
                photoUrl = firebaseUser.photoUrl?.toString()
            )
        } else null
    }

    fun isLoggedIn(): Boolean = firebaseAuth.currentUser != null

    suspend fun signInWithGoogle(idToken: String): Pair<AuthUser, String> = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    firebaseUser.getIdToken(true).addOnSuccessListener { tokenResult ->
                        val token = tokenResult.token ?: ""
                        continuation.resume(
                            Pair(
                                AuthUser(
                                    uid = firebaseUser.uid,
                                    email = firebaseUser.email,
                                    displayName = firebaseUser.displayName,
                                    photoUrl = firebaseUser.photoUrl?.toString()
                                ),
                                token
                            )
                        ) { _ -> }
                    }.addOnFailureListener {
                        continuation.resumeWithException(it)
                    }
                } else {
                    continuation.resumeWithException(Exception("Firebase user is null"))
                }
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
    }

    fun signOut() {
        firebaseAuth.signOut()
        googleSignInClient.signOut()
    }

    fun getSignInIntent(): android.content.Intent = googleSignInClient.signInIntent
}


