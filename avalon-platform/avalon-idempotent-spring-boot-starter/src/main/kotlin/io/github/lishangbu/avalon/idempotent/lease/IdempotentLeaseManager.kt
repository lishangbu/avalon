package io.github.lishangbu.avalon.idempotent.lease

/**
 * Periodically renews idempotent processing leases while a method is still running.
 */
interface IdempotentLeaseManager {
    fun start(
        key: String,
        token: String,
    ): LeaseHandle

    fun interface LeaseHandle {
        fun stop()
    }
}
