package com.example.petplace

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// ==========================================
// 📦 데이터 그릇들
// ==========================================
data class ReviewData(val id: Int, val rating: Float, val content: String, val imageUri: String?, val isReported: Boolean = false)
data class BusinessData(val id: Int, val bizNum: String, val address: String, val serviceType: String, val isApproved: Boolean = false)

// 🔥 InquiryData에 확인 여부(isChecked) 추가!
data class InquiryData(
    val id: Int,
    val category: String,
    val content: String,
    val email: String,
    val isChecked: Boolean = false // 기본값은 확인 안 함(false)
)

// ==========================================
// 🗄️ 데이터베이스 헬퍼 클래스
// ==========================================
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "petplace_local.db"
        private const val DATABASE_VERSION = 5 // 🔥 버전 5로 업그레이드!

        // 리뷰 테이블
        const val TABLE_REVIEW = "reviews"
        const val COLUMN_REVIEW_ID = "id"
        const val COLUMN_REVIEW_RATING = "rating"
        const val COLUMN_REVIEW_CONTENT = "content"
        const val COLUMN_REVIEW_IMAGE_URI = "image_uri"
        const val COLUMN_REVIEW_IS_REPORTED = "is_reported"

        // 사업자 테이블
        const val TABLE_BIZ = "businesses"
        const val COLUMN_BIZ_ID = "id"
        const val COLUMN_BIZ_NUM = "biz_num"
        const val COLUMN_BIZ_ADDRESS = "address"
        const val COLUMN_BIZ_SERVICE = "service_type"
        const val COLUMN_BIZ_APPROVED = "is_approved"

        // 1:1 문의 테이블
        const val TABLE_INQUIRY = "inquiries"
        const val COLUMN_INQUIRY_ID = "id"
        const val COLUMN_INQUIRY_CATEGORY = "category"
        const val COLUMN_INQUIRY_CONTENT = "content"
        const val COLUMN_INQUIRY_EMAIL = "email"
        const val COLUMN_INQUIRY_IS_CHECKED = "is_checked" // 🔥 확인 여부 컬럼 추가!
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $TABLE_REVIEW ($COLUMN_REVIEW_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_REVIEW_RATING REAL, $COLUMN_REVIEW_CONTENT TEXT, $COLUMN_REVIEW_IMAGE_URI TEXT, $COLUMN_REVIEW_IS_REPORTED INTEGER DEFAULT 0)")
        db.execSQL("CREATE TABLE $TABLE_BIZ ($COLUMN_BIZ_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_BIZ_NUM TEXT, $COLUMN_BIZ_ADDRESS TEXT, $COLUMN_BIZ_SERVICE TEXT, $COLUMN_BIZ_APPROVED INTEGER DEFAULT 0)")

        // 🔥 문의 테이블 생성할 때 is_checked 도 같이 만듦 (0: 미확인, 1: 확인완료)
        db.execSQL("CREATE TABLE $TABLE_INQUIRY ($COLUMN_INQUIRY_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_INQUIRY_CATEGORY TEXT, $COLUMN_INQUIRY_CONTENT TEXT, $COLUMN_INQUIRY_EMAIL TEXT, $COLUMN_INQUIRY_IS_CHECKED INTEGER DEFAULT 0)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_REVIEW")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BIZ")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_INQUIRY")
        onCreate(db)
    }

    // 리뷰 & 사업자 관련 함수들
    fun insertReview(rating: Float, content: String, imageUri: String?): Long { val db = this.writableDatabase; val values = ContentValues().apply { put(COLUMN_REVIEW_RATING, rating); put(COLUMN_REVIEW_CONTENT, content); put(COLUMN_REVIEW_IMAGE_URI, imageUri); put(COLUMN_REVIEW_IS_REPORTED, 0) }; val id = db.insert(TABLE_REVIEW, null, values); db.close(); return id }
    fun getAllReviews(): List<ReviewData> { val list = ArrayList<ReviewData>(); val cursor = this.readableDatabase.rawQuery("SELECT * FROM $TABLE_REVIEW ORDER BY $COLUMN_REVIEW_ID DESC", null); if (cursor.moveToFirst()) do { list.add(ReviewData(cursor.getInt(0), cursor.getFloat(1), cursor.getString(2), cursor.getString(3), cursor.getInt(4) == 1)) } while (cursor.moveToNext()); cursor.close(); return list }
    fun reportReview(id: Int) { this.writableDatabase.apply { update(TABLE_REVIEW, ContentValues().apply { put(COLUMN_REVIEW_IS_REPORTED, 1) }, "$COLUMN_REVIEW_ID = ?", arrayOf(id.toString())); close() } }
    fun getReportedReviews(): List<ReviewData> { val list = ArrayList<ReviewData>(); val cursor = this.readableDatabase.rawQuery("SELECT * FROM $TABLE_REVIEW WHERE $COLUMN_REVIEW_IS_REPORTED = 1 ORDER BY $COLUMN_REVIEW_ID DESC", null); if (cursor.moveToFirst()) do { list.add(ReviewData(cursor.getInt(0), cursor.getFloat(1), cursor.getString(2), cursor.getString(3), true)) } while (cursor.moveToNext()); cursor.close(); return list }
    fun deleteReview(id: Int) { this.writableDatabase.apply { delete(TABLE_REVIEW, "$COLUMN_REVIEW_ID = ?", arrayOf(id.toString())); close() } }
    fun insertBusiness(bizNum: String, address: String, serviceType: String) { this.writableDatabase.apply { insert(TABLE_BIZ, null, ContentValues().apply { put(COLUMN_BIZ_NUM, bizNum); put(COLUMN_BIZ_ADDRESS, address); put(COLUMN_BIZ_SERVICE, serviceType); put(COLUMN_BIZ_APPROVED, 0) }); close() } }
    fun getPendingBusinesses(): List<BusinessData> { val list = ArrayList<BusinessData>(); val cursor = this.readableDatabase.rawQuery("SELECT * FROM $TABLE_BIZ WHERE $COLUMN_BIZ_APPROVED = 0 ORDER BY $COLUMN_BIZ_ID DESC", null); if (cursor.moveToFirst()) do { list.add(BusinessData(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), false)) } while (cursor.moveToNext()); cursor.close(); return list }
    fun approveBusiness(id: Int) { this.writableDatabase.apply { update(TABLE_BIZ, ContentValues().apply { put(COLUMN_BIZ_APPROVED, 1) }, "$COLUMN_BIZ_ID = ?", arrayOf(id.toString())); close() } }

    // ==========================================
    // ✉️ 1:1 문의 관련 기능 함수들
    // ==========================================

    // 임시 문의 데이터 넣기 (테스트용)
    fun insertInquiry(category: String, content: String, email: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_INQUIRY_CATEGORY, category)
            put(COLUMN_INQUIRY_CONTENT, content)
            put(COLUMN_INQUIRY_EMAIL, email)
            put(COLUMN_INQUIRY_IS_CHECKED, 0) // 처음엔 무조건 미확인
        }
        db.insert(TABLE_INQUIRY, null, values)
        db.close()
    }

    // 🌟 문의 읽음 처리 함수 (0에서 1로 업데이트)
    fun checkInquiry(id: Int) {
        val db = this.writableDatabase
        val values = ContentValues().apply { put(COLUMN_INQUIRY_IS_CHECKED, 1) }
        db.update(TABLE_INQUIRY, values, "$COLUMN_INQUIRY_ID = ?", arrayOf(id.toString()))
        db.close()
    }

    // 문의 전체 목록 가져오기 (확인 상태 포함)
    fun getAllInquiries(): List<InquiryData> {
        val list = ArrayList<InquiryData>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_INQUIRY ORDER BY $COLUMN_INQUIRY_ID DESC", null)

        if (cursor.moveToFirst()) {
            do {
                list.add(
                    InquiryData(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_INQUIRY_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INQUIRY_CATEGORY)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INQUIRY_CONTENT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INQUIRY_EMAIL)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_INQUIRY_IS_CHECKED)) == 1 // 1이면 true
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }
}