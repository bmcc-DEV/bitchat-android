package com.ghostpay.android.pay.mesh

import com.ghostpay.android.pay.token.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * End-to-end integration test for PaymentFlowManager
 * Simulates atomic swap between two devices (buyer and seller)
 * 
 * This test verifies the three-step protocol:
 * 1. HANDSHAKE - Ephemeral key exchange and currency preference transmission
 * 2. COMMIT - Blinded token exchange (FNT-B and FNT-C)
 * 3. UNLOCK - Simultaneous reveal and validation
 */
class PaymentFlowIntegrationTest {

    private val signerKey = ByteArray(32) { it.toByte() }
    private lateinit var tokenFactory: TokenFactory
    private lateinit var validator: TokenValidator
    private lateinit var shadowCache: ShadowCache
    
    // Two separate instances simulating buyer and seller devices
    private lateinit var buyerFlowManager: PaymentFlowManager
    private lateinit var sellerFlowManager: PaymentFlowManager
    
    // Coroutine scope for async operations
    private val testScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Before
    fun setUp() {
        tokenFactory = TokenFactory(signerKey)
        validator = TokenValidator(signerKey)
        shadowCache = ShadowCache()
        
        // Initialize buyer and seller flow managers with their own scope
        buyerFlowManager = PaymentFlowManager(
            validator = validator,
            shadowCache = shadowCache,
            scope = testScope
        )
        
        sellerFlowManager = PaymentFlowManager(
            validator = validator,
            shadowCache = shadowCache,
            scope = testScope
        )
    }

    // ── Successful Payment Flow ────────────────────────────────────────────────

    @Test
    fun `complete atomic swap succeeds between two devices`() = runTest {
        // Arrange: Mint tokens for buyer and seller
        val buyerBearerToken = tokenFactory.mintBearer(10.0, "kg", "café")
        val sellerCommodityToken = tokenFactory.mintCommodity(10.0, "kg", "café")
        
        val buyerLatch = CountDownLatch(1)
        val sellerLatch = CountDownLatch(1)
        
        var buyerReceivedToken: Token? = null
        var sellerReceivedToken: Token? = null
        var buyerSuccess = false
        var sellerSuccess = false

        // Set up listeners
        buyerFlowManager.setListener(object : PaymentFlowManager.Listener {
            override fun onStateChanged(state: PaymentFlowManager.State) {
                println("Buyer state: $state")
            }

            override fun onSwapComplete(receivedToken: Token, fxHint: String?) {
                println("Buyer swap complete - received token: ${receivedToken.id}")
                buyerReceivedToken = receivedToken
                buyerSuccess = true
                buyerLatch.countDown()
            }

            override fun onSwapFailed(reason: String) {
                println("Buyer swap failed: $reason")
                buyerLatch.countDown()
            }
        })

        sellerFlowManager.setListener(object : PaymentFlowManager.Listener {
            override fun onStateChanged(state: PaymentFlowManager.State) {
                println("Seller state: $state")
            }

            override fun onSwapComplete(receivedToken: Token, fxHint: String?) {
                println("Seller swap complete - received token: ${receivedToken.id}")
                sellerReceivedToken = receivedToken
                sellerSuccess = true
                sellerLatch.countDown()
            }

            override fun onSwapFailed(reason: String) {
                println("Seller swap failed: $reason")
                sellerLatch.countDown()
            }
        })

        // Act: Execute the three-step protocol

        // Step 1: HANDSHAKE
        println("\n=== STEP 1: HANDSHAKE ===")
        val handshakePayload = buyerFlowManager.initiateHandshake(buyerBearerToken)
        assertNotNull("Handshake payload should not be null", handshakePayload)
        assertEquals("Handshake payload should be 72 bytes", 72, handshakePayload.size)

        val commitResponse = sellerFlowManager.acceptHandshake(handshakePayload, sellerCommodityToken)
        assertNotNull("Commit response should not be null", commitResponse)

        // Step 2: COMMIT
        println("\n=== STEP 2: COMMIT ===")
        val buyerCommitPayload = buyerFlowManager.processCommit(commitResponse)
        assertNotNull("Buyer commit payload should not be null", buyerCommitPayload)

        // Step 3: UNLOCK
        println("\n=== STEP 3: UNLOCK ===")
        // Extract nonces from commit payloads
        val sellerNonce = commitResponse.takeLast(32).toByteArray()
        val buyerNonce = buyerCommitPayload.takeLast(32).toByteArray()
        
        val sellerBlindedData = commitResponse.dropLast(32).toByteArray()
        val buyerBlindedData = buyerCommitPayload.dropLast(32).toByteArray()

        // Both parties unlock simultaneously
        buyerFlowManager.processUnlock(sellerNonce, sellerBlindedData, sellerCommodityToken, null)
        sellerFlowManager.processUnlock(buyerNonce, buyerBlindedData, buyerBearerToken, null)

        // Assert: Wait for completion and verify results
        val buyerCompleted = buyerLatch.await(5, TimeUnit.SECONDS)
        val sellerCompleted = sellerLatch.await(5, TimeUnit.SECONDS)

        assertTrue("Buyer should complete the swap", buyerCompleted)
        assertTrue("Seller should complete the swap", sellerCompleted)
        assertTrue("Buyer swap should succeed", buyerSuccess)
        assertTrue("Seller swap should succeed", sellerSuccess)
        
        assertNotNull("Buyer should receive commodity token", buyerReceivedToken)
        assertNotNull("Seller should receive bearer token", sellerReceivedToken)
        
        assertEquals("Buyer should receive seller's commodity token", 
            sellerCommodityToken.id, buyerReceivedToken?.id)
        assertEquals("Seller should receive buyer's bearer token", 
            buyerBearerToken.id, sellerReceivedToken?.id)
    }

    // ── Failure Scenarios ──────────────────────────────────────────────────────

    @Test
    fun `swap fails with mismatched token amounts`() = runTest {
        // Arrange: Tokens with different amounts
        val buyerBearerToken = tokenFactory.mintBearer(10.0, "kg", "café")
        val sellerCommodityToken = tokenFactory.mintCommodity(5.0, "kg", "café")
        
        val buyerLatch = CountDownLatch(1)
        var buyerFailed = false
        var failureReason: String? = null

        buyerFlowManager.setListener(object : PaymentFlowManager.Listener {
            override fun onStateChanged(state: PaymentFlowManager.State) {}

            override fun onSwapComplete(receivedToken: Token, fxHint: String?) {
                buyerLatch.countDown()
            }

            override fun onSwapFailed(reason: String) {
                buyerFailed = true
                failureReason = reason
                buyerLatch.countDown()
            }
        })

        // Act: Execute protocol
        val handshake = buyerFlowManager.initiateHandshake(buyerBearerToken)
        val commit = sellerFlowManager.acceptHandshake(handshake, sellerCommodityToken)
        val buyerCommit = buyerFlowManager.processCommit(commit)
        
        val sellerNonce = commit.takeLast(32).toByteArray()
        val sellerBlindedData = commit.dropLast(32).toByteArray()
        
        buyerFlowManager.processUnlock(sellerNonce, sellerBlindedData, sellerCommodityToken, null)

        // Assert: Should fail due to amount mismatch
        val completed = buyerLatch.await(5, TimeUnit.SECONDS)
        assertTrue("Buyer should complete (with failure)", completed)
        assertTrue("Swap should fail", buyerFailed)
        assertNotNull("Failure reason should be provided", failureReason)
        assertTrue("Failure should be due to invalid pair", 
            failureReason?.contains("invalid") ?: false)
    }

    @Test
    fun `swap prevents double-spend attack`() = runTest {
        // Arrange: Same tokens used twice
        val buyerBearerToken = tokenFactory.mintBearer(10.0, "kg", "café")
        val sellerCommodityToken1 = tokenFactory.mintCommodity(10.0, "kg", "café")
        val sellerCommodityToken2 = tokenFactory.mintCommodity(10.0, "kg", "café")
        
        // First swap should succeed
        val handshake1 = buyerFlowManager.initiateHandshake(buyerBearerToken)
        val commit1 = sellerFlowManager.acceptHandshake(handshake1, sellerCommodityToken1)
        val buyerCommit1 = buyerFlowManager.processCommit(commit1)
        
        val nonce1 = commit1.takeLast(32).toByteArray()
        val data1 = commit1.dropLast(32).toByteArray()
        
        buyerFlowManager.processUnlock(nonce1, data1, sellerCommodityToken1, null)
        
        // Give first swap time to complete
        delay(100)

        // Second swap attempt with same bearer token should fail
        val latch = CountDownLatch(1)
        var failed = false
        var reason: String? = null

        val buyerFlowManager2 = PaymentFlowManager(validator, shadowCache, testScope)
        buyerFlowManager2.setListener(object : PaymentFlowManager.Listener {
            override fun onStateChanged(state: PaymentFlowManager.State) {}
            override fun onSwapComplete(receivedToken: Token, fxHint: String?) {
                latch.countDown()
            }
            override fun onSwapFailed(failureReason: String) {
                failed = true
                reason = failureReason
                latch.countDown()
            }
        })

        // Act: Attempt second swap
        val handshake2 = buyerFlowManager2.initiateHandshake(buyerBearerToken)
        val commit2 = sellerFlowManager.acceptHandshake(handshake2, sellerCommodityToken2)
        val buyerCommit2 = buyerFlowManager2.processCommit(commit2)
        
        val nonce2 = commit2.takeLast(32).toByteArray()
        val data2 = commit2.dropLast(32).toByteArray()
        
        buyerFlowManager2.processUnlock(nonce2, data2, sellerCommodityToken2, null)

        // Assert: Second swap should fail due to double-spend detection
        val completed = latch.await(5, TimeUnit.SECONDS)
        assertTrue("Second swap should complete (with failure)", completed)
        assertTrue("Second swap should fail", failed)
        assertTrue("Should detect double-spend", 
            reason?.contains("Double-spend") ?: false)
    }

    @Test
    fun `buyer must provide FNT_B bearer token`() = runTest {
        // Arrange: Try to use commodity token as buyer (should fail)
        val wrongToken = tokenFactory.mintCommodity(10.0, "kg", "café")
        
        // Act & Assert: Should throw exception
        try {
            buyerFlowManager.initiateHandshake(wrongToken)
            fail("Should throw exception for non-bearer token")
        } catch (e: IllegalArgumentException) {
            assertTrue("Error message should mention FNT_B", 
                e.message?.contains("FNT_B") ?: false)
        }
    }
}
