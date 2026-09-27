package com.example.veltrix

import androidx.annotation.DrawableRes
import com.example.veltrix.R

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
        "nexagi" -> R.drawable.ic_provider_nexagi
        "nvidia" -> R.drawable.ic_provider_nvidia
        "openai" -> R.drawable.ic_provider_openai
        "poolside" -> R.drawable.ic_provider_poolside
        "qwen" -> R.drawable.ic_provider_qwen
        "thinkingmachines" -> R.drawable.ic_provider_thinkingmachines
        "xai" -> R.drawable.ic_provider_xai
        "zai" -> R.drawable.ic_provider_zai
        else -> null
    }
}
