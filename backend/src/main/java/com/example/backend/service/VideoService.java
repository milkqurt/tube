package com.example.backend.service;

import com.example.backend.dto.NewVideoDto;
import com.example.backend.dto.VideoMetadataDto;
import com.example.backend.persist.VideoMetadata;
import com.example.backend.persist.VideoMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.example.backend.Utils.removeFileExt;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

@Service
public class VideoService {

    private final Logger logger = LoggerFactory.getLogger(VideoService.class);

    @Value("${data.folder}")
    private String dataFolder;

    private final VideoMetadataRepository repository;

    private final FrameGrabberService frameGrabberService;

    @Autowired
    public VideoService(VideoMetadataRepository repository, FrameGrabberService frameGrabberService) {
        this.repository = repository;
        this.frameGrabberService = frameGrabberService;
    }

    public List<VideoMetadataDto> findAllVideoMetadata() {

        return repository.findAll().stream()
                .map(VideoService::convert)
                .collect(Collectors.toList());
    }

    public Optional<VideoMetadataDto> findById(Long id) {

        return repository.findById(id)
                .map(VideoService::convert);
    }

    private static VideoMetadataDto convert(VideoMetadata vmd) {
        VideoMetadataDto repr = new VideoMetadataDto();
        repr.setId(vmd.getId());
        repr.setPreviewUrl("/api/v1/video/preview/" + vmd.getId());
        repr.setStreamUrl("/api/v1/video/stream/" + vmd.getId());
        repr.setDescription(vmd.getDescription());
        repr.setContentType(vmd.getContentType());

        return repr;
    }

    public Optional<InputStream> getPreviewInputStream(Long id) {

        return repository.findById(id)
                .flatMap(vmd -> {
                    Path previewPicturePath = Path.of(dataFolder,
                            vmd.getId().toString(),
                            removeFileExt(vmd.getFileName()) + ".jpeg");
                    if (!Files.exists(previewPicturePath)) {
                        return Optional.empty();
                    }
                    try {
                        return Optional.of(Files.newInputStream(previewPicturePath));
                    } catch (IOException ex) {
                        logger.error("", ex);
                        return Optional.empty();
                    }
                });
    }

    @Transactional
    public void saveNewVideo(NewVideoDto newVideoRepr) {
        VideoMetadata metadata = new VideoMetadata();
        metadata.setFileName(newVideoRepr.getFile().getOriginalFilename());
        metadata.setContentType(newVideoRepr.getFile().getContentType());
        metadata.setFileSize(newVideoRepr.getFile().getSize());
        metadata.setDescription(newVideoRepr.getDescription());
        repository.save(metadata);

        Path directory = Path.of(dataFolder, metadata.getId().toString());
        try {
            Files.createDirectory(directory);
            Path file = Path.of(directory.toString(), newVideoRepr.getFile().getOriginalFilename());
            try (OutputStream output = Files.newOutputStream(file, CREATE, WRITE)) {
                newVideoRepr.getFile().getInputStream().transferTo(output);
            }
            long videoLength = frameGrabberService.generatePreviewPictures(file);
            metadata.setVideoLength(videoLength);
            repository.save(metadata);
        } catch (IOException ex) {
            logger.error("", ex);
            throw new IllegalStateException(ex);
        }
    }

    public Optional<StreamBytesInfo> getStreamBytes(Long id, HttpRange range) {
        Optional<VideoMetadata> byId = repository.findById(id);
        if (byId.isEmpty()) {
            return Optional.empty();
        }
        Path filePath = Path.of(dataFolder, Long.toString(id), byId.get().getFileName());
        if (!Files.exists(filePath)) {
            logger.error("File {} not found", filePath);
            return Optional.empty();
        }
        try {
            long fileSize = Files.size(filePath);
            long chunkSize = fileSize / 100;
            if (range == null) {
                return Optional.of(new StreamBytesInfo(
                        out -> Files.newInputStream(filePath).transferTo(out),
                        fileSize, 0, fileSize, byId.get().getContentType()));
            }

            long rangeStart = range.getRangeStart(0);
            long rangeEnd = rangeStart + chunkSize; // range.getRangeEnd(fileSize);
            if (rangeEnd >= fileSize) {
                rangeEnd = fileSize - 1;
            }
            long finalRangeEnd = rangeEnd;
            return Optional.of(new StreamBytesInfo(
                    out -> {
                        try (InputStream inputStream = Files.newInputStream(filePath)) {
                            inputStream.skip(rangeStart);
                            byte[] bytes = inputStream.readNBytes((int) ((finalRangeEnd - rangeStart) + 1));
                            out.write(bytes);
                        }
                    },
                    fileSize, rangeStart, rangeEnd, byId.get().getContentType()));
        } catch (IOException ex) {
            logger.error("", ex);
            return Optional.empty();
        }
    }
}
