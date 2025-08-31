package com.tvz.mediaapp.backend.service;

import com.tvz.mediaapp.backend.model.Post;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.presigned-url-expiration-minutes}")
    private long expirationMinutes;

    private static final String PENDING_UPLOAD_PREFIX = "uploads/pending/";
    private static final String PERMANENT_MEDIA_PREFIX = "media/posts/";

    public URL generatePreSignedUploadUrl(String uniqueFilename) {
        String objectKey = PENDING_UPLOAD_PREFIX + uniqueFilename;

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expirationMinutes))
                .putObjectRequest(builder -> builder
                        .bucket(bucketName)
                        .key(objectKey))
                .build();

        return s3Presigner.presignPutObject(presignRequest).url();
    }

    public Map<String, String> generatePreSignedDownloadUrls(String postUuid, List<String> filenames) {
        Map<String, String> urls = new HashMap<>();

        for (String filename : filenames) {
            String objectKey = PERMANENT_MEDIA_PREFIX + postUuid + "/" + filename;
            try {
                GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(expirationMinutes))
                        .getObjectRequest(builder -> builder
                                .bucket(bucketName)
                                .key(objectKey))
                        .build();

                urls.put(filename, s3Presigner.presignGetObject(presignRequest).url().toString());
            } catch (Exception e) {
                log.error("Could not generate download URL for {}", objectKey, e);
                urls.put(filename, "");
            }
        }
        return urls;
    }

    public void movePendingFilesToPermanentLocation(Post post, List<String> filenames) {
        log.info("Moving {} files from pending to permanent location for post {}", filenames.size(), post.getUuid());

        for (String filename : filenames) {
            String sourceKey = PENDING_UPLOAD_PREFIX + filename;
            String destinationKey = PERMANENT_MEDIA_PREFIX + post.getUuid().toString() + "/" + filename;

            log.debug("Attempting to move file: {} -> {}", sourceKey, destinationKey);

            try {
                s3Client.copyObject(builder -> builder
                        .sourceBucket(bucketName)
                        .sourceKey(sourceKey)
                        .destinationBucket(bucketName)
                        .destinationKey(destinationKey));

                s3Client.deleteObject(builder -> builder
                        .bucket(bucketName)
                        .key(sourceKey));

                log.info("Successfully moved file {} to permanent location", filename);

            } catch (NoSuchKeyException e) {
                log.error("Source file not found in pending location: {}. Upload may have failed.", sourceKey);
            }

            String thumbnailFilename = getThumbnailFilename(filename);
            String sourceThumbnailKey = PENDING_UPLOAD_PREFIX + thumbnailFilename;
            String destThumbnailKey = PERMANENT_MEDIA_PREFIX + post.getUuid().toString() + "/" + thumbnailFilename;

            try {
                s3Client.copyObject(builder -> builder
                        .sourceBucket(bucketName)
                        .sourceKey(sourceThumbnailKey)
                        .destinationBucket(bucketName)
                        .destinationKey(destThumbnailKey));

                s3Client.deleteObject(builder -> builder
                        .bucket(bucketName)
                        .key(sourceThumbnailKey));

                log.debug("Successfully moved thumbnail {} to permanent location", thumbnailFilename);
            } catch (NoSuchKeyException e) {
                log.debug("Thumbnail not found in pending location: {}", sourceThumbnailKey);
            }
        }

        log.info("Completed moving all files to permanent location for post {}", post.getUuid());
    }

    private String getThumbnailFilename(String originalFilename) {
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            String nameWithoutExt = originalFilename.substring(0, lastDotIndex);
            return nameWithoutExt + "_thumb.jpg";
        } else {
            return originalFilename + "_thumb.jpg";
        }
    }

    public void deleteMediaObjects(Post post, List<String> filenamesToDelete) {
        for (String filename : filenamesToDelete) {
            String objectKey = PERMANENT_MEDIA_PREFIX + post.getUuid().toString() + "/" + filename;
            deleteObject(objectKey);

            String thumbnailFilename = getThumbnailFilename(filename);
            String thumbnailKey = PERMANENT_MEDIA_PREFIX + post.getUuid().toString() + "/" + thumbnailFilename;
            deleteObject(thumbnailKey);
        }
    }

    public void deleteMediaForPost(Post post) {
        if (post.getMediaUris() == null || post.getMediaUris().isEmpty()) return;

        for (String mediaUri : post.getMediaUris()) {
            String objectKey = PERMANENT_MEDIA_PREFIX + post.getUuid().toString() + "/" + mediaUri;
            deleteObject(objectKey);

            String thumbnailFilename = getThumbnailFilename(mediaUri);
            String thumbnailKey = PERMANENT_MEDIA_PREFIX + post.getUuid().toString() + "/" + thumbnailFilename;
            deleteObject(thumbnailKey);
        }
    }

    private void deleteObject(String key) {
        try {
            s3Client.deleteObject(builder -> builder
                    .bucket(bucketName)
                    .key(key));
            log.debug("Successfully deleted object: {}", key);
        } catch (Exception e) {
            log.error("Failed to delete S3 object: {}", key, e);
        }
    }

    public void cleanupPendingUploads() {
        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(PENDING_UPLOAD_PREFIX)
                    .build();

            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

            for (S3Object object : listResponse.contents()) {
                if (object.lastModified().isBefore(java.time.Instant.now().minus(java.time.Duration.ofHours(24)))) {
                    deleteObject(object.key());
                    log.info("Cleaned up orphaned pending file: {}", object.key());
                }
            }
        } catch (Exception e) {
            log.error("Failed to cleanup pending uploads", e);
        }
    }
}