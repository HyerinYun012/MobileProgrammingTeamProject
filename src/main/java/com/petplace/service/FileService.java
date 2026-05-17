package com.petplace.service;

import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final S3Template s3Template;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    // 💡 throws IOException을 명시하여 예외 처리를 GlobalExceptionHandler로 위임합니다.
    public String uploadFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;

        String originalName = file.getOriginalFilename();
        String extension = "";

        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }

        String savedName = UUID.randomUUID() + extension;

        // 💡 try-catch 방어막을 제거하고 직진합니다. 에러가 나면 전역 핸들러가 알아서 가로챕니다.
        var s3Resource = s3Template.upload(bucket, savedName, file.getInputStream());
        String uploadedUrl = s3Resource.getURL().toString();

        log.info("S3 파일 업로드 완료: {}", uploadedUrl);
        return uploadedUrl;
    }

    public void deleteFile(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return;

        try {
            String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            s3Template.deleteObject(bucket, fileName);
            log.info("S3 파일 삭제 완료: {}", fileName);
        } catch (Exception e) {
            // 삭제 실패는 런타임에 치명적이지 않으므로 로그만 남기는 기존 정책 유지
            log.error("S3 파일 삭제 오류: ", e);
        }
    }
}