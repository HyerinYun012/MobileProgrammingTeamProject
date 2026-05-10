package com.petplace.service;

import com.petplace.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
public class FileService {

    // 상대 경로 설정 (윈도우/리눅스 공용)
    private final String uploadPath = "uploads" + File.separator;

    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        // 절대 경로로 변환하여 폴더 생성
        String absolutePath = Paths.get(uploadPath).toAbsolutePath().toString() + File.separator;
        File folder = new File(absolutePath);

        if (!folder.exists() && !folder.mkdirs()) {
            log.error("폴더 생성 실패: {}", absolutePath);
            throw new BusinessException("서버 저장소 준비에 실패했습니다.");
        }

        // 파일명 및 확장자 처리
        String originalName = file.getOriginalFilename();
        String extension = "";

        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }

        String savedName = UUID.randomUUID() + extension;

        // 물리적 저장
        try {
            file.transferTo(new File(absolutePath + savedName));
            log.info("파일 저장 완료: {}", absolutePath + savedName);
        } catch (IOException e) {
            log.error("파일 저장 오류: ", e);
            throw new BusinessException("이미지 저장 중 오류가 발생했습니다.");
        }

        return "/images/" + savedName;
    }

    public void deleteFile(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return;

        String fileName = imageUrl.replace("/images/", "");
        String absolutePath = Paths.get(uploadPath).toAbsolutePath().toString() + File.separator;
        File file = new File(absolutePath + fileName);

        if (file.exists()) {
            if (!file.delete()) {
                log.warn("파일 삭제 실패: {}", file.getAbsolutePath());
            }
        }
    }
}