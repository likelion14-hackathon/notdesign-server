package com.likelionknu.notdesign.analysis.client;

import com.likelionknu.notdesign.analysis.exception.ImageUploadException;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3Uploader {
    private static final Duration PRESIGN_TTL = Duration.ofMinutes(10);
    private static final String KEY_PREFIX = "skin-analysis/";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    /**
     * 이미지 파일을 검증하고 S3에 업로드한 뒤 presigned GET URL을 반환합니다.
     *
     * @param file 업로드할 이미지 파일
     * @return 분석 서버가 접근할 수 있는 presigned URL
     */
    public String upload(MultipartFile file) {
        validateImage(file);

        String key = KEY_PREFIX + UUID.randomUUID() + "." + extension(file.getOriginalFilename());
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

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.error("[S3Uploader] 업로드할 이미지가 비어있습니다.");
            throw new ImageUploadException();
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            log.error("[S3Uploader] 허용되지 않은 Content-Type: {}", contentType);
            throw new ImageUploadException();
        }

        if (!ALLOWED_EXTENSIONS.contains(extension(file.getOriginalFilename()))) {
            log.error("[S3Uploader] 허용되지 않은 확장자: {}", file.getOriginalFilename());
            throw new ImageUploadException();
        }
    }

    private String presignedGetUrl(String key) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(PRESIGN_TTL)
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    private String extension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        return dot >= 0 ? originalFilename.substring(dot + 1).toLowerCase() : "";
    }
}
