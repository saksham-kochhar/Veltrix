package com.example.cherryai

import androidx.annotation.DrawableRes
import com.example.cherryai.R

object ProviderLogos {
    @DrawableRes
    fun resFor(providerId: String): Int? = when (providerId) {
        "anthropic" -> R.drawable.ic_provider_anthropic
        "cohere" -> R.drawable.ic_provider_cohere
        "deepseek" -> R.drawable.ic_provider_deepseek
        "dots" -> R.drawable.ic_provider_dots
        "google" -> R.drawable.ic_provider_google
        "inclusionai" -> R.drawable.ic_provider_inclusionai
        "liquid" -> R.drawable.ic_provider_liquid
        "mistral" -> R.drawable.ic_provider_mistral
        // Live catalog ids use the vendor-qualified forms below.
        "nex-agi" -> R.drawable.ic_provider_nexagi
        "nexagi" -> R.drawable.ic_provider_nexagi
        "nvidia" -> R.drawable.ic_provider_nvidia
        "openai" -> R.drawable.ic_provider_openai
        "openrouter" -> R.drawable.ic_provider_openai
        "poolside" -> R.drawable.ic_provider_poolside
        "qwen" -> R.drawable.ic_provider_qwen
        "thinkingmachines" -> R.drawable.ic_provider_thinkingmachines
        "x-ai" -> R.drawable.ic_provider_xai
        "xai" -> R.drawable.ic_provider_xai
        "z-ai" -> R.drawable.ic_provider_zai
        "zai" -> R.drawable.ic_provider_zai
        else -> null
    }
}
