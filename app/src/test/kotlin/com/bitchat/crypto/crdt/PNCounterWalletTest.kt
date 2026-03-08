package com.bitchat.crypto.crdt

import org.junit.Assert.*
import org.junit.Test

class PNCounterWalletTest {
    
    @Test
    fun `initial balance is zero`() {
        val wallet = PNCounterWallet.create("alice")
        assertEquals(0.0, wallet.balance(), 1e-6)
    }

    @Test
    fun `deposit increases balance`() {
        val wallet = PNCounterWallet.create("alice")
        wallet.deposit(100.0)
        
        assertEquals(100.0, wallet.balance(), 1e-6)
    }

    @Test
    fun `withdraw decreases balance`() {
        val wallet = PNCounterWallet.create("alice")
        wallet.deposit(100.0)
        wallet.withdraw(30.0)
        
        assertEquals(70.0, wallet.balance(), 1e-6)
    }

    @Test
    fun `withdraw fails with insufficient balance`() {
        val wallet = PNCounterWallet.create("alice")
        wallet.deposit(10.0)
        
        val result = wallet.withdraw(20.0)
        
        assertNull(result) // Veto Ancestral
        assertEquals(10.0, wallet.balance(), 1e-6)
    }

    @Test
    fun `merge combines two wallets`() {
        val wallet1 = PNCounterWallet.create("alice")
        wallet1.deposit(100.0)
        
        val wallet2 = PNCounterWallet.create("alice")
        wallet2.deposit(50.0)
        
        val merged = wallet1.merge(wallet2)
        
        assertEquals(150.0, merged.balance(), 1e-6)
    }

    @Test
    fun `merge is commutative`() {
        val wallet1 = PNCounterWallet.create("alice")
        wallet1.deposit(100.0)
        
        val wallet2 = PNCounterWallet.create("alice")
        wallet2.deposit(50.0)
        
        val merged1 = wallet1.merge(wallet2)
        val merged2 = wallet2.merge(wallet1)
        
        assertEquals(merged1.balance(), merged2.balance(), 1e-6)
    }

    @Test
    fun `merge is idempotent`() {
        val wallet = PNCounterWallet.create("alice")
        wallet.deposit(100.0)
        
        val merged = wallet.merge(wallet)
        
        assertEquals(wallet.balance(), merged.balance(), 1e-6)
    }

    @Test
    fun `offline convergence scenario`() {
        // Alice has two devices that sync later
        val device1 = PNCounterWallet.create("alice")
        val device2 = PNCounterWallet.create("alice")
        
        // Device 1 offline: deposit 100
        device1.deposit(100.0)
        
        // Device 2 offline: deposit 50
        device2.deposit(50.0)
        
        // Devices meet and sync
        val synced1 = device1.merge(device2)
        val synced2 = device2.merge(device1)
        
        // Both converge to same state
        assertEquals(150.0, synced1.balance(), 1e-6)
        assertEquals(150.0, synced2.balance(), 1e-6)
    }

    @Test
    fun `transaction history is recorded`() {
        val wallet = PNCounterWallet.create("alice")
        wallet.deposit(100.0)
        wallet.withdraw(30.0)
        wallet.deposit(50.0)
        
        val history = wallet.getHistory()
        
        assertEquals(3, history.size)
        assertEquals(PNCounterWallet.TransactionType.DEPOSIT, history[0].type)
        assertEquals(100.0, history[0].amount, 1e-6)
    }
}
