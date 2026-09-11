package com.therealsylva.roaches.data.remote

import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets

object DashResolver {
    private val NOTICE_MARKERS = listOf(
        "macdn.aoneroom.com/other/",
        "/notice.mp4",
        "1c7de0bd",
        "9a0461bc",
        "b164fbfb",
    )

    fun isNoticeUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return true
        val lower = url.lowercase()
        return NOTICE_MARKERS.any(lower::contains)
    }

    fun resolveDashManifestFromPolicy(signCookie: String): String? {
        for (part in signCookie.split(';')) {
            val trimmed = part.trim()
            if (!trimmed.startsWith("CloudFront-Policy=")) continue

            var policy = trimmed
                .removePrefix("CloudFront-Policy=")
                .replace('-', '+')
                .replace('_', '=')
                .replace('~', '/')
            val pad = (4 - policy.length % 4) % 4
            if (pad > 0) policy += "=".repeat(pad)

            val decoded = try {
                String(Base64.decode(policy, Base64.DEFAULT), StandardCharsets.UTF_8)
            } catch (_: Exception) {
                continue
            }
            val json = try {
                JSONObject(decoded)
            } catch (_: Exception) {
                continue
            }
            val resource = json.optJSONArray("Statement")
                ?.optJSONObject(0)
                ?.optString("Resource")
                ?.trimEnd('*', '/')
                ?: continue
            if (resource.startsWith("http://") || resource.startsWith("https://")) {
                return "$resource/index.mpd"
            }
        }
        return null
    }
}
