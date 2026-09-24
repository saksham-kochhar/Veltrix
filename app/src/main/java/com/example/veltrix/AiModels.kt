package com.example.veltrix

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class AiModelOption(
    val id: String,
    val displayName: String,
    val icon: ImageVector,
    val tint: Color,
    val minTier: String = "free",
    val providerId: String
)

data class AiProvider(
    val id: String,
    val displayName: String,
    val icon: ImageVector,
    val tint: Color,
    val models: List<AiModelOption>
)

object AiModels {
    const val OFFLINE_DISPLAY = "Qwen 2.5"
    const val DEFAULT_ONLINE_ID = "openrouter/free"

    val offline = AiModelOption(
        id = "local/qwen-2.5",
        displayName = OFFLINE_DISPLAY,
        icon = Icons.Outlined.CloudOff,
        tint = Color(0xFF00C853),
        providerId = "local"
    )

    val offlineProvider = AiProvider(
        id = "local",
        displayName = "On device",
        icon = Icons.Outlined.CloudOff,
        tint = Color(0xFF00C853),
        models = listOf(offline)
    )

    /** Full PRD v1.0 catalog; server sets which entries are allowed per user. */
    val providers: List<AiProvider> = listOf(
        provider(
            id = "veltrix",
            displayName = "Veltrix",
            icon = Icons.Outlined.AutoAwesome,
            tint = Color(0xFF5B4DFF),
            "openrouter/free" to "OpenRouter Free" to "free"
        ),
        provider(
            id = "nvidia",
            displayName = "NVIDIA",
            icon = Icons.Outlined.Memory,
            tint = Color(0xFF76B900),
            "nvidia/nemotron-3-ultra-550b-a55b:free" to "Nemotron 3 Ultra 550B" to "free",
            "nvidia/nemotron-3-super-120b-a12b:free" to "Nemotron 3 Super 120B" to "free",
            "nvidia/nemotron-3-nano-30b-a3b:free" to "Nemotron Nano 30B" to "free",
            "nvidia/nemotron-nano-12b-2-vl:free" to "Nemotron Nano 12B VL" to "free",
            "nvidia/nemotron-nano-9b-v2:free" to "Nemotron Nano 9B" to "free"
        ),
        provider(
            id = "qwen",
            displayName = "Qwen",
            icon = Icons.Outlined.SmartToy,
            tint = Color(0xFF607D8B),
            "qwen/qwen3-coder:free" to "Qwen3 Coder 480B" to "free",
            "qwen/qwen3-next-80b-a3b:free" to "Qwen3 Next 80B" to "free",
            "qwen/qwen3-4b:free" to "Qwen3 4B" to "free"
        ),
        provider(
            id = "google",
            displayName = "Google",
            icon = Icons.Outlined.Bolt,
            tint = Color(0xFF4285F4),
            "google/gemma-4-31b-it:free" to "Gemma 4 31B" to "free",
            "google/gemma-4-26b-a4b-it:free" to "Gemma 4 26B MoE" to "free",
            "google/gemini-2.5-flash-lite" to "Gemini 2.5 Flash-Lite" to "pro",
            "google/gemini-2.5-flash" to "Gemini 2.5 Flash" to "pro",
            "google/gemini-3.6-flash" to "Gemini 3.6 Flash" to "pro_plus",
            "google/gemini-3.1-pro" to "Gemini 3.1 Pro" to "pro_plus"
        ),
        provider(
            id = "openai",
            displayName = "OpenAI",
            icon = Icons.Outlined.Psychology,
            tint = Color(0xFF009688),
            "openai/gpt-oss-120b:free" to "gpt-oss 120B" to "free",
            "openai/gpt-oss-20b:free" to "gpt-oss 20B" to "free",
            "openai/gpt-5.4-nano" to "GPT-5.4 Nano" to "pro",
            "openai/gpt-5.6-luna" to "GPT-5.6 Luna" to "pro_plus",
            "openai/gpt-5.6-terra" to "GPT-5.6 Terra" to "ultra",
            "openai/gpt-5.6-sol" to "GPT-5.6 Sol" to "ultra"
        ),
        provider(
            id = "meta",
            displayName = "Meta",
            icon = Icons.Outlined.Hub,
            tint = Color(0xFF1877F2),
            "meta-llama/llama-3.3-70b-instruct:free" to "Llama 3.3 70B" to "free",
            "meta-llama/llama-3.2-3b-instruct:free" to "Llama 3.2 3B" to "free",
            "meta-llama/llama-3.1-405b-instruct:free" to "Llama 3.1 405B" to "free"
        ),
        provider(
            id = "nous",
            displayName = "Nous",
            icon = Icons.Outlined.Science,
            tint = Color(0xFF8E24AA),
            "nousresearch/hermes-3-llama-3.1-405b:free" to "Hermes 3 405B" to "free"
        ),
        provider(
            id = "zai",
            displayName = "Z.ai",
            icon = Icons.Outlined.Bolt,
            tint = Color(0xFF455A64),
            "z-ai/glm-4.5-air:free" to "GLM 4.5 Air" to "free"
        ),
        provider(
            id = "poolside",
            displayName = "Poolside",
            icon = Icons.Outlined.Hub,
            tint = Color(0xFF00695C),
            "poolside/laguna-xs.2:free" to "Laguna XS" to "free"
        ),
        provider(
            id = "allenai",
            displayName = "AllenAI",
            icon = Icons.Outlined.Science,
            tint = Color(0xFF5D4037),
            "allenai/molmo2-8b:free" to "Molmo2 8B" to "free"
        ),
        provider(
            id = "liquid",
            displayName = "LiquidAI",
            icon = Icons.Outlined.Memory,
            tint = Color(0xFF00ACC1),
            "liquid/lfm2.5-1.2b-instruct:free" to "LFM2.5 1.2B" to "free"
        ),
        provider(
            id = "tngtech",
            displayName = "TNG",
            icon = Icons.Outlined.AutoAwesome,
            tint = Color(0xFF6A1B9A),
            "tngtech/r1t-chimera:free" to "R1T Chimera" to "free"
        ),
        provider(
            id = "deepseek",
            displayName = "DeepSeek",
            icon = Icons.Outlined.Bolt,
            tint = Color(0xFF26A69A),
            "deepseek/deepseek-v4" to "DeepSeek V4" to "pro"
        ),
        provider(
            id = "mistral",
            displayName = "Mistral",
            icon = Icons.Outlined.WbSunny,
            tint = Color(0xFFFF6F00),
            "mistralai/mistral-large-3" to "Mistral Large 3" to "pro"
        ),
        provider(
            id = "xai",
            displayName = "xAI",
            icon = Icons.Outlined.Psychology,
            tint = Color(0xFF212121),
            "x-ai/grok-4.7" to "Grok 4.7" to "pro_plus"
        ),
        provider(
            id = "anthropic",
            displayName = "Anthropic",
            icon = Icons.Outlined.WbSunny,
            tint = Color(0xFFFF9800),
            "anthropic/claude-haiku-4-5" to "Claude Haiku 4.5" to "pro_plus",
            "anthropic/claude-sonnet-5" to "Claude Sonnet 5" to "ultra",
            "anthropic/claude-opus-4-8" to "Claude Opus 4.8" to "ultra"
        )
    )

    val online: List<AiModelOption> = providers.flatMap { it.models }

    fun isModelAllowed(modelId: String, access: Map<String, Boolean>?): Boolean {
        if (access == null || access.isEmpty()) {
            return find(modelId)?.minTier == "free"
        }
        return access[modelId] == true
    }

    fun find(id: String): AiModelOption? =
        online.find { it.id == id } ?: offline.takeIf { it.id == id }

    fun providerOf(id: String): AiProvider? =
        providers.find { provider -> provider.models.any { it.id == id } }
            ?: offlineProvider.takeIf { it.models.any { model -> model.id == id } }

    fun displayName(id: String): String =
        find(id)?.displayName ?: id.substringAfterLast('/')

    fun tierLabel(minTier: String): String = when (minTier) {
        "pro" -> "Pro"
        "pro_plus" -> "Pro+"
        "ultra" -> "Ultra"
        else -> "Free"
    }

    fun lockLabel(minTier: String, freeOnlyMode: Boolean): String {
        if (freeOnlyMode) return "Credits exhausted — free models only"
        return "Requires ${tierLabel(minTier)}"
    }

    private fun provider(
        id: String,
        displayName: String,
        icon: ImageVector,
        tint: Color,
        vararg models: Triple<String, String, String>
    ): AiProvider = AiProvider(
        id = id,
        displayName = displayName,
        icon = icon,
        tint = tint,
        models = models.map { (modelId, modelName, minTier) ->
            AiModelOption(
                id = modelId,
                displayName = modelName,
                icon = icon,
                tint = tint,
                minTier = minTier,
                providerId = id
            )
        }
    )
}

private infix fun <A, B, C> Pair<A, B>.to(third: C): Triple<A, B, C> = Triple(first, second, third)
