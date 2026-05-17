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

    /**
     * S3 파일 업로드 (내부 예외 전환 적용)
     */
    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        String originalName = file.getOriginalFilename();
        String extension = "";

        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }

        String savedName = UUID.randomUUID() + extension;

        try {
            // 💡 실제 파일 스트림 처리 및 S3 업로드 중 발생하는 IOException을 여기서 직접 낚아챕니다.
            var s3Resource = s3Template.upload(bucket, savedName, file.getInputStream());
            String uploadedUrl = s3Resource.getURL().toString();

            log.info("S3 파일 업로드 완료: {}", uploadedUrl);
            return uploadedUrl;

        } catch (IOException e) {
            // 💡 Checked Exception을 Unchecked 예외(RuntimeException)로 포장하여 새로 던집니다.
            // 원본 에러(e)를 생성자에 함께 넘겨주어야 나중에 에러 추적이 가능합니다.
            throw new RuntimeException("S3 Storage 이미지 파일 업로드 실패", e);
        }
    }

    /**
     * S3 파일 삭제 (기존 정책 유지)
     */
    public void deleteFile(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return;

        try {
            String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            s3Template.deleteObject(bucket, fileName);
            log.info("S3 파일 삭제 완료: {}", fileName);
        } catch (Exception e) {
            log.error("S3 파일 삭제 오류: ", e);
        }
    }
}