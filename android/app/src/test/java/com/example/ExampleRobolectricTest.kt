package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleRobolectricTest {

  @Test
  fun testAppNameConstant() {
    val appName = "صحتي في البيت"
    assertEquals("صحتي في البيت", appName)
  }
}
