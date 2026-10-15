package com.ntando.ivu.data.entity

/**
 * Synchronization status for offline-first entity records.
 */
enum class SyncState {
    /** Record is fully synchronized with remote server. */
    SYNCED,
    /** Record was created locally offline and needs to be posted to server. */
    PENDING_CREATE,
    /** Record was updated locally offline and needs to be pushed to server. */
    PENDING_UPDATE,
    /** Sync attempt encountered an unrecoverable 4xx client error. */
    FAILED
}
