package com.petplace.dto.response;

import com.petplace.entity.Restaurant;
import com.petplace.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@Schema(description = "사장님 입점 승인을 위한 통합 사업자 정보 응답")
public class OwnerBusinessInfoResponse {

    @Schema(description = "사장님(유저) ID", example = "1")
    private Long ownerId;

    @Schema(description = "사장님 실명", example = "김사장")
    private String ownerName;

    @Schema(description = "사장님 연락처", example = "010-1234-5678")
    private String phone;

    @Schema(description = "사장님 이메일", example = "owner@petplace.com")
    private String email;

    @Schema(description = "사장님 승인 완료 여부", example = "false")
    private boolean isVerified;

    @Schema(description = "해당 사장님이 등록한 사업장(식당/카페) 목록")
    private List<BusinessDetail> businesses;

    @Getter
    @Builder
    @Schema(description = "개별 사업장 요약 정보")
    public static class BusinessDetail {
        @Schema(description = "장소 ID", example = "10")
        private Long restaurantId;

        @Schema(description = "상호명", example = "멍멍 플레이그라운드 카페")
        private String storeName;

        @Schema(description = "사업자 등록 번호", example = "123-45-67890")
        private String businessNo;

        @Schema(description = "카테고리", example = "카페")
        private String category;

        @Schema(description = "전체 도로명 주소", example = "경기도 시흥시 대야동 123-4")
        private String address;
    }

    public static OwnerBusinessInfoResponse of(User owner, List<Restaurant> restaurants) {
        List<BusinessDetail> businessDetails = restaurants.stream()
                .map(r -> BusinessDetail.builder()
                        .restaurantId(r.getId())
                        .storeName(r.getName())
                        .businessNo(r.getBusinessNo())
                        .category(r.getCategory() != null ? r.getCategory().getDescription() : null)
                        .address(r.getAddress())
                        .build())
                .collect(Collectors.toList());

        return OwnerBusinessInfoResponse.builder()
                .ownerId(owner.getId())
                // name이 없으면 nickname 제공
                .ownerName(owner.getName() != null ? owner.getName() : owner.getNickname())
                .phone(owner.getPhone())
                .email(owner.getEmail())
                .isVerified(owner.isVerified())
                .businesses(businessDetails)
                .build();
    }
}