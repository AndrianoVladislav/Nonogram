package com.example.nonogram.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nonogram.R

@Composable
fun StartScreen(
    onCollectionClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    // Белый полупрозрачный цвет для эффекта стекла
    val glassColor = Color.White.copy(alpha = 0.15f)
    val highlightColor = Color.White.copy(alpha = 0.4f)


    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Уникальный фон для начального экрана
        Image(
            painter = painterResource(id = R.drawable.bg_start),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Nastavení
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.White
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Используем Box вместо Button для чистого визуального эффекта
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(220.dp)
                    .height(64.dp)
                    // 1. Сначала обрезаем форму, чтобы ripple не выходил за углы
                    .clip(RoundedCornerShape(16.dp))
                    // 2. Добавляем градиентный фон для объема (верх чуть плотнее, низ прозрачнее)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.25f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        )
                    )
                    // 3. Рисуем тонкую светлую рамку (эффект грани стекла)
                    .border(
                        BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                        RoundedCornerShape(16.dp)
                    )
                    // 4. Делаем кликабельной
                    .clickable { onCollectionClick() }
            ) {
                Text(
                    text = "Kolekce",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

    }
}