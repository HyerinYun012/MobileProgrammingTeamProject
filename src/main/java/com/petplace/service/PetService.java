package com.petplace.service;

import com.petplace.dto.request.PetRequest;
import com.petplace.entity.Pet;
import com.petplace.entity.User;
import com.petplace.exception.BusinessException;
import com.petplace.repository.PetRepository;
import com.petplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetService {

    private final PetRepository petRepo;
    private final UserRepository userRepo;
    private final FileService fileService; // S3 연동

    /**
     * 반려동물 목록 조회
     */
    public List<?> getPets(Long userId) {
        return petRepo.findAllByUserId(userId);
    }

    /**
     * 반려동물 신규 등록 (S3 이미지 파일 업로드 포함)
     */
    @Transactional
    public Pet addPet(Long userId, PetRequest req, MultipartFile image) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다."));

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = fileService.uploadFile(image);
        }

        // 🌟 [수정] 닫혀버린 Setter와 생성자 대신, 엔티티에 구현한 정적 팩토리 메서드를 호출합니다.
        // 이로써 불완전한 상태의 객체가 생성되는 것을 원천 차단합니다.
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
        Pet pet = petRepo.findById(petId)
                .orElseThrow(() -> new BusinessException("반려동물 정보를 찾을 수 없습니다."));

        // Objects.equals 안전 장치로 경고 및 예외 원천 차단
        if (!Objects.equals(pet.getUser().getId(), userId)) {
            throw new BusinessException("해당 반려동물 정보에 대한 수정 권한이 없습니다.");
        }

        String currentImageUrl = pet.getImageUrl();

        // 새로운 이미지가 들어왔다면 S3 고아 파일 방지 로직 가동
        if (image != null && !image.isEmpty()) {
            if (currentImageUrl != null) {
                fileService.deleteFile(currentImageUrl); // 옛날 물리 파일 파괴
            }
            currentImageUrl = fileService.uploadFile(image); // 새 파일 저장 후 주소 갱신
        }

        // 💡 기존에 잘 작성해 두신 비즈니스 메서드가 Setter가 없는 엔티티를 완벽하게 제어합니다. (Dirty Checking)
        pet.updateInfo(req, currentImageUrl);

        return pet;
    }
}