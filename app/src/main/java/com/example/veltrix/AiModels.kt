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

    /** Client catalog mirrors backend seed; server still enforces allow-lists. */
    val providers: List<AiProvider> = listOf(
        provider(
            id = "veltrix",
            displayName = "Veltrix",
            icon = Icons.Outlined.AutoAwesome,
            tint = Color(0xFF5B4DFF),
            "openrouter/free" to "Veltrix Free" to "free",
            "openrouter/auto" to "Veltrix Auto" to "pro"
        ),
        provider(
            id = "openai",
            displayName = "OpenAI",
            icon = Icons.Outlined.Psychology,
            tint = Color(0xFF009688),
            "openai/gpt-6-luna" to "GPT Luna" to "pro",
            "openai/gpt-5.6-luna" to "GPT-5.6 Luna" to "ultra",
            "openai/gpt-5.6-sol" to "GPT Sol" to "ultra",
            "openai/gpt-6-astra" to "GPT Astra" to "ultra"
        ),
        provider(
            id = "anthropic",
            displayName = "Anthropic",
            icon = Icons.Outlined.WbSunny,
            tint = Color(0xFFFF9800),
            "anthropic/claude-sonnet-4.6" to "Claude Sonnet 4.6" to "pro_plus"
        ),
        provider(
            id = "google",
            displayName = "Google",
            icon = Icons.Outlined.Bolt,
            tint = Color(0xFF4285F4),
            "google/gemma-4-26b-a4b-it:free" to "Gemma 4 26B" to "free",
            "google/gemma-4-31b-it:free" to "Gemma 4 31B" to "free",
            "google/gemini-3.8-flash" to "Gemini 3.8 Flash" to "pro_plus"
        ),
        provider(
            id = "deepseek",
            displayName = "DeepSeek",
            icon = Icons.Outlined.Bolt,
            tint = Color(0xFF26A69A),
            "deepseek/deepseek-v4.1-flash" to "DeepSeek Flash" to "pro",
            "deepseek/deepseek-v4-pro-0813" to "DeepSeek Pro" to "pro_plus"
        ),
        provider(
            id = "moonshot",
            displayName = "Moonshot",
            icon = Icons.Outlined.SmartToy,
            tint = Color(0xFF5C6BC0),
            "moonshotai/kimi-k3" to "Kimi K3" to "pro_plus"
        ),
        provider(
            id = "minimax",
            displayName = "MiniMax",
            icon = Icons.Outlined.SmartToy,
            tint = Color(0xFFE91E63),
            "minimax/minimax-m3" to "MiniMax M3" to "pro_plus"
        ),
        provider(
            id = "nvidia",
            displayName = "NVIDIA",
            icon = Icons.Outlined.Memory,
            tint = Color(0xFF76B900),
            "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free" to "Nemotron Nano" to "free"
        ),
        provider(
            id = "inception",
            displayName = "Inception",
            icon = Icons.Outlined.Hub,
            tint = Color(0xFF7C4DFF),
            "inception/mercury-2.5" to "Mercury 2.5" to "pro"
        ),
        provider(
            id = "inclusionai",
            displayName = "InclusionAI",
            icon = Icons.Outlined.SmartToy,
            tint = Color(0xFF00897B),
            "inclusionai/ling-3.0-flash" to "Ling 3 Flash" to "pro"
        ),
        provider(
            id = "sakana",
            displayName = "Sakana",
            icon = Icons.Outlined.Science,
            tint = Color(0xFF1565C0),
            "sakana/fugu-max" to "Fugu Max" to "pro_plus",
            "sakana/fugu-ultra-v2" to "Fugu Ultra" to "ultra"
        )
    )

    val online: List<AiModelOption> = providers.flatMap { it.models }

    /** Providers intersected with server allow-list; empty allow-list → full static catalog. */
    fun providersForAllowlist(allowedIds: Set<String>?): List<AiProvider> {
        if (allowedIds == null || allowedIds.isEmpty()) return providers
        return providers.mapNotNull { provider ->
            val models = provider.models.filter { it.id in allowedIds }
            if (models.isEmpty()) null
            else provider.copy(models = models)
        }
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
