package com.petplace.service;

import com.petplace.exception.BusinessException;
import com.petplace.exception.ErrorCode;
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
     * S3 파일 업로드 (ErrorCode 적용)
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
            var s3Resource = s3Template.upload(bucket, savedName, file.getInputStream());
            String uploadedUrl = s3Resource.getURL().toString();

            log.info("S3 파일 업로드 완료: {}", uploadedUrl);
            return uploadedUrl;

        } catch (IOException e) {
            // 💡 실제 원인(e)은 서버 로그에 기록하여 디버깅이 가능하도록 함
            log.error("S3 파일 업로드 실패: ", e);
            // 💡 클라이언트에게는 정의된 에러 코드를 담은 비즈니스 예외를 던짐
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * S3 파일 삭제 (ErrorCode 및 로깅 강화)
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