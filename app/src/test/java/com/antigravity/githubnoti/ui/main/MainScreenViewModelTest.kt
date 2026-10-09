package com.antigravity.githubnoti.ui.main

import com.antigravity.githubnoti.data.model.DownloadIncreaseEvent
import com.antigravity.githubnoti.data.model.GithubAsset
import com.antigravity.githubnoti.data.model.GithubRelease
import com.antigravity.githubnoti.data.model.GithubRepo
import com.antigravity.githubnoti.data.model.RepoItemUiState
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GitHub 릴리즈 모니터링 핵심 비즈니스 로직에 대한 단위 테스트입니다.
 */
class MainScreenViewModelTest {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun testDownloadIncreaseEventCalculation() {
        val event = DownloadIncreaseEvent(
            repoFullName = "test-user/sample-repo",
            releaseTagName = "v1.0.0",
            assetName = "release-v1.0.0.apk",
            previousCount = 120L,
            newCount = 150L,
            releaseUrl = "https://github.com/test-user/sample-repo/releases/tag/v1.0.0"
        )

        assertEquals("증가한 다운로드 수 계산이 정확해야 합니다.", 30L, event.increaseAmount)
    }

    @Test
    fun testRepoFilteringAndSortingByDownloads() {
        val repoA = GithubRepo(
            id = 1,
            name = "Alpha",
            fullName = "user/Alpha",
            description = "Android app for notifications",
            htmlUrl = "https://github.com/user/Alpha",
            stargazersCount = 5
        )
        val repoB = GithubRepo(
            id = 2,
            name = "Beta",
            fullName = "user/Beta",
            description = "Web tool",
            htmlUrl = "https://github.com/user/Beta",
            stargazersCount = 20
        )

        val itemA = RepoItemUiState(repo = repoA, totalDownloadCount = 100L)
        val itemB = RepoItemUiState(repo = repoB, totalDownloadCount = 500L)

        val state = MainUiState(
            repos = listOf(itemA, itemB),
            sortType = SortType.DOWNLOAD_DESC
        )

        val sorted = state.filteredRepos
        assertEquals("다운로드 많은 순으로 정렬 시 Beta가 첫 번째여야 합니다.", "Beta", sorted[0].repo.name)
        assertEquals("Alpha가 두 번째여야 합니다.", "Alpha", sorted[1].repo.name)
    }

    @Test
    fun testRepoFilteringAndSortingByStars() {
        val repoA = GithubRepo(id = 1, name = "Alpha", fullName = "user/Alpha", htmlUrl = "url", stargazersCount = 50)
        val repoB = GithubRepo(id = 2, name = "Beta", fullName = "user/Beta", htmlUrl = "url", stargazersCount = 10)

        val itemA = RepoItemUiState(repo = repoA, totalDownloadCount = 10L)
        val itemB = RepoItemUiState(repo = repoB, totalDownloadCount = 50L)

        val state = MainUiState(
            repos = listOf(itemA, itemB),
            sortType = SortType.STARS_DESC
        )

        val sorted = state.filteredRepos
        assertEquals("스타 많은 순으로 정렬 시 Alpha가 첫 번째여야 합니다.", "Alpha", sorted[0].repo.name)
    }

    @Test
    fun testSearchQueryFiltering() {
        val repoA = GithubRepo(id = 1, name = "Android-Noti", fullName = "user/Android-Noti", htmlUrl = "url")
        val repoB = GithubRepo(id = 2, name = "Web-Dashboard", fullName = "user/Web-Dashboard", htmlUrl = "url")

        val state = MainUiState(
            repos = listOf(RepoItemUiState(repo = repoA), RepoItemUiState(repo = repoB)),
            searchQuery = "Noti"
        )

        val filtered = state.filteredRepos
        assertEquals("검색어 'Noti'에 매칭되는 리포지토리는 1개여야 합니다.", 1, filtered.size)
        assertEquals("Android-Noti", filtered[0].repo.name)
    }

    @Test
    fun testGithubReleaseJsonParsing() {
        val sampleJson = """
            {
              "id": 12345,
              "tag_name": "v2.1.0",
              "name": "Release 2.1.0",
              "html_url": "https://github.com/user/repo/releases/tag/v2.1.0",
              "published_at": "2026-01-01T00:00:00Z",
              "assets": [
                {
                  "id": 999,
                  "name": "app.apk",
                  "size": 10485760,
                  "download_count": 42,
                  "browser_download_url": "https://github.com/user/repo/releases/download/v2.1.0/app.apk"
                }
              ]
            }
        """.trimIndent()

        val release = jsonParser.decodeFromString<GithubRelease>(sampleJson)
        assertEquals(12345L, release.id)
        assertEquals("v2.1.0", release.tagName)
        assertEquals(1, release.assets.size)
        assertEquals(42L, release.assets[0].downloadCount)
    }
}
