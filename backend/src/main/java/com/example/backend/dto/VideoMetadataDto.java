package com.example.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VideoMetadataDto {

    private Long id;

    private String description;

    private String contentType;

    private String previewUrl;

    private String streamUrl;

    public VideoMetadataDto(){
    }
}
