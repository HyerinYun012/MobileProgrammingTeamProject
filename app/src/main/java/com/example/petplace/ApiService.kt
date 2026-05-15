package com.example.petplace

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.*

// Request body 채울 내용들
data class SignupCustomerRequest(
    @SerializedName("name") val name: String,
    @SerializedName("loginId") val loginId: String,
    @SerializedName("password") val password: String,
    @SerializedName("passwordCheck") val passwordCheck: String,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("phone") val phone: String
)
data class SignupOwnerRequest(
    @SerializedName("name") val name: String,
    @SerializedName("loginId") val loginId: String,
    @SerializedName("password") val password: String,
    @SerializedName("passwordCheck") val passwordCheck: String,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("businessNo") val businessNo: String,
    @SerializedName("businessAddress") val businessAddress: String,
    @SerializedName("marketingAgree") val marketingAgree: Boolean
)
data class LoginRequest(
    @SerializedName("loginId") val loginId:String,
    @SerializedName("password") val password:String
)
data class SearchRequest(
    @SerializedName("keyword") val keyword: String,
    @SerializedName("userId") val userId: String
)
data class SearchRecentRequest(
    @SerializedName("userId") val userId: String
)
class SearchRecommendRequest

data class KakaoLoginRequest(
    @SerializedName("additionalProp1") val additionalProp1: String,
    @SerializedName("additionalProp2") val additionalProp2: String,
    @SerializedName("additionalProp3") val additionalProp3: String
)


// RESPONESE ///////////////////////////////////////////////////////////
data class SignUpCustomerResponse(
    @SerializedName("additionalProp1") val additionalProp1: Unit
)
data class SignUpOwnerResponse(
    @SerializedName("additionalProp1") val additionalProp1: Unit
)
data class LoginResponse(
    @SerializedName("additionalProp1") val additionalProp1: Unit
)
data class KakaoLoginResponse(
    @SerializedName("additionalProp1") val additionalProp1: Unit
)
data class SearchResponse(
    @SerializedName("additionalProp1") val additionalProp1:Unit
)
data class SearchRecentResponse(
    @SerializedName("additionalProp1") val additionalProp1:Unit
)
data class SearchRecommendResponse(
    @SerializedName("additionalProp1") val additionalProp1:Unit
)
////////////////////////////////////////////////////////////////////////
interface ApiService {
    @POST("api/auth/signup/customer")
    fun signupCustomer(@Body signupRequest: SignupCustomerRequest): Call<SignUpCustomerResponse>

    @POST("api/auth/signup/owner")
    fun signupOwner(@Body signupRequest: SignupOwnerRequest): Call<SignUpOwnerResponse>

    @POST("api/auth/login")
    fun login(@Body loginRequest: LoginRequest): Call<LoginResponse>

    @POST("api/auth/social/kakao")
    fun kakaoLogin(@Body kakaoLoginRequest: KakaoLoginRequest): Call<KakaoLoginResponse>

    @GET("api/search")
    fun search(
        @Query("keyword") keyword: String,
        @Query("userId") userId: String
    ): Call<List<SearchResponse>>

    @GET("api/search/recent")
    fun searchRecent(@Query("userId") userId: String): Call<List<SearchRecentResponse>>
}
