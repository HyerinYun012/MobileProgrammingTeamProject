package com.petplace.repository;

import com.petplace.entity.Pet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetRepository extends JpaRepository<Pet, Long> {

    /**
     * 💡 [페이징 적용]
     * 기존 List<Pet>을 Page<Pet>으로 변경하여 메모리 과부하를 방지합니다.
     * * @param userId 조회할 유저 ID
     * @param pageable 페이징 정보 (페이지 번호, 사이즈, 정렬)
     * @return Page<Pet> 페이징 처리된 반려동물 목록
     */
    Page<Pet> findAllByUserId(Long userId, Pageable pageable);

    void deleteByUserId(Long userId);
}