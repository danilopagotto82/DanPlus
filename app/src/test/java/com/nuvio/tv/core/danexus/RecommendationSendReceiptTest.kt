package com.nuvio.tv.core.danexus

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationSendReceiptTest {
    @Test
    fun `a successful response without a receipt for exactly one recipient is not a confirmation`() = runTest {
        val row = ProfileRecommendation("request", 1, 2, "tt123", "movie", "Title", null, 0, toServerId = "recipient")
        val account = mockk<RecommendationAccount> {
            io.mockk.every { json } returns Json.Default
            coEvery { request("owner", "sender", "send", any(), null) } returns buildJsonObject {}
        }
        val sync = RecommendationSync(account, mockk())
        assertTrue(runCatching { sync.send("owner", "sender", row) }.isFailure)
        coEvery { account.request("owner", "sender", "send", any(), null) } returns buildJsonObject { put("accepted", 0) }
        assertTrue(runCatching { sync.send("owner", "sender", row) }.isFailure)
        coEvery { account.request("owner", "sender", "send", any(), null) } returns buildJsonObject { put("accepted", 1) }
        sync.send("owner", "sender", row)
    }
}
