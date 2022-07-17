package com.example.backend.persist;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "video_metadata")
@Getter
@Setter
public class VideoMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String fileName;

    @Column
    private String contentType;

    @Column
    private String description;

    @Column
    private Long fileSize;

    @Column
    private Long videoLength;

    public VideoMetadata(){

    }
}
