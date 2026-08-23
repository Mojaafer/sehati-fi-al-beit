package com.example.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmailProblemTest {
    @Test
    fun validEmailPasses() {
        assertNull(emailProblem("user@example.com"))
        assertNull(emailProblem("  user@example.com  "))
    }

    @Test
    fun missingOrInvalidEmailFails() {
        assertEquals("أدخل البريد الإلكتروني", emailProblem(""))
        assertEquals("البريد الإلكتروني غير صحيح", emailProblem("user.example.com"))
        assertEquals("البريد الإلكتروني غير صحيح", emailProblem("user@example"))
    }
}
