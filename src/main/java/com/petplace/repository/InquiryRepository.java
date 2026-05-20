package com.petplace.repository;

import com.petplace.entity.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    /**
     * 💡 [페이징 적용] 관리자 대시보드용 전체 1:1 문의 내역 페이징 조회
     * * @param pageable 페이징 정보 (Sort 정보 포함 가능)
     * @return Page<Inquiry> 페이징된 문의 목록
     */
    @Query(value = "select i from Inquiry i " +
            "left join fetch i.user " +
            "order by i.createdAt desc",
            countQuery = "select count(i) from Inquiry i")
    Page<Inquiry> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 💡 [페이징 적용] 일반 사용자 마이페이지용 본인 1:1 문의 내역 페이징 조회
     * * @param userId 조회할 유저 ID
     * @param pageable 페이징 정보
     * @return Page<Inquiry> 페이징된 문의 목록
     */
    @Query(value = "select i from Inquiry i " +
            "left join fetch i.user " +
            "where i.user.id = :userId",
            countQuery = "select count(i) from Inquiry i where i.user.id = :userId")
    Page<Inquiry> findAllByUserId(@Param("userId") Long userId, Pageable pageable);
}