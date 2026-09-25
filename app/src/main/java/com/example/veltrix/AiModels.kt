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
            "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free" to "Nemotron 3 Nano Omni 30B" to "free"
        ),
        provider(
            id = "inclusionai",
            displayName = "Inclusion AI",
            icon = Icons.Outlined.AutoAwesome,
            tint = Color(0xFF3949AB),
            "inclusionai/ling-3.0-flash-fin:free" to "Ling 3.0 Flash Fin" to "free",
            "inclusionai/ling-3.0-flash-sante:free" to "Ling 3.0 Flash Santé" to "free"
        ),
        provider(
            id = "poolside",
            displayName = "Poolside",
            icon = Icons.Outlined.Hub,
            tint = Color(0xFF00695C),
            "poolside/laguna-s-2.1:free" to "Laguna S 2.1" to "free",
            "poolside/laguna-xs-2.1:free" to "Laguna XS 2.1" to "free"
        ),
        provider(
            id = "dots",
            displayName = "Dots",
            icon = Icons.Outlined.Bolt,
            tint = Color(0xFF5C6BC0),
            "dots-studio/dots-3-note-preview:free" to "Dots 3 Note Preview" to "free"
        ),
        provider(
            id = "nexagi",
            displayName = "Nex AGI",
            icon = Icons.Outlined.Psychology,
            tint = Color(0xFF00897B),
            "nex-agi/nex-n2.5-pro:free" to "Nex N2.5 Pro" to "free",
            "nex-agi/nex-n2.5-mini:free" to "Nex N2.5 Mini" to "free"
        ),
        provider(
            id = "thinkingmachines",
            displayName = "Thinking Machines",
            icon = Icons.Outlined.Science,
            tint = Color(0xFF6D4C41),
            "thinkingmachines/inkling:free" to "Inkling" to "free",
            "thinkingmachines/inkling-small:free" to "Inkling Small" to "free"
        ),
        provider(
            id = "cohere",
            displayName = "Cohere",
            icon = Icons.Outlined.Hub,
            tint = Color(0xFF2E7D32),
            "cohere/north-mini-code:free" to "North Mini Code" to "free"
        ),
        provider(
            id = "qwen",
            displayName = "Qwen",
            icon = Icons.Outlined.SmartToy,
            tint = Color(0xFF607D8B),
            "qwen/qwen3.8-27b:free" to "Qwen3.8 27B" to "free"
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
            "openai/gpt-5.4-nano" to "GPT-5.4 Nano" to "pro",
            "openai/gpt-5.6-luna" to "GPT-5.6 Luna" to "pro_plus",
            "openai/gpt-5.6-terra" to "GPT-5.6 Terra" to "ultra",
            "openai/gpt-5.6-sol" to "GPT-5.6 Sol" to "ultra"
        ),
        provider(
            id = "zai",
            displayName = "Z.ai",
            icon = Icons.Outlined.Bolt,
            tint = Color(0xFF455A64),
            "z-ai/glm-5.2:free" to "GLM 5.2" to "free"
        ),
        provider(
            id = "liquid",
            displayName = "LiquidAI",
            icon = Icons.Outlined.Memory,
            tint = Color(0xFF00ACC1),
            "liquid/lfm-2.5-2.6b:free" to "LFM 2.5 2.6B" to "free"
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
