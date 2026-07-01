package com.example.cabbagemarket10.global.util;

import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StorageUploadUtils {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.region}")
    private String region;

    /**
     * @param file    업로드할 파일
     * @param dirName 버킷 내 폴더 경로 (예: "items", "profiles")
     * @return Public URL
     */
    public String upload(MultipartFile file, String dirName) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));

        // 입력받은 dirName을 기반으로 경로 세분화 (예: items/uuid.jpg 또는 profiles/uuid.png)
        String s3Key = dirName + "/" + UUID.randomUUID().toString() + extension;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, s3Key);

        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 파일 삭제 로직 (어떤 폴더 경로든 동적으로 찾아서 삭제 가능)
     */
    public void delete(String imageUrl, String dirName) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        try {
            // "items/" 또는 "profiles/" 위치를 기준으로 S3 Key 값 슬라이싱
            String targetMarker = dirName + "/";
            int keyStartIndex = imageUrl.indexOf(targetMarker);
            if (keyStartIndex < 0) {
                return;
            }
            String s3Key = imageUrl.substring(keyStartIndex);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
