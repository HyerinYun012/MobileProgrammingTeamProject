package com.example.petplace

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T,
    val timestamp: String
)

data class PageData<T>(
    val content: List<T>,   // 진짜 데이터 리스트가 들어갈 자리!
    val totalPages: Int,
    val totalElements: Int,
    val size: Int,
    val number: Int
)

data class ReviewReportItem(
    val id: Int,
    val reviewId: Int,
    val reviewContent: String,
    val ownerName: String,
    val reason: String,
    val status: String,
    val createdAt: String
)