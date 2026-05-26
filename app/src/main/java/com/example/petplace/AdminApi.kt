package com.example.petplace

import retrofit2.Call
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface AdminApi {

    // 1. 리뷰 신고 반려/단순 완료
    @PATCH("/api/admin/reports/{reportId}/complete")
    fun completeReviewReport(
        @Path("reportId") reportId: Int
    ): Call<Any>

    // 2. 사장님 입점 승인
    @PATCH("/api/admin/owners/{ownerId}/verify")
    fun verifyOwner(
        @Path("ownerId") ownerId: Int
    ): Call<Any>

    // 3. 1:1 문의 처리 완료
    @PATCH("/api/admin/inquiries/{inquiryId}/complete")
    fun completeInquiry(
        @Path("inquiryId") inquiryId: Int
    ): Call<Any>

    // 4. 커뮤니티 신고 반려/단순 완료
    @PATCH("/api/admin/community/reports/{reportId}/complete")
    fun completeCommunityReport(
        @Path("reportId") reportId: Int
    ): Call<Any>

    // 5. 리뷰 신고 내역 전체 조회 (페이징 + 3중 포장지 적용 완료!)
    @GET("/api/admin/reports/reviews")
    fun getReviewReports(
        @Query("page") page: Int = 0, // 기본값 0페이지 (첫 페이지)
        @Query("size") size: Int = 10 // 기본값 10개씩
    ): Call<ApiResponse<PageData<ReviewReportItem>>>

    // 6. 커뮤니티 신고 내역 목록 조회
    @GET("/api/admin/reports/community")
    fun getCommunityReports(): Call<List<Any>> // ⚠️ 나중에 Any 대신 진짜 데이터 그릇 이름으로 변경!

    // 7. 1:1 문의 내역 전체 조회
    @GET("/api/admin/inquiries")
    fun getInquiries(): Call<List<Any>> // ⚠️ 나중에 Any 대신 진짜 데이터 그릇 이름으로 변경!

    // 8. 신고된 리뷰 삭제
    @DELETE("/api/admin/reports/reviews/{reviewId}")
    fun deleteReview(
        @Path("reviewId") reviewId: Int
    ): Call<Any>

    // 9. 신고된 커뮤니티 게시글 강제 삭제
    @DELETE("/api/admin/community/posts/{postId}")
    fun deleteCommunityPost(
        @Path("postId") postId: Int
    ): Call<Any>

    // 10. 신고된 커뮤니티 댓글 강제 삭제
    @DELETE("/api/admin/community/comments/{commentId}")
    fun deleteCommunityComment(
        @Path("commentId") commentId: Int
    ): Call<Any>
}