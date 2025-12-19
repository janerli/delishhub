package com.janerli.delishhub.core.ui.theme

import androidx.compose.ui.graphics.Color

// ===== Accent (еда/fresh) =====
// Light оставляем как было (яркость там уместна)
val LightPrimary = Color(0xFFEF6C00)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightSecondary = Color(0xFF558B2F)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightTertiary = Color(0xFF8D6E63)
val LightOnTertiary = Color(0xFFFFFFFF)

// ✅ Dark — приглушаем, чтобы сочеталось и не било по глазам
val DarkPrimary = Color(0xFFE38B2C)      // было ярче (FFA726)
val DarkOnPrimary = Color(0xFF1A1A1A)

val DarkSecondary = Color(0xFF8FBF6A)    // было ярче (9CCC65)
val DarkOnSecondary = Color(0xFF141414)

val DarkTertiary = Color(0xFFC9BDB7)     // мягче, меньше контраста
val DarkOnTertiary = Color(0xFF141414)

// ===== Containers (ВАЖНО: чтобы M3 не подставлял дефолтный purple) =====
// Light контейнеры — как раньше
val LightPrimaryContainer = Color(0xFFFFDCC8)
val LightOnPrimaryContainer = Color(0xFF3A1A00)

val LightSecondaryContainer = Color(0xFFDDECCB)
val LightOnSecondaryContainer = Color(0xFF1A2B0A)

val LightTertiaryContainer = Color(0xFFE8DAD5)
val LightOnTertiaryContainer = Color(0xFF2D1B16)

// ✅ Dark контейнеры — более “графитовые”, без кислотности
val DarkPrimaryContainer = Color(0xFF3A240F)   // было 5A2A00 (слишком рыже)
val DarkOnPrimaryContainer = Color(0xFFFFDCC8)

val DarkSecondaryContainer = Color(0xFF1C2416) // было 243510 (зелень резкая)
val DarkOnSecondaryContainer = Color(0xFFDDECCB)

val DarkTertiaryContainer = Color(0xFF272120)  // мягкий тёплый графит
val DarkOnTertiaryContainer = Color(0xFFE8DAD5)

// ===== Neutrals (чтобы не было “фиолетовой поверхности”) =====
val LightBackground = Color(0xFFFFFBFF)
val LightOnBackground = Color(0xFF1C1B1F)
val LightSurface = Color(0xFFFFFBFF)
val LightOnSurface = Color(0xFF1C1B1F)

val LightSurfaceVariant = Color(0xFFF2F2F2)
val LightOnSurfaceVariant = Color(0xFF2B2B2B)

val LightOutline = Color(0xFF79747E)

// ✅ Dark нейтрали — чуть глубже и ровнее
val DarkBackground = Color(0xFF0F1110)   // было 121212
val DarkOnBackground = Color(0xFFE6E6E6)

val DarkSurface = Color(0xFF0F1110)      // было 121212
val DarkOnSurface = Color(0xFFE6E6E6)

val DarkSurfaceVariant = Color(0xFF1A1D1B)   // было 1E1E1E
val DarkOnSurfaceVariant = Color(0xFFDADADA)

val DarkOutline = Color(0xFF8C8C8C)

// ===== Error =====
val ErrorRed = Color(0xFFB00020)

val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF410002)

val DarkErrorContainer = Color(0xFF5A1A1A)    // менее “красный прожектор”
val DarkOnErrorContainer = Color(0xFFFFDAD6)
