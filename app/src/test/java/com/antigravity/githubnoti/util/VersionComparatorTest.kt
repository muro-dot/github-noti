package com.antigravity.githubnoti.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 버전 비교 유틸리티(VersionComparator)에 대한 단위 테스트입니다.
 */
class VersionComparatorTest {

    @Test
    fun testIsNewerWithStandardSemVer() {
        assertTrue("1.1.0은 1.0.0보다 최신이어야 합니다.", VersionComparator.isNewer("1.0.0", "1.1.0"))
        assertTrue("v2.0.0은 v1.9.9보다 최신이어야 합니다.", VersionComparator.isNewer("v1.9.9", "v2.0.0"))
        assertFalse("동일 버전은 최신이 아니어야 합니다.", VersionComparator.isNewer("1.1.0", "1.1.0"))
        assertFalse("과거 버전은 최신이 아니어야 합니다.", VersionComparator.isNewer("1.2.0", "1.1.0"))
    }

    @Test
    fun testIsNewerWithPrefixAndPatchDifferences() {
        assertTrue("v 접두사가 있어도 1.1.0은 1.0보다 최신이어야 합니다.", VersionComparator.isNewer("1.0", "v1.1.0"))
        assertTrue("패치 버전 증가를 올바르게 감지해야 합니다.", VersionComparator.isNewer("1.1.0", "v1.1.1"))
        assertFalse("마이너 버전이 낮으면 패치 버전이 높아도 최신이 아닙니다.", VersionComparator.isNewer("1.2.0", "v1.1.9"))
    }

    @Test
    fun testIsNewerWithPreReleaseTags() {
        assertTrue("프리릴리즈 태그가 붙어 있어도 기본 버전 비교가 가능해야 합니다.", VersionComparator.isNewer("1.0.0", "1.1.0-beta1"))
    }
}
