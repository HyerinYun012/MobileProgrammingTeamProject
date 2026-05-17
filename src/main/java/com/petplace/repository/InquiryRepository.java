package com.petplace.repository;

import com.petplace.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    /**
     * 💡 [기존] 관리자 대시보드용 전체 1:1 문의 내역 최신순 조회
     * Fetch Join을 사용하여 Inquiry 엔티티와 연관된 User 엔티티를 한 번에 영속성 컨텍스트로 끌고 옵니다.
     */
    @Query("select i from Inquiry i " +
            "left join fetch i.user " +
            "order by i.createdAt desc")
    List<Inquiry> findAllByOrderByCreatedAtDesc();

    /**
     * 💡 [신규 추가 및 성능 최적화] 일반 사용자 마이페이지용 본인 1:1 문의 내역 최신순 조회
     * 특정 유저(userId)가 작성한 문의 목록만 필터링하되, 데이터 매핑 시 N+1 쿼리가 발생하는 것을
     * 방지하기 위해 left join fetch 문법을 적용하여 연관된 User 객체를 단 하나의 쿼리로 함께 묶어 가져옵니다.
     */
    @Query("select i from Inquiry i " +
            "left join fetch i.user " +
            "where i.user.id = :userId " +
            "order by i.createdAt desc")
    List<Inquiry> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
}