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
data class InquiryData(val id: Int, val category: String, val content: String, val email: String, val isChecked: Boolean = false)

data class NoticeData(
    var id: Int = 0,
    var storeId: Int = 1,
    var title: String = "",
    var content: String = "",
    var bannerUri: String = "", // 목록용 가로 이미지
    var originalUri: String = "" // 본문용 원본(혹은 자유 비율) 이미지
)

// 🔥 반려동물 등록용 데이터 클래스
data class PetData(
    var id: Int = 0,
    var userId: Int = 1, // 고객 고유 번호 (임시: 1)
    var name: String = "",
    var birthday: String = "",
    var breed: String = "",
    var imageUri: String = ""
)

// 🔥 사장님 메뉴 관리를 위한 새로운 데이터 클래스 추가
data class MenuData(
    var id: Int = 0, // DB에서 관리되는 고유 식별자 (새 데이터는 0)
    var storeId: Int = 1, // 사업장(가게) 고유 번호 (임시 할당값: 1)
    var name: String = "",
    var price: String = "",
    var desc: String = "",
    var imageUri: String = ""
)

// ==========================================
// 🗄️ 데이터베이스 헬퍼 클래스
// ==========================================
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "petplace_local.db"
        // 🔥 메뉴 테이블 추가를 위해 버전을 6으로 업그레이드하여 자동 초기화 유도
        private const val DATABASE_VERSION = 8 // 🔥 8로 업그레이드

        const val TABLE_PET = "pets"
        const val COLUMN_PET_ID = "id"
        const val COLUMN_PET_USER_ID = "user_id"
        const val COLUMN_PET_NAME = "name"
        const val COLUMN_PET_BIRTHDAY = "birthday"
        const val COLUMN_PET_BREED = "breed"
        const val COLUMN_PET_IMAGE_URI = "image_uri"

        const val TABLE_NOTICE = "notices"
        const val COLUMN_NOTICE_ID = "id"
        const val COLUMN_NOTICE_STORE_ID = "store_id"
        const val COLUMN_NOTICE_TITLE = "title"
        const val COLUMN_NOTICE_CONTENT = "content"
        const val COLUMN_NOTICE_BANNER_URI = "banner_uri"
        const val COLUMN_NOTICE_ORIGINAL_URI = "original_uri"

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
        const val COLUMN_INQUIRY_IS_CHECKED = "is_checked"

        // 🔥 메뉴 관리 테이블 (새로 추가됨)
        const val TABLE_MENU = "menus"
        const val COLUMN_MENU_ID = "id"
        const val COLUMN_MENU_STORE_ID = "store_id"
        const val COLUMN_MENU_NAME = "name"
        const val COLUMN_MENU_PRICE = "price"
        const val COLUMN_MENU_DESC = "description"
        const val COLUMN_MENU_IMAGE_URI = "image_uri"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $TABLE_REVIEW ($COLUMN_REVIEW_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_REVIEW_RATING REAL, $COLUMN_REVIEW_CONTENT TEXT, $COLUMN_REVIEW_IMAGE_URI TEXT, $COLUMN_REVIEW_IS_REPORTED INTEGER DEFAULT 0)")
        db.execSQL("CREATE TABLE $TABLE_BIZ ($COLUMN_BIZ_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_BIZ_NUM TEXT, $COLUMN_BIZ_ADDRESS TEXT, $COLUMN_BIZ_SERVICE TEXT, $COLUMN_BIZ_APPROVED INTEGER DEFAULT 0)")
        db.execSQL("CREATE TABLE $TABLE_INQUIRY ($COLUMN_INQUIRY_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_INQUIRY_CATEGORY TEXT, $COLUMN_INQUIRY_CONTENT TEXT, $COLUMN_INQUIRY_EMAIL TEXT, $COLUMN_INQUIRY_IS_CHECKED INTEGER DEFAULT 0)")
        db.execSQL("CREATE TABLE $TABLE_NOTICE ($COLUMN_NOTICE_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_NOTICE_STORE_ID INTEGER, $COLUMN_NOTICE_TITLE TEXT, $COLUMN_NOTICE_CONTENT TEXT, $COLUMN_NOTICE_BANNER_URI TEXT, $COLUMN_NOTICE_ORIGINAL_URI TEXT)")
        db.execSQL("CREATE TABLE $TABLE_PET ($COLUMN_PET_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_PET_USER_ID INTEGER, $COLUMN_PET_NAME TEXT, $COLUMN_PET_BIRTHDAY TEXT, $COLUMN_PET_BREED TEXT, $COLUMN_PET_IMAGE_URI TEXT)")

        // 메뉴 테이블 스키마 생성 쿼리 실행
        db.execSQL("CREATE TABLE $TABLE_MENU ($COLUMN_MENU_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_MENU_STORE_ID INTEGER, $COLUMN_MENU_NAME TEXT, $COLUMN_MENU_PRICE TEXT, $COLUMN_MENU_DESC TEXT, $COLUMN_MENU_IMAGE_URI TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_REVIEW")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BIZ")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_INQUIRY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MENU")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTICE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PET")
        onCreate(db)
    }


    // [기존 함수들: 리뷰, 사업자, 1:1 문의 기능 생략 없이 유지]
    fun insertReview(rating: Float, content: String, imageUri: String?): Long { val db = this.writableDatabase; val values = ContentValues().apply { put(COLUMN_REVIEW_RATING, rating); put(COLUMN_REVIEW_CONTENT, content); put(COLUMN_REVIEW_IMAGE_URI, imageUri); put(COLUMN_REVIEW_IS_REPORTED, 0) }; val id = db.insert(TABLE_REVIEW, null, values); db.close(); return id }
    fun getAllReviews(): List<ReviewData> { val list = ArrayList<ReviewData>(); val cursor = this.readableDatabase.rawQuery("SELECT * FROM $TABLE_REVIEW ORDER BY $COLUMN_REVIEW_ID DESC", null); if (cursor.moveToFirst()) do { list.add(ReviewData(cursor.getInt(0), cursor.getFloat(1), cursor.getString(2), cursor.getString(3), cursor.getInt(4) == 1)) } while (cursor.moveToNext()); cursor.close(); return list }
    fun reportReview(id: Int) { this.writableDatabase.apply { update(TABLE_REVIEW, ContentValues().apply { put(COLUMN_REVIEW_IS_REPORTED, 1) }, "$COLUMN_REVIEW_ID = ?", arrayOf(id.toString())); close() } }
    fun getReportedReviews(): List<ReviewData> { val list = ArrayList<ReviewData>(); val cursor = this.readableDatabase.rawQuery("SELECT * FROM $TABLE_REVIEW WHERE $COLUMN_REVIEW_IS_REPORTED = 1 ORDER BY $COLUMN_REVIEW_ID DESC", null); if (cursor.moveToFirst()) do { list.add(ReviewData(cursor.getInt(0), cursor.getFloat(1), cursor.getString(2), cursor.getString(3), true)) } while (cursor.moveToNext()); cursor.close(); return list }
    fun deleteReview(id: Int) { this.writableDatabase.apply { delete(TABLE_REVIEW, "$COLUMN_REVIEW_ID = ?", arrayOf(id.toString())); close() } }
    fun insertBusiness(bizNum: String, address: String, serviceType: String) { this.writableDatabase.apply { insert(TABLE_BIZ, null, ContentValues().apply { put(COLUMN_BIZ_NUM, bizNum); put(COLUMN_BIZ_ADDRESS, address); put(COLUMN_BIZ_SERVICE, serviceType); put(COLUMN_BIZ_APPROVED, 0) }); close() } }
    fun getPendingBusinesses(): List<BusinessData> { val list = ArrayList<BusinessData>(); val cursor = this.readableDatabase.rawQuery("SELECT * FROM $TABLE_BIZ WHERE $COLUMN_BIZ_APPROVED = 0 ORDER BY $COLUMN_BIZ_ID DESC", null); if (cursor.moveToFirst()) do { list.add(BusinessData(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), false)) } while (cursor.moveToNext()); cursor.close(); return list }
    fun approveBusiness(id: Int) { this.writableDatabase.apply { update(TABLE_BIZ, ContentValues().apply { put(COLUMN_BIZ_APPROVED, 1) }, "$COLUMN_BIZ_ID = ?", arrayOf(id.toString())); close() } }
    fun insertInquiry(category: String, content: String, email: String) { val db = this.writableDatabase; val values = ContentValues().apply { put(COLUMN_INQUIRY_CATEGORY, category); put(COLUMN_INQUIRY_CONTENT, content); put(COLUMN_INQUIRY_EMAIL, email); put(COLUMN_INQUIRY_IS_CHECKED, 0) }; db.insert(TABLE_INQUIRY, null, values); db.close() }
    fun checkInquiry(id: Int) { val db = this.writableDatabase; val values = ContentValues().apply { put(COLUMN_INQUIRY_IS_CHECKED, 1) }; db.update(TABLE_INQUIRY, values, "$COLUMN_INQUIRY_ID = ?", arrayOf(id.toString())); db.close() }
    fun getAllInquiries(): List<InquiryData> { val list = ArrayList<InquiryData>(); val cursor = this.readableDatabase.rawQuery("SELECT * FROM $TABLE_INQUIRY ORDER BY $COLUMN_INQUIRY_ID DESC", null); if (cursor.moveToFirst()) do { list.add(InquiryData(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getInt(4) == 1)) } while (cursor.moveToNext()); cursor.close(); return list }

    // ==========================================
    // 🍔 사장님 메뉴 관리 전용 함수
    // ==========================================

    /**
     * 특정 가게(storeId)의 저장된 메뉴 데이터를 모두 반환합니다.
     * 화면 재진입 시 기존 입력 데이터를 유지하기 위해 호출됩니다.
     */
    fun getMenusByStoreId(storeId: Int): List<MenuData> {
        val list = ArrayList<MenuData>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_MENU WHERE $COLUMN_MENU_STORE_ID = ?", arrayOf(storeId.toString()))

        if (cursor.moveToFirst()) {
            do {
                list.add(
                    MenuData(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MENU_ID)),
                        storeId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MENU_STORE_ID)),
                        name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MENU_NAME)),
                        price = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MENU_PRICE)),
                        desc = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MENU_DESC)),
                        imageUri = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MENU_IMAGE_URI))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    /**
     * 리스트로 전달된 메뉴 데이터를 트랜잭션 기반으로 동기화합니다.
     * 화면에서 삭제된 데이터의 처리 복잡도를 줄이기 위해, 해당 가게의 기존 메뉴를 전부 삭제하고 새 리스트를 일괄 삽입합니다.
     */
    fun saveOrUpdateMenus(storeId: Int, menuList: List<MenuData>) {
        val db = this.writableDatabase
        db.beginTransaction() // 트랜잭션 시작 (성능 최적화 및 원자성 보장)
        try {
            // 1. 기존에 저장되어 있던 이 가게의 모든 메뉴 레코드 삭제
            db.delete(TABLE_MENU, "$COLUMN_MENU_STORE_ID = ?", arrayOf(storeId.toString()))

            // 2. 현재 화면에 남아있는(수정된) 메뉴 데이터들을 새로 Insert
            for (menu in menuList) {
                // 이름이나 가격이 비어있지 않은 유효한 데이터만 저장 처리 (필수 요구사항 시)
                if (menu.name.isNotBlank() || menu.price.isNotBlank()) {
                    val values = ContentValues().apply {
                        put(COLUMN_MENU_STORE_ID, storeId)
                        put(COLUMN_MENU_NAME, menu.name)
                        put(COLUMN_MENU_PRICE, menu.price)
                        put(COLUMN_MENU_DESC, menu.desc)
                        put(COLUMN_MENU_IMAGE_URI, menu.imageUri)
                    }
                    db.insert(TABLE_MENU, null, values)
                }
            }
            db.setTransactionSuccessful() // 모든 쿼리가 정상 수행됨을 확정
        } finally {
            db.endTransaction() // 트랜잭션 종료 (커밋)
            db.close()
        }
    }

    // 🔥 공지사항 기능 함수들
    fun insertNotice(storeId: Int, title: String, content: String, bannerUri: String, originalUri: String) {
        this.writableDatabase.apply {
            insert(TABLE_NOTICE, null, ContentValues().apply {
                put(COLUMN_NOTICE_STORE_ID, storeId); put(COLUMN_NOTICE_TITLE, title); put(COLUMN_NOTICE_CONTENT, content)
                put(COLUMN_NOTICE_BANNER_URI, bannerUri); put(COLUMN_NOTICE_ORIGINAL_URI, originalUri)
            })
            close()
        }
    }

    fun updateNotice(id: Int, title: String, content: String, bannerUri: String, originalUri: String) {
        this.writableDatabase.apply {
            update(TABLE_NOTICE, ContentValues().apply {
                put(COLUMN_NOTICE_TITLE, title); put(COLUMN_NOTICE_CONTENT, content)
                put(COLUMN_NOTICE_BANNER_URI, bannerUri); put(COLUMN_NOTICE_ORIGINAL_URI, originalUri)
            }, "$COLUMN_NOTICE_ID = ?", arrayOf(id.toString()))
            close()
        }
    }

    fun deleteNotice(id: Int) {
        this.writableDatabase.apply { delete(TABLE_NOTICE, "$COLUMN_NOTICE_ID = ?", arrayOf(id.toString())); close() }
    }

    fun getNoticesByStoreId(storeId: Int): List<NoticeData> {
        val list = ArrayList<NoticeData>()
        val cursor = this.readableDatabase.rawQuery("SELECT * FROM $TABLE_NOTICE WHERE $COLUMN_NOTICE_STORE_ID = ? ORDER BY $COLUMN_NOTICE_ID DESC", arrayOf(storeId.toString()))
        if (cursor.moveToFirst()) {
            do {
                list.add(NoticeData(cursor.getInt(0), cursor.getInt(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5)))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    // 🔥 반려동물 관련 함수들 (맨 밑에 추가)
    fun getPetsByUserId(userId: Int): List<PetData> {
        val list = ArrayList<PetData>()
        val cursor = this.readableDatabase.rawQuery("SELECT * FROM $TABLE_PET WHERE $COLUMN_PET_USER_ID = ?", arrayOf(userId.toString()))
        if (cursor.moveToFirst()) {
            do {
                list.add(PetData(cursor.getInt(0), cursor.getInt(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5)))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun saveOrUpdatePets(userId: Int, petList: List<PetData>) {
        val db = this.writableDatabase
        db.beginTransaction()
        try {
            // 1. 기존 데이터 싹 날리기 (동기화)
            db.delete(TABLE_PET, "$COLUMN_PET_USER_ID = ?", arrayOf(userId.toString()))
            // 2. 현재 입력된 리스트 통째로 다시 저장
            for (pet in petList) {
                val values = ContentValues().apply {
                    put(COLUMN_PET_USER_ID, userId)
                    put(COLUMN_PET_NAME, pet.name)
                    put(COLUMN_PET_BIRTHDAY, pet.birthday)
                    put(COLUMN_PET_BREED, pet.breed)
                    put(COLUMN_PET_IMAGE_URI, pet.imageUri)
                }
                db.insert(TABLE_PET, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.close()
        }
    }

}
