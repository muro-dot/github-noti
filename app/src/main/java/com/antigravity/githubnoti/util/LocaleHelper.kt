package com.antigravity.githubnoti.util

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.antigravity.githubnoti.data.local.PreferenceManager
import java.util.Locale

/**
 * 앱의 언어(다국어) 설정 및 동적 로케일 적용을 총괄하는 유틸리티 클래스입니다.
 *
 * 안드로이드 시스템 기본 설정 및 앱 내부 수동 언어 전환("ko", "en", "system")을 완벽히 지원하며,
 * Android 13(API 33) 이상의 시스템 앱 언어 설정(LocaleManager)과도 완벽하게 상호 호환됩니다.
 */
object LocaleHelper {

    const val LANGUAGE_SYSTEM = "system"
    const val LANGUAGE_KOREAN = "ko"
    const val LANGUAGE_ENGLISH = "en"

    /**
     * 지정된 언어 코드("system", "ko", "en")에 맞춰 Context의 Configuration을 래핑하여 반환합니다.
     * Activity의 attachBaseContext()에서 호출하여 앱 전체의 Resource 문자열을 동적으로 교체합니다.
     */
    fun wrapContext(context: Context, languageCode: String): Context {
        if (languageCode == LANGUAGE_SYSTEM) {
            // 시스템 기본 언어 설정을 그대로 따름
            return context
        }

        val targetLocale = when (languageCode) {
            LANGUAGE_KOREAN -> Locale.KOREAN
            LANGUAGE_ENGLISH -> Locale.ENGLISH
            else -> Locale.forLanguageTag(languageCode)
        }

        Locale.setDefault(targetLocale)

        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(targetLocale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = targetLocale
        }

        return context.createConfigurationContext(config)
    }

    /**
     * 사용자가 설정 화면에서 언어를 변경했을 때 호출됩니다.
     * 설정을 SharedPreferences에 저장하고, 시스템 API(Android 13+)를 업데이트한 뒤 액티비티를 새로고침합니다.
     */
    fun setAppLanguage(activity: Activity, languageCode: String) {
        val prefs = PreferenceManager(activity)
        prefs.appLanguage = languageCode

        // Android 13+ (API 33) 전용 Per-app language 동기화
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = activity.getSystemService(LocaleManager::class.java)
            if (languageCode == LANGUAGE_SYSTEM) {
                localeManager?.applicationLocales = LocaleList.getEmptyLocaleList()
            } else {
                localeManager?.applicationLocales = LocaleList.forLanguageTags(languageCode)
            }
        }

        // 전체 화면 UI를 즉시 새로운 언어 리소스로 다시 로드
        activity.recreate()
    }
}
