package org.tenny.common.helper.llmclient.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class StreamTextChunk {
    private final StreamChunkKind kind;
    private final String text;
}
