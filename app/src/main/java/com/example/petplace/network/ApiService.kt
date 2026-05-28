package com.example.petplace.network

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*
import java.io.Serializable

// --- 1. Common Response ---
data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: T?,
    @SerializedName("timestamp") val timestamp: String?
) : Serializable

data class ReportRequest(
    val reason: String
)

data class PageResponse<T>(
    val content: List<T>,
    val totalPages: Int,
    val totalElements: Long,
    val size: Int,
    val number: Int,
    val last: Boolean,
    val first: Boolean,
    val empty: Boolean
) : Serializable

// --- 2. Auth DTOs ---
data class LoginRequest(val loginId: String, val password: String)
data class FindIdRequest(val name: String, val phone: String)
data class ResetPasswordRequest(val loginId: String, val phone: String, val newPassword: String)
data class SocialSignupRequest(
    val provider: String,
    val accessToken: String,
    val providerId: String,
    val nickname: String,
    val phone: String,
    val role: String,
    val marketingAgree: Boolean
)

data class SocialLoginRequest(
    val provider: String,
    val accessToken: String,
    val providerId: String,
)

data class CustomerSignupRequest(
    val name: String,
    val loginId: String,
    val password: String,
    val passwordConfirm: String,
    val nickname: String,
    val phone: String,
    val email: String?
)

data class OwnerSignupRequest(
    val loginId: String,
    val password: String,
    val passwordConfirm: String,
    val name: String,
    val nickname: String,
    val phone: String,
    val email: String?
)

// --- 3. User DTOs ---
data class User(
    val id: Long,
    val nickname: String,
    val name: String,
    val email: String?,
    val phone: String,
    val role: String,
    val profileUrl: String?,
    val marketingAgree: Boolean,
    val verified: Boolean,
    val createdAt: String?,
    val updatedAt: String?
) : Serializable

data class UserProfileResponse(
    val id: Long,
    val email: String?,
    val nickname: String,
    val profileImageUrl: String?,
    val role: String
) : Serializable

// --- 4. Restaurant DTOs ---
data class MenuRequest(
    val name: String,
    val price: Int,
    val description: String?
) : Serializable

data class OperatingHourRequest(
    val dayOfWeek: String,
    val openTime: String?,
    val closeTime: String?,
    @SerializedName("isRegularHoliday") val regularHoliday: Boolean
) : Serializable

data class RestaurantRequest(
    val name: String,
    val address: String,
    val phone: String,
    val businessNo: String,
    val category: String,
    val region: String?,
    val latitude: Double?,
    val longitude: Double?,
    val hasFence: Boolean = false,
    val hasArtificialGrass: Boolean = false,
    val hasNaturalGrass: Boolean = false,
    val hasSnack: Boolean = false,
    val hasParking: Boolean = false,
    val hasRestroom: Boolean = false,
    val hasIndoor: Boolean = false,
    val hasOutdoor: Boolean = false,
    val allowSmall: Boolean = true,
    val allowMedium: Boolean = false,
    val allowLarge: Boolean = false,
    val menus: List<MenuRequest> = emptyList(),
    val operatingHours: List<OperatingHourRequest> = emptyList()
) : Serializable

data class OperatingHour(val dayOfWeek: String, val openTime: String, val closeTime: String, val regularHoliday: Boolean) : Serializable
data class RestaurantImage(val id: Long, val imageUrl: String, val sortOrder: Int) : Serializable

data class RestaurantResponse(
    val id: Long,
    val name: String,
    val category: String,
    val region: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val phone: String,
    val operatingHours: List<OperatingHour>?,
    val allowSmall: Boolean,
    val allowMedium: Boolean,
    val allowLarge: Boolean,
    val hasFence: Boolean,
    val hasArtificialGrass: Boolean,
    val hasNaturalGrass: Boolean,
    val hasSnack: Boolean,
    val hasParking: Boolean,
    val hasRestroom: Boolean,
    val hasIndoor: Boolean,
    val hasOutdoor: Boolean,
    val imageUrl: String?,
    val verified: Boolean,
    val bookmarked: Boolean
) : Serializable

data class RestaurantDetailResponse(
    val id: Long,
    val owner: User?,
    val name: String,
    val category: String,
    val region: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val phone: String,
    val businessNo: String?,
    val operatingHours: List<OperatingHour>?,
    val allowSmall: Boolean,
    val allowMedium: Boolean,
    val allowLarge: Boolean,
    val hasFence: Boolean,
    val hasArtificialGrass: Boolean,
    val hasNaturalGrass: Boolean,
    val hasSnack: Boolean,
    val hasParking: Boolean,
    val hasRestroom: Boolean,
    val hasIndoor: Boolean,
    val hasOutdoor: Boolean,
    val images: List<RestaurantImage>?,
    val reviews: List<ReviewResponse>?,
    val menus: List<MenuResponse>?,
    val verified: Boolean,
    val imageUrl: String?,
    val bookmarked: Boolean = false
) : Serializable

data class MenuResponse(val id: Long, val name: String, val price: Int, val description: String?, val imageUrl: String?) : Serializable

// --- 5. Review / Notice / Pet / Community ---
data class ReviewResponse(
    val id: Long,
    val rating: Int,
    val content: String,
    val imageUrl: String?,
    val writerName: String?,
    val user: User?,
    val createdAt: String
) : Serializable

data class MyReviewResponse(
    val reviewId: Long,
    val restaurantId: Long,
    val restaurantName: String,
    val content: String,
    val rating: Int,
    val createdAt: String,
    val imageUrl: String?
) : Serializable

data class NoticeResponse(val id: Long, val title: String, val content: String, val createdAt: String, val imageUrl: String?, val thumbnailUrl: String?) : Serializable
data class Pet(val id: Long, val name: String, val breed: String, val birth: String, val imageUrl: String?, val user: User?) : Serializable
data class PetRequest(val name: String, val breed: String, val birth: String) : Serializable

data class CommentResponse(
    val id: Long,
    val userId: Long,
    val nickname: String,
    val profileUrl: String?,
    val content: String,
    val createdAt: String,
    val children: List<CommentResponse>?,
    val mine: Boolean
) : Serializable

data class PostResponse(
    val id: Long,
    val user: User?,
    val title: String,
    val content: String,
    val imageUrl: String?,
    val createdAt: String,
    val comments: List<CommentResponse>?
) : Serializable

data class PostDetailResponse(
    val id: Long,
    val userId: Long,
    val title: String,
    val content: String,
    val imageUrl: String?,
    val writerNickname: String,
    val writerProfileUrl: String?,
    val createdAt: String,
    val comments: List<CommentResponse>?
) : Serializable

// --- 6. MyPage & Inquiry DTOs ---
data class BookmarkResponse(val restaurantId: Long, val restaurantName: String, val category: String, val address: String, val imageUrl: String?) : Serializable
data class RecentViewResponse(val restaurantId: Long, val restaurantName: String, val category: String, val imageUrl: String?, val createdAt: String) : Serializable
data class InquiryRequest(val category: String, val email: String, val content: String, val imageUrl: String? = null)
data class InquiryResponse(val id: Long, val userName: String,  val email: String,val category: String, val content: String, val status: String, val createdAt: String) : Serializable

// --- 7. Admin DTOs ---
data class ReviewReportItem(
    val id: Long,
    val reviewId: Long,
    val reviewContent: String,
    val ownerName: String,
    val reason: String,
    val status: String,
    val createdAt: String
) : Serializable

data class BusinessInfo(
    val restaurantId: Long,
    val storeName: String,
    val businessNo: String,
    val category: String,
    val address: String
) : Serializable

data class OwnerBusinessInfoResponse(
    val ownerId: Long,
    val ownerName: String,
    val phone: String,
    val email: String?,
    val businesses: List<BusinessInfo>,
    val verified: Boolean
) : Serializable

data class PendingOwnerItem(
    val ownerId: Long,
    val loginId: String,
    val name: String,
    val email: String?,
    val phone: String,
    val createdAt: String
) : Serializable

// --- 8. API Interface ---
interface ApiService {
    @GET("api/search/search") fun search(@Query("keyword") keyword: String, @QueryMap pageable: Map<String, String>): Call<ApiResponse<PageResponse<RestaurantResponse>>>
    @GET("api/search/recent") fun getRecentSearches(): Call<ApiResponse<List<String>>>
    @DELETE("api/search/recent/{keyword}") fun deleteRecentSearch(@Path("keyword") keyword: String): Call<ApiResponse<Any>>
    @GET("api/search/popular") fun getPopularKeywords(): Call<ApiResponse<List<String>>>
    @POST("api/auth/login") fun login(@Body request: LoginRequest): Call<ApiResponse<String>>
    @POST("api/auth/social/signup") fun socialSignup(@Body request: SocialSignupRequest): Call<ApiResponse<Any>>
    @POST("api/auth/social/login") fun socialLogin(@Body request: SocialLoginRequest): Call<ApiResponse<String>>
    @POST("api/auth/signup/customer") fun signupCustomer(@Body request: CustomerSignupRequest): Call<ApiResponse<Any>>
    @POST("api/auth/signup/owner") fun signupOwner(@Body request: OwnerSignupRequest): Call<ApiResponse<Any>>
    @POST("api/auth/find-id") fun findId(@Body request: FindIdRequest): Call<ApiResponse<String>>
    @POST("api/auth/reset-password") fun resetPassword(@Body request: ResetPasswordRequest): Call<ApiResponse<Any>>
    @GET("api/auth/check-id") fun checkId(@Query("loginId") loginId: String): Call<ApiResponse<Boolean>>
    @GET("api/auth/check-nickname") fun checkNickname(@Query("nickname") nickname: String): Call<ApiResponse<Boolean>>

    @GET("api/community/posts") fun getCommunityPosts(@QueryMap pageable: Map<String, String>): Call<ApiResponse<PageResponse<PostResponse>>>
    @GET("api/community/posts/{postId}") fun getPostDetail(@Path("postId") postId: Long): Call<ApiResponse<PostDetailResponse>>
    @Multipart @POST("api/community/posts") fun writePost(@Query("title") title: String, @Query("content") content: String, @Part image: MultipartBody.Part?): Call<ApiResponse<Any>>
    @Multipart @PUT("api/community/posts/{postId}") fun updatePost(@Path("postId") postId: Long, @Query("title") title: String, @Query("content") content: String, @Part image: MultipartBody.Part?): Call<ApiResponse<Any>>
    @DELETE("api/community/posts/{postId}") fun deletePost(@Path("postId") postId: Long): Call<ApiResponse<Any>>
    @POST("api/community/posts/{postId}/report") fun reportPost(@Path("postId") postId: Long, @Query("reason") reason: String): Call<ApiResponse<Any>>
    @GET("api/community/posts/{postId}/comments") fun getPostComments(@Path("postId") postId: Long, @QueryMap pageable: Map<String, String>): Call<ApiResponse<PageResponse<CommentResponse>>>
    @POST("api/community/posts/{postId}/comments") fun writeComment(@Path("postId") postId: Long, @Query("content") content: String, @Query("parentId") parentId: Long? = null): Call<ApiResponse<Any>>
    @PUT("api/community/comments/{commentId}") fun updateComment(@Path("commentId") commentId: Long, @Query("content") content: String): Call<ApiResponse<Any>>
    @DELETE("api/community/comments/{commentId}") fun deleteComment(@Path("commentId") commentId: Long): Call<ApiResponse<Any>>
    @POST("api/community/comments/{commentId}/report") fun reportComment(@Path("commentId") commentId: Long, @Query("reason") reason: String): Call<ApiResponse<Any>>

    @GET("api/my/profile") fun getProfile(): Call<ApiResponse<UserProfileResponse>>
    @Multipart @PUT("api/my/profile") fun updateProfile(@Part("nickname") nickname: RequestBody, @Part("email") email: RequestBody, @Part("phone") phone: RequestBody, @Part profileImage: MultipartBody.Part?): Call<ApiResponse<Any>>
    @GET("api/my/bookmarks") fun getBookmarks(@QueryMap pageable: Map<String, String>): Call<ApiResponse<PageResponse<BookmarkResponse>>>
    @POST("api/my/bookmarks/{restaurantId}") fun toggleBookmark(@Path("restaurantId") restaurantId: Long): Call<ApiResponse<Boolean>>
    @GET("api/my/recent") fun getRecentViews(@QueryMap pageable: Map<String, String>): Call<ApiResponse<PageResponse<RecentViewResponse>>>
    @POST("api/my/recent/{restaurantId}") fun addRecentView(@Path("restaurantId") restaurantId: Long): Call<ApiResponse<Any>>
    @GET("api/my/reviews") fun getMyReviews(@QueryMap pageable: Map<String, String>): Call<ApiResponse<PageResponse<MyReviewResponse>>>

    @GET("api/my/pets") fun getMyPets(@QueryMap pageable: Map<String, String>): Call<ApiResponse<PageResponse<Pet>>>
    @Multipart @POST("api/my/pets") fun addPet(@Part("request") request: RequestBody, @Part image: MultipartBody.Part?): Call<ApiResponse<Pet>>
    @Multipart @PUT("api/my/pets/{petId}") fun updatePet(@Path("petId") petId: Long, @Part("request") request: RequestBody, @Part image: MultipartBody.Part?): Call<ApiResponse<Pet>>

    @GET("api/restaurants/{id}") fun getRestaurantDetail(@Path("id") id: Long): Call<ApiResponse<RestaurantDetailResponse>>
    @Multipart @POST("api/restaurants") fun registerRestaurant(@Part("request") request: RequestBody, @Part images: List<MultipartBody.Part>?): Call<ApiResponse<Long>>
    @Multipart @PUT("api/restaurants/{id}") fun updateRestaurant(@Path("id") id: Long, @Part("request") request: RequestBody, @Part images: List<MultipartBody.Part>?): Call<ApiResponse<Long>>
    @GET("api/restaurants/nearby") fun getNearbyRestaurants(@Query("lat") lat: Double, @Query("lng") lng: Double, @Query("radius") radius: Double = 3.0, @QueryMap pageable: Map<String, String>): Call<ApiResponse<PageResponse<RestaurantResponse>>>
    @GET("api/restaurants/filter") fun filterRestaurants(@QueryMap condition: Map<String, @JvmSuppressWildcards Any>, @QueryMap pageable: Map<String, String>): Call<ApiResponse<PageResponse<RestaurantResponse>>>

    @GET("api/restaurants/{restaurantId}/notices") fun getNotices(@Path("restaurantId") restaurantId: Long, @QueryMap pageable: Map<String, String>): Call<ApiResponse<PageResponse<NoticeResponse>>>
    @Multipart @POST("api/restaurants/{restaurantId}/notices") fun registerNotice(@Path("restaurantId") restaurantId: Long, @Query("title") title: String, @Query("content") content: String, @Part thumbnail: MultipartBody.Part?, @Part descriptionImage: MultipartBody.Part?): Call<ApiResponse<Any>>
    @DELETE("api/restaurants/notices/{noticeId}") fun deleteNotice(@Path("noticeId") noticeId: Long): Call<ApiResponse<Any>>

    @GET("api/restaurants/{restaurantId}/menus") fun getMenus(@Path("restaurantId") restaurantId: Long, @QueryMap pageable: Map<String, String>): Call<ApiResponse<PageResponse<MenuResponse>>>
    @Multipart @POST("api/restaurants/{restaurantId}/menus") fun registerMenu(@Path("restaurantId") restaurantId: Long, @Part("name") name: RequestBody, @Part("price") price: RequestBody, @Part("description") description: RequestBody, @Part imageFile: MultipartBody.Part?): Call<ApiResponse<Long>>
    @DELETE("api/restaurants/{restaurantId}/menus/{menuId}") fun deleteMenu(@Path("restaurantId") restaurantId: Long, @Path("menuId") menuId: Long): Call<ApiResponse<Any>>

    @GET("api/reviews/restaurant/{restaurantId}") fun getReviews(@Path("restaurantId") restaurantId: Long, @QueryMap pageable: Map<String, String>): Call<ApiResponse<PageResponse<ReviewResponse>>>
    @Multipart @POST("api/reviews/restaurant/{restaurantId}") fun writeReview(@Path("restaurantId") restaurantId: Long, @Part("data") data: okhttp3.RequestBody,@Part imageFile: MultipartBody.Part?): Call<ApiResponse<Any>>

    @POST("api/reviews/{reviewId}/report") fun reportReview(@Path("reviewId") reviewId: Long, @Body reason: Map<String, String>): Call<ApiResponse<Any>>
    @DELETE("api/reviews/{reviewId}") fun deleteReview(@Path("reviewId") reviewId: Long): Call<ApiResponse<Any>>

    @POST("api/inquiries") fun submitInquiry(@Body request: InquiryRequest): Call<ApiResponse<Any>>
    @GET("api/inquiries/my") fun getMyInquiries(@QueryMap pageable: Map<String, String>): Call<ApiResponse<PageResponse<InquiryResponse>>>

    @PATCH("api/admin/reports/{reportId}/complete") fun completeReviewReport(@Path("reportId") reportId: Long): Call<ApiResponse<Any>>
    @PATCH("api/admin/owners/{ownerId}/verify") fun verifyOwner(@Path("ownerId") ownerId: Long): Call<ApiResponse<Any>>
    @PATCH("api/admin/inquiries/{inquiryId}/complete") fun completeInquiry(@Path("inquiryId") inquiryId: Long): Call<ApiResponse<Any>>
    @PATCH("api/admin/community/reports/{reportId}/complete") fun completeCommunityReport(@Path("reportId") reportId: Long): Call<ApiResponse<Any>>
    @GET("api/admin/reports/reviews") fun getReviewReports(@Query("page") page: Int, @Query("size") size: Int): Call<ApiResponse<PageResponse<ReviewReportItem>>>
    @GET("api/admin/reports/community") fun getCommunityReports(): Call<ApiResponse<List<Any>>>
    @GET("api/admin/owners/{ownerId}/business-info") fun getOwnerBusinessInfo(@Path("ownerId") ownerId: Long): Call<ApiResponse<OwnerBusinessInfoResponse>>
    @GET("api/admin/owners/pending") fun getPendingOwners(@QueryMap pageable: Map<String, String>): Call<ApiResponse<PageResponse<PendingOwnerItem>>>
    @GET("api/admin/inquiries") fun getAdminInquiries(@QueryMap pageable: Map<String, String>): Call<ApiResponse<PageResponse<InquiryResponse>>>
    @DELETE("api/admin/reports/reviews/{reviewId}") fun adminDeleteReview(@Path("reviewId") reviewId: Long): Call<ApiResponse<Any>>
    @DELETE("api/admin/community/posts/{postId}") fun adminDeleteCommunityPost(@Path("postId") postId: Long): Call<ApiResponse<Any>>
    @DELETE("api/admin/community/comments/{commentId}") fun adminDeleteCommunityComment(@Path("commentId") commentId: Long): Call<ApiResponse<Any>>

    @GET("api/owner/inquiries") fun getGeneralInquiries(@QueryMap pageable: Map<String, String>): Call<ApiResponse<PageResponse<InquiryResponse>>>
    @POST("/api/reviews/{reviewId}/report") fun reportReview(@Header("Authorization") token: String, @Path("reviewId") reviewId: Long, @Body request: ReportRequest): Call<ApiResponse<Any>>
}
