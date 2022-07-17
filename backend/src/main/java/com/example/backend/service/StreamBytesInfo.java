package com.example.backend.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
@Getter
@Setter
public class StreamBytesInfo {

    private final StreamingResponseBody responseBody;

    private final long fileSize;

    private final long rangeStart;

    private final long rangeEnd;

    private final String contentType;

    public StreamBytesInfo(StreamingResponseBody responseBody,
                           long fileSize, long rangeStart, long rangeStop,
                           String contentType) {
        this.responseBody = responseBody;
        this.fileSize = fileSize;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeStop;
        this.contentType = contentType;
    }
}
