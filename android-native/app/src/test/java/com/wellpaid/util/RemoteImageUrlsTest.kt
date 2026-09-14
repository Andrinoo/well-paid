package com.wellpaid.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteImageUrlsTest {
    @Test
    fun `adds https to protocol-relative google thumbs`() {
        val raw = "//encrypted-tbn0.gstatic.com/shopping?q=tbn:abc"
        assertEquals(
            "https://encrypted-tbn0.gstatic.com/shopping?q=tbn:abc",
            RemoteImageUrls.normalize(raw),
        )
    }

    @Test
    fun `rejects javascript urls`() {
        assertNull(RemoteImageUrls.normalize("javascript:alert(1)"))
    }

    @Test
    fun `google image hosts include gstatic`() {
        assertTrue(RemoteImageUrls.isGoogleImageHost("encrypted-tbn3.gstatic.com"))
    }
}
