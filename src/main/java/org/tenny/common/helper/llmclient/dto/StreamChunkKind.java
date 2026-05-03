package org.tenny.common.helper.llmclient.dto;

/**
 * Distinguishes model chain-of-thought deltas from user-visible answer text in streaming APIs.
 */
public enum StreamChunkKind {
    REASONING,
    CONTENT
}
