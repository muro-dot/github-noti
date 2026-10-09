package com.antigravity.githubnoti.data.api

import com.antigravity.githubnoti.data.model.GithubRelease
import com.antigravity.githubnoti.data.model.GithubRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * GitHub REST API v3와 통신하여 리포지토리 및 릴리즈 정보를 조회하는 클라이언트입니다.
 *
 * 네트워크 호출 시 발생할 수 있는 403 Rate Limit, 404 Not Found, 인터넷 연결 오류 등을
 * Kotlin [Result] 타입으로 안전하게 캡슐화하여 UI/워커에서 명확히 대처할 수 있도록 설계되었습니다.
 */
class GithubApiClient {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonParser = Json {
        ignoreUnknownKeys = true // GitHub 응답의 수많은 부가 필드들을 유연하게 무시
        isLenient = true
        coerceInputValues = true
    }

    companion object {
        private const val BASE_URL = "https://api.github.com"
    }

    /**
     * 특정 깃허브 사용자의 공개(Public) 리포지토리 목록을 가져옵니다.
     *
     * @param username 깃허브 사용자 계정 또는 조직명
     * @param token 개인 액세스 토큰 (선택 사항, Rate limit 확장용)
     * @return 성공 시 [GithubRepo] 리스트, 실패 시 발생한 예외 정보를 담은 [Result]
     */
    suspend fun fetchUserRepositories(
        username: String,
        token: String? = null
    ): Result<List<GithubRepo>> = withContext(Dispatchers.IO) {
        val cleanUser = username.trim()
        if (cleanUser.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("GitHub 계정명을 입력해주세요."))
        }

        // 최신 업데이트 순으로 최대 100개 공개 리포지토리 조회
        val url = "$BASE_URL/users/$cleanUser/repos?sort=updated&per_page=100&type=all"
        val requestBuilder = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github.v3+json")
            .header("User-Agent", "GitHub-Noti-Android-App")

        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer ${token.trim()}")
        }

        try {
            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                when (response.code) {
                    200 -> {
                        val body = response.body?.string() ?: "[]"
                        val repos: List<GithubRepo> = jsonParser.decodeFromString(body)
                        // 공개(Public) 리포지토리만 필터링하여 반환
                        Result.success(repos.filter { !it.isPrivate })
                    }
                    403 -> {
                        val remaining = response.header("x-ratelimit-remaining")
                        Result.failure(
                            IOException(
                                "GitHub API 호출 한도(Rate limit)를 초과했습니다. " +
                                "설정에서 GitHub 개인 액세스 토큰(PAT)을 입력하시면 시간당 5,000회까지 이용할 수 있습니다."
                            )
                        )
                    }
                    404 -> {
                        Result.failure(IOException("사용자 '$cleanUser'를 찾을 수 없습니다. 아이디를 확인해주세요."))
                    }
                    else -> {
                        Result.failure(IOException("GitHub API 요청 실패 (HTTP ${response.code}: ${response.message})"))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException("네트워크 연결을 확인해주세요: ${e.localizedMessage ?: e.message}", e))
        }
    }

    /**
     * 특정 리포지토리의 모든 릴리즈(Releases) 목록을 가져옵니다.
     * 각 릴리즈에는 바이너리 에셋 목록 및 다운로드 수가 포함되어 있습니다.
     *
     * @param repoFullName 리포지토리 전체 명칭 (예: "facebook/react", "user/repo")
     * @param token 개인 액세스 토큰 (선택 사항)
     * @return 성공 시 [GithubRelease] 리스트
     */
    suspend fun fetchRepositoryReleases(
        repoFullName: String,
        token: String? = null
    ): Result<List<GithubRelease>> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/repos/$repoFullName/releases?per_page=30"
        val requestBuilder = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github.v3+json")
            .header("User-Agent", "GitHub-Noti-Android-App")

        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer ${token.trim()}")
        }

        try {
            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                when (response.code) {
                    200 -> {
                        val body = response.body?.string() ?: "[]"
                        val releases: List<GithubRelease> = jsonParser.decodeFromString(body)
                        Result.success(releases)
                    }
                    404 -> {
                        // 릴리즈가 아예 없는 저장소일 경우 404가 올 수도 있으므로 빈 목록으로 우아하게 처리
                        Result.success(emptyList())
                    }
                    403 -> {
                        Result.failure(IOException("API 호출 한도 초과: 릴리즈 정보를 가져올 수 없습니다."))
                    }
                    else -> {
                        Result.failure(IOException("릴리즈 정보 요청 실패 (HTTP ${response.code})"))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
