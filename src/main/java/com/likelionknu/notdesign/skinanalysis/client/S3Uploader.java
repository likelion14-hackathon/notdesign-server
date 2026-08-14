package com.likelionknu.notdesign.skinanalysis.client;

import com.likelionknu.notdesign.skinanalysis.exception.ImageUploadException;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * 업로드된 이미지 파일을 S3에 저장하고, analyze 서버가 다운로드할 수 있는 presigned GET URL을 반환한다.
 * 버킷을 공개하지 않아도 되도록 presigned URL(10분 유효)을 사용한다. 분석 요청은 수 초 내 처리되므로 충분하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class S3Uploader {
    private static final Duration PRESIGN_TTL = Duration.ofMinutes(10);
    private static final String KEY_PREFIX = "skin-analysis/";

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    /**
     * 이미지 파일을 S3에 업로드하고 presigned GET URL을 반환한다.
     *
     * @param file 업로드할 이미지 파일
     * @return 분석 서버가 접근 가능한 presigned URL
     */
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.error("[S3Uploader] 업로드할 이미지가 비어있습니다.");
            throw new ImageUploadException();
        }

        String key = KEY_PREFIX + UUID.randomUUID() + extension(file.getOriginalFilename());
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException | SdkException e) {
            log.error("[S3Uploader] S3 업로드 실패: key={}, message={}", key, e.getMessage());
            throw new ImageUploadException();
        }

        String url = presignedGetUrl(key);
        log.info("[S3Uploader] 업로드 완료: key={}", key);
        return url;
    }

    private String presignedGetUrl(String key) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(PRESIGN_TTL)
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    private String extension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        return dot >= 0 ? originalFilename.substring(dot) : "";
    }
}
