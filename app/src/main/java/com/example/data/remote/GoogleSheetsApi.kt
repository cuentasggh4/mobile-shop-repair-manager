package com.example.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class GoogleUserInfoResponse(
    val email: String? = null,
    val name: String? = null
)

@JsonClass(generateAdapter = true)
data class DriveFilesResponse(
    val files: List<DriveFileItem>? = null
)

@JsonClass(generateAdapter = true)
data class DriveFileItem(
    val id: String,
    val name: String? = null,
    val parents: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class DriveCreateFileRequest(
    val name: String,
    val mimeType: String,
    val parents: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class DriveCopyFileRequest(
    val name: String,
    val parents: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class CreateSpreadsheetRequest(
    val properties: SpreadsheetProperties,
    val sheets: List<SheetCreateSpec>
)

@JsonClass(generateAdapter = true)
data class SpreadsheetProperties(
    val title: String
)

@JsonClass(generateAdapter = true)
data class SheetCreateSpec(
    val properties: SheetPropertiesSpec
)

@JsonClass(generateAdapter = true)
data class SheetPropertiesSpec(
    val title: String
)

@JsonClass(generateAdapter = true)
data class SpreadsheetMetadataResponse(
    val spreadsheetId: String,
    val spreadsheetUrl: String? = null,
    val sheets: List<SheetWrapper>? = null
)

@JsonClass(generateAdapter = true)
data class SheetWrapper(
    val properties: SheetPropertiesInfo? = null
)

@JsonClass(generateAdapter = true)
data class SheetPropertiesInfo(
    val sheetId: Int? = null,
    val title: String? = null
)

@JsonClass(generateAdapter = true)
data class BatchUpdateSpreadsheetRequest(
    val requests: List<BatchUpdateRequestItem>
)

@JsonClass(generateAdapter = true)
data class BatchUpdateRequestItem(
    val addSheet: SheetCreateSpec
)

@JsonClass(generateAdapter = true)
data class ValueRangeResponse(
    val range: String? = null,
    val values: List<List<String>>? = null
)

@JsonClass(generateAdapter = true)
data class ValueRangeRequest(
    val values: List<List<String>>
)

@JsonClass(generateAdapter = true)
data class ValueRangeUpdateItem(
    val range: String,
    val values: List<List<String>>
)

@JsonClass(generateAdapter = true)
data class BatchUpdateValuesRequest(
    val valueInputOption: String = "RAW",
    val data: List<ValueRangeUpdateItem>
)

interface GoogleSheetsApi {

    @GET("https://www.googleapis.com/oauth2/v3/userinfo")
    suspend fun getUserInfo(
        @Header("Authorization") bearerToken: String
    ): GoogleUserInfoResponse

    @GET("https://www.googleapis.com/drive/v3/files")
    suspend fun findSpreadsheets(
        @Header("Authorization") bearerToken: String,
        @Query("q") query: String,
        @Query("fields") fields: String = "files(id,name,parents)",
        @Query("spaces") spaces: String = "drive"
    ): DriveFilesResponse

    @POST("https://www.googleapis.com/drive/v3/files")
    suspend fun createDriveFile(
        @Header("Authorization") bearerToken: String,
        @Query("fields") fields: String = "id,name,parents",
        @Body request: DriveCreateFileRequest
    ): DriveFileItem

    @PATCH("https://www.googleapis.com/drive/v3/files/{fileId}")
    suspend fun moveDriveFileToFolder(
        @Header("Authorization") bearerToken: String,
        @Path("fileId") fileId: String,
        @Query("addParents") addParents: String,
        @Query("removeParents") removeParents: String? = null,
        @Query("fields") fields: String = "id,name,parents",
        @Body emptyBody: Map<String, String> = emptyMap()
    ): DriveFileItem

    @POST("https://www.googleapis.com/drive/v3/files/{fileId}/copy")
    suspend fun copyDriveFile(
        @Header("Authorization") bearerToken: String,
        @Path("fileId") fileId: String,
        @Query("fields") fields: String = "id,name,parents",
        @Body request: DriveCopyFileRequest
    ): DriveFileItem

    @GET("https://www.googleapis.com/drive/v3/files/{fileId}")
    suspend fun getDriveFileMetadata(
        @Header("Authorization") bearerToken: String,
        @Path("fileId") fileId: String,
        @Query("fields") fields: String = "id,name,parents"
    ): DriveFileItem

    @POST("https://sheets.googleapis.com/v4/spreadsheets")
    suspend fun createSpreadsheet(
        @Header("Authorization") bearerToken: String,
        @Body request: CreateSpreadsheetRequest
    ): SpreadsheetMetadataResponse

    @GET("https://sheets.googleapis.com/v4/spreadsheets/{spreadsheetId}")
    suspend fun getSpreadsheetMetadata(
        @Header("Authorization") bearerToken: String,
        @Path("spreadsheetId") spreadsheetId: String,
        @Query("fields") fields: String = "spreadsheetId,spreadsheetUrl,sheets.properties"
    ): SpreadsheetMetadataResponse

    @POST("https://sheets.googleapis.com/v4/spreadsheets/{spreadsheetId}:batchUpdate")
    suspend fun batchUpdateSpreadsheet(
        @Header("Authorization") bearerToken: String,
        @Path("spreadsheetId") spreadsheetId: String,
        @Body request: BatchUpdateSpreadsheetRequest
    )

    @GET("https://sheets.googleapis.com/v4/spreadsheets/{spreadsheetId}/values/{range}")
    suspend fun getValues(
        @Header("Authorization") bearerToken: String,
        @Path("spreadsheetId") spreadsheetId: String,
        @Path("range") range: String
    ): ValueRangeResponse

    @PUT("https://sheets.googleapis.com/v4/spreadsheets/{spreadsheetId}/values/{range}")
    suspend fun updateValues(
        @Header("Authorization") bearerToken: String,
        @Path("spreadsheetId") spreadsheetId: String,
        @Path("range") range: String,
        @Query("valueInputOption") valueInputOption: String = "RAW",
        @Body body: ValueRangeRequest
    )

    @POST("https://sheets.googleapis.com/v4/spreadsheets/{spreadsheetId}/values/{range}:append")
    suspend fun appendValues(
        @Header("Authorization") bearerToken: String,
        @Path("spreadsheetId") spreadsheetId: String,
        @Path("range") range: String,
        @Query("valueInputOption") valueInputOption: String = "RAW",
        @Query("insertDataOption") insertDataOption: String = "INSERT_ROWS",
        @Body body: ValueRangeRequest
    )

    @POST("https://sheets.googleapis.com/v4/spreadsheets/{spreadsheetId}/values:batchUpdate")
    suspend fun batchUpdateValues(
        @Header("Authorization") bearerToken: String,
        @Path("spreadsheetId") spreadsheetId: String,
        @Body body: BatchUpdateValuesRequest
    )

    @POST("https://sheets.googleapis.com/v4/spreadsheets/{spreadsheetId}/values/{range}:clear")
    suspend fun clearValues(
        @Header("Authorization") bearerToken: String,
        @Path("spreadsheetId") spreadsheetId: String,
        @Path("range") range: String,
        @Body emptyBody: Map<String, String> = emptyMap()
    )
}
