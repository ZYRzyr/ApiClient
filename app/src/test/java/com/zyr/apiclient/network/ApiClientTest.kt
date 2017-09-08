package com.zyr.apiclient.network

import org.junit.Assert.assertEquals
import org.junit.Test


class ApiClientTest {
    @Test
    fun testApiClientIsSingleton() {
        val a = ApiClient.instance
        val b = ApiClient.instance
        assertEquals(a, b)
    }
}
