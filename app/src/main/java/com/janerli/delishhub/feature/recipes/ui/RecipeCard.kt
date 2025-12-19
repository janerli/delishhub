package com.janerli.delishhub.feature.recipes.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage

@Composable
fun RecipeCard(
    item: RecipeCardUi,
    onOpen: (String) -> Unit,
    onToggleFavorite: ((String) -> Unit)? = null
) {
    Card(
        onClick = { onOpen(item.id) },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        // ❗ВАЖНО: тут НЕ ДОЛЖНО быть fillMaxWidth(), иначе LazyRow будет делать квадрат
        modifier = Modifier.animateContentSize()
    ) {
        Column {

            // Фото (если есть) + бейджи поверх фото
            if (!item.imageUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    SubcomposeAsyncImage(
                        model = item.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
                        // ✅ главное: заполняем блок и обрезаем лишнее
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator() }
                        },
                        error = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) { Text("Не удалось загрузить фото") }
                        }
                    )

                    BadgesRow(
                        // ✅ FIX: демо-рецепты (ownerId == "demo") показываем как PUBLIC
                        isPublic = item.isPublic || item.ownerId == "demo",
                        isMine = item.isMine,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    )
                }
            } else {
                // Если фото нет — всё равно хотим видеть бейджи сверху карточки
                BadgesRow(
                    // ✅ FIX: демо-рецепты (ownerId == "demo") показываем как PUBLIC
                    isPublic = item.isPublic || item.ownerId == "demo",
                    isMine = item.isMine,
                    modifier = Modifier
                        .padding(start = 12.dp, top = 12.dp)
                )
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.Schedule, contentDescription = null)
                    Text(
                        text = "${item.cookTimeMin} мин",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Сложность ${item.difficulty}/5",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(Modifier.weight(1f))

                    if (onToggleFavorite != null) {
                        IconButton(onClick = { onToggleFavorite(item.id) }) {
                            Icon(
                                imageVector = if (item.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgesRow(
    isPublic: Boolean,
    isMine: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isPublic && !isMine) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isPublic) Badge(text = "PUBLIC")
        if (isMine) Badge(text = "МОЙ")
    }
}

@Composable
private fun Badge(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        tonalElevation = 2.dp,
        shadowElevation = 0.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}
