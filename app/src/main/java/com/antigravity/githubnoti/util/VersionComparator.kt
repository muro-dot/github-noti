package com.antigravity.githubnoti.util

/**
 * 시맨틱 버저닝(Semantic Versioning) 문자열을 비교하여
 * 최신 릴리즈가 현재 설치된 버전보다 상위 버전인지 판단하는 유틸리티입니다.
 */
object VersionComparator {

    /**
     * [latest] 버전이 [current] 버전보다 더 높은(최신) 버전인지 검사합니다.
     *
     * @param current 현재 설치된 앱 버전 (예: "1.0", "1.1.0", "v1.0")
     * @param latest GitHub에서 조회한 최신 릴리즈 태그 (예: "v1.1.0", "1.2.0-beta")
     * @return 최신 버전이 더 높으면 true, 같거나 낮으면 false
     */
    fun isNewer(current: String, latest: String): Boolean {
        val currentParts = parseVersionNumbers(current)
        val latestParts = parseVersionNumbers(latest)

        val maxLength = maxOf(currentParts.size, latestParts.size)

        for (i in 0 until maxLength) {
            val curr = currentParts.getOrElse(i) { 0 }
            val late = latestParts.getOrElse(i) { 0 }

            if (late > curr) return true
            if (late < curr) return false
        }

        return false
    }

    /**
     * 문자열에서 숫자 부분만 파싱하여 정수 리스트로 추출합니다.
     * 예: "v1.1.0" -> [1, 1, 0]
     * 예: "2.0.1-rc1" -> [2, 0, 1]
     */
    private fun parseVersionNumbers(versionStr: String): List<Int> {
        val sanitized = versionStr.trim()
            .removePrefix("v")
            .removePrefix("V")
            .split("-", "_", "+")[0] // 메타데이터 및 프리릴리즈 식별자 분리

        return sanitized.split(".")
            .mapNotNull { it.toIntOrNull() }
    }
}
