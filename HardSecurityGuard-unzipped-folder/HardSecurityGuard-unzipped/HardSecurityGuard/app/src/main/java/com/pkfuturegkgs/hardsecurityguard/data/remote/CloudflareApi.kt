package com.pkfuturegkgs.hardsecurityguard.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit contract for the EXISTING Cloudflare Worker. Endpoints and
 * request shape are unchanged from what's already deployed:
 *   GET  /health  — authenticated health check
 *   POST /report  — authenticated report submission
 * The Worker itself is not modified by this app.
 */
interface CloudflareApi {
    @GET("/health")
    suspend fun health(@Header("Authorization") authHeader: String): Response<Void>

    @POST("/report")
    suspend fun postReport(
        @Header("Authorization") authHeader: String,
        @Body body: ReportRequest
    ): Response<Void>
}
