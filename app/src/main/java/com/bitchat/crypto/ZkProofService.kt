package com.bitchat.crypto

import java.security.MessageDigest

/**
 * Stub zero-knowledge proof service. In practice proofs would be generated and
 * verified using ZK-SNARK/PLONK circuits; here we simply return a hash.
 */
object ZkProofService {
    data class Proof(val value: String)

    interface ZkProofProvider {
        fun prove(statement: String): Proof
        fun verify(statement: String, proof: Proof): Boolean
    }

    private object LegacyProvider : ZkProofProvider {
        override fun prove(statement: String): Proof = Proof("PROOF:${statement.reversed()}")
        override fun verify(statement: String, proof: Proof): Boolean {
            return proof.value == "PROOF:${statement.reversed()}"
        }
    }

    /**
     * Hash transcript provider: deterministic pseudo-circuit adapter.
     * This is still not a real zk-SNARK, but it gives stronger structure than plain reverse-string.
     */
    private object HashTranscriptProvider : ZkProofProvider {
        override fun prove(statement: String): Proof {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(("zk|" + statement).toByteArray())
            return Proof(bytes.joinToString("") { "%02x".format(it) })
        }

        override fun verify(statement: String, proof: Proof): Boolean {
            return prove(statement).value == proof.value
        }
    }

    @Volatile
    private var provider: ZkProofProvider = HashTranscriptProvider

    fun useLegacyProvider() {
        provider = LegacyProvider
    }

    fun useHashTranscriptProvider() {
        provider = HashTranscriptProvider
    }

    /**
     * Generate a proof for a given statement (represented as string).
     */
    fun prove(statement: String): Proof {
        return provider.prove(statement)
    }

    /**
     * Verify that proof corresponds to statement.
     */
    fun verify(statement: String, proof: Proof): Boolean {
        return provider.verify(statement, proof)
    }
}
