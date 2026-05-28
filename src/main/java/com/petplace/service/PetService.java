package com.petplace.service;

import com.petplace.dto.request.PetRequest;
import com.petplace.dto.response.PetResponse;
import com.petplace.entity.Pet;
import com.petplace.entity.User;
import com.petplace.exception.BusinessException;
import com.petplace.exception.ErrorCode;
import com.petplace.repository.PetRepository;
import com.petplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetService {

    private final PetRepository petRepo;
    private final UserRepository userRepo;
    private final FileService fileService;

    public Page<PetResponse> getPets(Long userId, Pageable pageable) {
        return petRepo.findAllByUserId(userId, pageable)
                .map(PetResponse::from);
    }

    /**
     * 반려동물 신규 등록
     * ★ 규칙 적용: 트랜잭션 실패 시 신규 업로드된 S3 파일 자동 삭제
     */
    @Transactional(rollbackFor = Exception.class)
    public Pet addPet(Long userId, PetRequest req, MultipartFile image) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String imageUrl = null;
        final List<String> newlyUploadedFiles = new ArrayList<>(); // 롤백 대비 추적

        if (image != null && !image.isEmpty()) {
            String tempUrl = fileService.uploadFile(image);

            // 🌟 Null 방어: 업로드 성공 시에만 데이터 반영
            if (tempUrl != null) {
                imageUrl = tempUrl;
                newlyUploadedFiles.add(tempUrl);
            }
        }

        // 트랜잭션 롤백 시 파일 자동 삭제 동기화
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    newlyUploadedFiles.forEach(fileService::deleteFile);
                }
            }
        });

        Pet pet = Pet.createPet(user, req.getName(), req.getBirth(), req.getBreed(), imageUrl);
        return petRepo.save(pet);
    }

    /**
     * 반려동물 정보 및 프로필 이미지 수정
     * ★ 규칙 적용: 커밋 후 구버전 삭제(안전), 롤백 시 신규파일 회수(유령방지)
     */
    @Transactional(rollbackFor = Exception.class)
    public Pet updatePet(Long userId, Long petId, PetRequest req, MultipartFile image) {
        Pet pet = petRepo.findById(petId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_NOT_FOUND));

        if (!Objects.equals(pet.getUser().getId(), userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        final String oldImageUrl = pet.getImageUrl(); // 커밋 후 삭제용
        final List<String> newlyUploadedFiles = new ArrayList<>(); // 롤백 시 삭제용
        String nextImageUrl = pet.getImageUrl();

        // 💡 새 이미지가 들어온 경우에만 처리
        if (image != null && !image.isEmpty()) {
            String tempUrl = fileService.uploadFile(image);

            if (tempUrl != null) {
                nextImageUrl = tempUrl;
                newlyUploadedFiles.add(tempUrl);
            }
        }

        // 트랜잭션 동기화 등록
        String finalNextImageUrl = nextImageUrl;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // 커밋 성공 시에만 옛날 파일 삭제 (안전성 확보)
                if (oldImageUrl != null && !oldImageUrl.equals(finalNextImageUrl)) {
                    fileService.deleteFile(oldImageUrl);
                }
            }

            @Override
            public void afterCompletion(int status) {
                // 수정 중 에러 발생 시, 새로 업로드했던 파일만 삭제 (유령 파일 방지)
                if (status == STATUS_ROLLED_BACK) {
                    newlyUploadedFiles.forEach(fileService::deleteFile);
                }
            }
        });

        pet.updateInfo(req.getName(), req.getBirth(), req.getBreed(), nextImageUrl);
        return pet;
    }
}