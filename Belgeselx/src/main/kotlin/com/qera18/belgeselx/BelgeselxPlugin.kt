package com.qera18.belgeselx

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class BelgeselxPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(BelgeselxProvider())
    }
}
