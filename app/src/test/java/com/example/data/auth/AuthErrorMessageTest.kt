package com.example.data.auth

import com.google.firebase.auth.FirebaseAuthException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthErrorMessageTest {
    private fun firebaseError(code: String) = FirebaseAuthException(code, "some english text")

    @Test
    fun expiredEmailLinkAsksForANewLink() {
        val message = authErrorMessage(firebaseError("ERROR_EXPIRED_ACTION_CODE"))
        assertTrue(message.contains("رابطاً جديداً"))
        assertNoLatinLetters(message)
    }

    @Test
    fun invalidEmailLinkExplainsTheNextStep() {
        assertEquals(
            "تعذر التحقق من رابط الدخول، اطلب رابطاً جديداً",
            authErrorMessage(firebaseError("ERROR_INVALID_CREDENTIAL"))
        )
    }

    @Test
    fun throttlingAndNetworkFailuresRemainActionable() {
        assertEquals(
            "تم إرسال محاولات كثيرة، حاول بعد قليل",
            authErrorMessage(Exception("We have blocked all requests from this device"))
        )
        assertTrue(authErrorMessage(Exception("A network error occurred")).contains("الإنترنت"))
    }

    @Test
    fun unknownFailureFallsBackToArabic() {
        val message = authErrorMessage(Exception("Something nobody mapped"))
        assertEquals("تعذر إكمال العملية، حاول مرة أخرى", message)
        assertNoLatinLetters(message)
    }

    private fun assertNoLatinLetters(value: String) {
        assertFalse(value.any { it in 'A'..'Z' || it in 'a'..'z' })
    }
}
