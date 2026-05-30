package com.petplace.repository;

import com.petplace.entity.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.Optional;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    /**
     * 특정 카테고리들에 해당하는 문의 내역 페이징 조회 (관리자용)
     */
    @Query(value = "select i from Inquiry i " +
            "left join fetch i.user " +
            "left join fetch i.restaurant " + // 💡 [N+1 방지 추가]
            "where i.category in :categories " +
            "order by i.createdAt desc",
            countQuery = "select count(i) from Inquiry i where i.category in :categories")
    Page<Inquiry> findAllByCategoryIn(@Param("categories") Collection<Inquiry.Category> categories, Pageable pageable);

    /**
     * 일반 사용자 마이페이지용 본인 1:1 문의 내역 페이징 조회
     */
    @Query(value = "select i from Inquiry i " +
            "left join fetch i.user " +
            "left join fetch i.restaurant " + // 💡 [N+1 방지 추가]
            "where i.user.id = :userId",
            countQuery = "select count(i) from Inquiry i where i.user.id = :userId")
    Page<Inquiry> findAllByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 사장님 본인의 식당에 접수된 특정 카테고리(GENERAL) 문의 내역만 페이징 조회
     */
    @Query(value = "select i from Inquiry i " +
            "left join fetch i.user " +
            "left join fetch i.restaurant r " +
            "where i.category = :category " +
            "and r.owner.id = :ownerId " +
            "order by i.createdAt desc",
            countQuery = "select count(i) from Inquiry i " +
                    "join i.restaurant r " +
                    "where i.category = :category " +
                    "and r.owner.id = :ownerId")
    Page<Inquiry> findAllByCategoryAndRestaurantOwnerId(
            @Param("category") Inquiry.Category category,
            @Param("ownerId") Long ownerId,
            Pageable pageable
    );

    /**
     * 단건 상세 조회 성능 최적화를 위한 Fetch Join 쿼리
     * Inquiry 상세 응답 DTO 변환 및 사장님 권한 검증에 필요한 연관 엔티티를 한 번에 가져옵니다.
     */
    @Query("select i from Inquiry i " +
            "left join fetch i.user " +
            "left join fetch i.restaurant r " +
            "left join fetch r.owner " +
            "where i.id = :id")
    Optional<Inquiry> findByIdWithUserAndRestaurant(@Param("id") Long id);

    /** 식당 삭제 시 해당 식당 참조를 null로 처리 (문의 내역 자체는 보존) */
    @Modifying
    @Query("UPDATE Inquiry i SET i.restaurant = null WHERE i.restaurant.id = :restaurantId")
    void clearRestaurantReference(@Param("restaurantId") Long restaurantId);
}