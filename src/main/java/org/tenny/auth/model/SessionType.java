package org.tenny.auth.model;

/**
 * Mirrors conversation kind in Redis-backed {@link org.tenny.common.session.ConversationStore}.
 */
public enum SessionType {
    GENERIC,
    LOGISTICS
}
