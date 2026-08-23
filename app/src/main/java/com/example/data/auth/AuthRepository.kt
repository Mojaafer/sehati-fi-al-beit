package com.example.data.auth

import android.content.Context
import androidx.core.content.edit
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

sealed class AuthResult {
    data class EmailLinkSent(val email: String) : AuthResult()
    data class Success(val user: FirebaseUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
    /** The account chooser was dismissed. Not a failure, so the screen shows no error. */
    data object Cancelled : AuthResult()
}

/**
 * Firebase reports failures as untranslated English sentences, which surfaced verbatim in the
 * Arabic UI. Codes are matched where the SDK exposes one, falling back to the message text for
 * the exceptions that carry no code.
 */
internal fun authErrorMessage(e: Exception): String {
    val code = (e as? com.google.firebase.auth.FirebaseAuthException)?.errorCode ?: ""
    val text = e.message ?: ""

    return when {
        code == "ERROR_INVALID_ACTION_CODE" || code == "ERROR_EXPIRED_ACTION_CODE" ->
            "رابط الدخول غير صالح أو انتهت صلاحيته، اطلب رابطاً جديداً"
        code == "ERROR_TOO_MANY_REQUESTS" || text.contains("blocked all requests") ->
            "تم إرسال محاولات كثيرة، حاول بعد قليل"
        code == "ERROR_QUOTA_EXCEEDED" -> "تم تجاوز عدد الرسائل المسموح، حاول لاحقاً"
        code == "ERROR_OPERATION_NOT_ALLOWED" || text.contains("CONFIGURATION_NOT_FOUND") ->
            "خدمة تسجيل الدخول غير مهيأة، تواصل مع الدعم"
        code == "ERROR_INVALID_EMAIL" -> "البريد الإلكتروني غير صحيح"
        code == "ERROR_INVALID_CREDENTIAL" -> "تعذر التحقق من رابط الدخول، اطلب رابطاً جديداً"
        code == "ERROR_USER_DISABLED" -> "تم إيقاف هذا الحساب، تواصل مع الدعم"
        text.contains("network", ignoreCase = true) ->
            "تعذر الاتصال بالشبكة، تحقق من الإنترنت"
        else -> "تعذر إكمال العملية، حاول مرة أخرى"
    }
}

/**
 * Firebase rejects a malformed address with an English exception after a
 * round trip. Catching both here keeps the failure instant and in Arabic. The address test is
 * deliberately loose — anything stricter rejects valid addresses, and Firebase is the real judge.
 */
internal fun emailProblem(email: String): String? {
    val trimmed = email.trim()
    return when {
        trimmed.isEmpty() -> "أدخل البريد الإلكتروني"
        !Regex("^[^@\\s]+@[^@\\s.]+\\.[^@\\s]+$").matches(trimmed) -> "البريد الإلكتروني غير صحيح"
        else -> null
    }
}

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun sendEmailLink(email: String, context: Context): AuthResult {
        val normalized = email.trim().lowercase()
        return try {
            val settings = ActionCodeSettings.newBuilder()
                .setUrl("https://sehati-home-care.firebaseapp.com/emailSignIn")
                .setHandleCodeInApp(true)
                .setAndroidPackageName(context.packageName, true, null)
                .build()
            auth.sendSignInLinkToEmail(normalized, settings).await()
            context.getSharedPreferences("sehati_auth", Context.MODE_PRIVATE)
                .edit { putString("pending_email", normalized) }
            AuthResult.EmailLinkSent(normalized)
        } catch (e: Exception) {
            AuthResult.Error(authErrorMessage(e))
        }
    }

    fun isEmailSignInLink(link: String): Boolean = auth.isSignInWithEmailLink(link)

    fun pendingEmail(context: Context): String =
        context.getSharedPreferences("sehati_auth", Context.MODE_PRIVATE)
            .getString("pending_email", "").orEmpty()

    suspend fun completeEmailLink(email: String, link: String, context: Context): AuthResult {
        return try {
            val user = auth.signInWithEmailLink(email.trim().lowercase(), link).await().user
            if (user == null) {
                AuthResult.Error("تعذر تسجيل الدخول بالرابط، اطلب رابطاً جديداً")
            } else {
                ensureUserDocument(user)
                context.getSharedPreferences("sehati_auth", Context.MODE_PRIVATE)
                    .edit { remove("pending_email") }
                AuthResult.Success(user)
            }
        } catch (e: Exception) {
            AuthResult.Error(authErrorMessage(e))
        }
    }

    suspend fun signInAnonymously(): AuthResult {
        return try {
            val result = auth.signInAnonymously().await()
            val user = result.user

            if (user != null) {
                ensureUserDocument(user)
                AuthResult.Success(user)
            } else {
                AuthResult.Error("فشل الدخول كضيف")
            }
        } catch (e: Exception) {
            AuthResult.Error(authErrorMessage(e))
        }
    }

    /**
     * Google sign-in remains a fast alternative to passwordless email links. The web client id comes
     * from the OAuth client
     * the google-services plugin writes into BuildConfig's resources — Google verifies the token
     * against it, so a device-side id would be rejected.
     */
    suspend fun signInWithGoogle(context: Context): AuthResult {
        val option = GetSignInWithGoogleOption
            .Builder(context.getString(R.string.default_web_client_id))
            .build()
        val request = androidx.credentials.GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        val idToken = try {
            val response = CredentialManager.create(context).getCredential(context, request)
            val credential = response.credential
            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                return AuthResult.Error("تعذر تسجيل الدخول عبر Google")
            }
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (e: GetCredentialCancellationException) {
            return AuthResult.Cancelled
        } catch (e: NoCredentialException) {
            return AuthResult.Error("لا يوجد حساب Google على هذا الجهاز، أضف حساباً ثم حاول")
        } catch (e: GetCredentialException) {
            return AuthResult.Error("تعذر تسجيل الدخول عبر Google")
        }

        return try {
            val result = auth.signInWithCredential(
                GoogleAuthProvider.getCredential(idToken, null)
            ).await()
            val user = result.user

            if (user != null) {
                ensureUserDocument(user)
                AuthResult.Success(user)
            } else {
                AuthResult.Error("تعذر تسجيل الدخول عبر Google")
            }
        } catch (e: Exception) {
            AuthResult.Error(authErrorMessage(e))
        }
    }

    /**
     * An account can arrive with an email or anonymously, so the profile is seeded from
     * whatever the provider supplied. Only ever written on first sign-in: a later overwrite would
     * wipe the name and address the user edited on the profile screen.
     */
    private suspend fun ensureUserDocument(user: FirebaseUser) {
        val userRef = firestore.collection("users").document(user.uid)
        val snapshot = userRef.get().await()

        if (!snapshot.exists()) {
            val userData = hashMapOf(
                "email" to (user.email ?: ""),
                "name" to (user.displayName ?: ""),
                "role" to "PATIENT",
                "createdAt" to com.google.firebase.Timestamp.now()
            )
            userRef.set(userData).await()
        }
    }

    suspend fun fetchUserRole(uid: String): String {
        return try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            snapshot.getString("role") ?: "PATIENT"
        } catch (e: Exception) {
            "PATIENT"
        }
    }

    /** The provider profile this user owns, or empty when they are not a provider. */
    suspend fun fetchProviderId(uid: String): String {
        return try {
            firestore.collection("users").document(uid).get().await()
                .getString("providerId") ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /** Name and saved address for the profile screen; empty strings when nothing is stored yet. */
    suspend fun fetchUserProfile(uid: String): Pair<String, String> {
        return try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            (snapshot.getString("name") ?: "") to (snapshot.getString("address") ?: "")
        } catch (e: Exception) {
            "" to ""
        }
    }

    suspend fun updateUserProfile(uid: String, name: String, address: String) {
        firestore.collection("users").document(uid)
            .update(mapOf("name" to name, "address" to address)).await()
    }

    fun signOut() {
        auth.signOut()
    }
}
