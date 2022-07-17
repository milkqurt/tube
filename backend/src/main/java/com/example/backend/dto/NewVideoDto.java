package com.example.backend.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class NewVideoDto {

    private String description;

    private MultipartFile file;

    public NewVideoDto(){
    }
}
