package com.antigravity.githubnoti.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 깃허브 리포지토리 정보를 담는 데이터 클래스입니다.
 * GitHub REST API (/users/{username}/repos) 응답과 1:1 매핑됩니다.
 *
 * @property id 리포지토리 고유 ID
 * @property name 리포지토리 이름 (예: "github-noti")
 * @property fullName 소유자 포함 전체 이름 (예: "octocat/Hello-World")
 * @property description 리포지토리 설명 (null 가능)
 * @property htmlUrl 브라우저로 접근할 수 있는 웹 URL
 * @property stargazersCount 스타(별) 개수
 * @property forksCount 포크 개수
 * @property language 주요 사용 언어
 * @property isPrivate 비공개 리포지토리 여부 (공개 채널 확인용)
 * @property updatedAt 최근 업데이트 일시 (ISO-8601 문자열)
 */
@Serializable
data class GithubRepo(
    val id: Long,
    val name: String,
    @SerialName("full_name")
    val fullName: String,
    val description: String? = null,
    @SerialName("html_url")
    val htmlUrl: String,
    @SerialName("stargazers_count")
    val stargazersCount: Int = 0,
    @SerialName("forks_count")
    val forksCount: Int = 0,
    val language: String? = null,
    @SerialName("private")
    val isPrivate: Boolean = false,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * 깃허브 릴리즈 정보를 담는 데이터 클래스입니다.
 * GitHub REST API (/repos/{owner}/{repo}/releases) 응답과 매핑됩니다.
 *
 * @property id 릴리즈 고유 ID
 * @property tagName 태그명 (예: "v1.0.0")
 * @property name 릴리즈 제목
 * @property htmlUrl 웹 브라우저에서 볼 수 있는 릴리즈 페이지 링크
 * @property publishedAt 릴리즈 배포 일시
 * @property assets 첨부된 바이너리/압축 파일 목록 (다운로드 수 포함)
 */
@Serializable
data class GithubRelease(
    val id: Long,
    @SerialName("tag_name")
    val tagName: String,
    val name: String? = null,
    @SerialName("html_url")
    val htmlUrl: String,
    @SerialName("published_at")
    val publishedAt: String? = null,
    val assets: List<GithubAsset> = emptyList()
)

/**
 * 릴리즈에 첨부된 각 에셋 파일의 정보를 담는 데이터 클래스입니다.
 * 사용자가 실제로 다운로드하는 파일(.apk, .zip, .exe 등)의 다운로드 수를 가지고 있습니다.
 *
 * @property id 에셋 고유 ID
 * @property name 파일명 (예: "app-release.apk")
 * @property size 파일 크기(바이트)
 * @property downloadCount 사용자들이 다운로드한 누적 횟수 (핵심 추적 지표)
 * @property browserDownloadUrl 직접 다운로드 링크
 */
@Serializable
data class GithubAsset(
    val id: Long,
    val name: String,
    val size: Long = 0,
    @SerialName("download_count")
    val downloadCount: Long = 0,
    @SerialName("browser_download_url")
    val browserDownloadUrl: String
)

/**
 * UI 화면 및 다운로드 수 모니터링을 위해 리포지토리와 릴리즈 정보를 결합한 모델입니다.
 *
 * @property repo 기본 리포지토리 정보
 * @property releases 해당 리포지토리의 릴리즈 목록
 * @property totalDownloadCount 모든 릴리즈와 에셋의 총 다운로드 수 합계
 * @property isTracked 백그라운드 다운로드 증가 알림 추적 대상 여부
 * @property isLoadingReleases 릴리즈 정보를 불러오는 중인지 여부
 */
data class RepoItemUiState(
    val repo: GithubRepo,
    val releases: List<GithubRelease> = emptyList(),
    val totalDownloadCount: Long = 0,
    val isTracked: Boolean = true,
    val isLoadingReleases: Boolean = false,
    val isExpanded: Boolean = false
)

/**
 * 다운로드 증가 감지 시 전달되는 이벤트 데이터입니다.
 * 알림 메시지 구성에 활용됩니다.
 */
data class DownloadIncreaseEvent(
    val repoFullName: String,
    val releaseTagName: String,
    val assetName: String,
    val previousCount: Long,
    val newCount: Long,
    val releaseUrl: String
) {
    val increaseAmount: Long get() = newCount - previousCount
}
