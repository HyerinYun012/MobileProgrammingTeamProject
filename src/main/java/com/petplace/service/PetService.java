package com.petplace.service;

import com.petplace.dto.request.PetRequest;
import com.petplace.dto.response.PetResponse;
import com.petplace.entity.Pet;
import com.petplace.entity.User;
import com.petplace.exception.BusinessException;
import com.petplace.exception.ErrorCode; // 💡 ErrorCode import 추가
import com.petplace.repository.PetRepository;
import com.petplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetService {

    private final PetRepository petRepo;
    private final UserRepository userRepo;
    private final FileService fileService; // S3 연동

    /**
     * 반려동물 목록 조회 (페이징 적용 및 DTO 변환)
     */
    public Page<PetResponse> getPets(Long userId, Pageable pageable) {
        return petRepo.findAllByUserId(userId, pageable)
                .map(PetResponse::from);
    }

    /**
     * 반려동물 신규 등록 (S3 이미지 파일 업로드 포함)
     */
    @Transactional
    public Pet addPet(Long userId, PetRequest req, MultipartFile image) {
        // 💡 ErrorCode 적용: 사용자 존재 확인
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = fileService.uploadFile(image);
        }

        Pet pet = Pet.createPet(
                user,
                req.getName(),
                req.getBirth(),
                req.getBreed(),
                imageUrl
        );

        return petRepo.save(pet);
    }

    /**
     * 반려동물 정보 및 프로필 이미지 수정 (소유권 검증 + S3 옛날 파일 제거)
     */
    @Transactional
    public Pet updatePet(Long userId, Long petId, PetRequest req, MultipartFile image) {
        // 💡 ErrorCode 적용: 반려동물 존재 확인
        Pet pet = petRepo.findById(petId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_NOT_FOUND));

        // 💡 ErrorCode 적용: 권한 검증
        if (!Objects.equals(pet.getUser().getId(), userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        String currentImageUrl = pet.getImageUrl();

        // 새로운 이미지가 들어왔다면 S3 고아 파일 방지 로직 가동
        if (image != null && !image.isEmpty()) {
            if (currentImageUrl != null) {
                fileService.deleteFile(currentImageUrl); // 옛날 물리 파일 파괴
            }
            currentImageUrl = fileService.uploadFile(image); // 새 파일 저장 후 주소 갱신
        }

        pet.updateInfo(
                req.getName(),
                req.getBirth(),
                req.getBreed(),
                currentImageUrl
        );

        return pet;
    }
}