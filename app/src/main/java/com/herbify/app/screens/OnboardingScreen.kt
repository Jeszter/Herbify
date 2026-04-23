package com.herbify.app.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herbify.app.ui.theme.*

data class OnboardingPage(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val tip: String? = null,
    val accentColor: Color = NeonGreen
)

private val pages = listOf(
    OnboardingPage(
        emoji = "🌍",
        title = "Добро пожаловать\nв Herbify",
        subtitle = "Исследуй реальный мир",
        description = "Herbify превращает прогулки в игру. Находи растения вокруг тебя, сканируй их и строй свою коллекцию.",
        accentColor = NeonGreen
    ),
    OnboardingPage(
        emoji = "🗺️",
        title = "Открой карту",
        subtitle = "Найди зоны рядом",
        description = "На карте отображаются зоны — лес, парк, поле, город. В каждой зоне растут свои растения. Зайди в зону чтобы начать поиск.",
        tip = "💡 Чем редче зона — тем редче растения",
        accentColor = NeonGreen
    ),
    OnboardingPage(
        emoji = "📷",
        title = "Сфотай растение",
        subtitle = "AI определит вид",
        description = "Найди реальное растение, нажми кнопку сканера и сфотографируй его. Наш AI (PlantNet) определит вид за несколько секунд.",
        tip = "📸 Фотай крупно — листья, цветы, кору",
        accentColor = NeonGreen
    ),
    OnboardingPage(
        emoji = "✨",
        title = "Получай награды",
        subtitle = "XP и ECO токены",
        description = "За каждое найденное растение ты получаешь XP и ECO токены. Новый вид — большой бонус. Редкое растение — огромный бонус!",
        tip = "🔥 Первооткрыватель получает особый значок",
        accentColor = EcoGold
    ),
    OnboardingPage(
        emoji = "⏱️",
        title = "Кулдауны",
        subtitle = "Не фармь одно место",
        description = "После скана нужно подождать 2 минуты перед следующим. Одно и то же растение можно сканировать раз в 2-6 часов. Иди исследовать новые места!",
        tip = "🚶 Двигайся — это суть игры",
        accentColor = Color(0xFFFF8C00)
    ),
    OnboardingPage(
        emoji = "🛒",
        title = "Магазин апгрейдов",
        subtitle = "Трать ECO с умом",
        description = "На заработанные ECO можно купить улучшения: расширить радар, ускорить кулдаун сканера или получить бонус к наградам.",
        accentColor = NeonGreen
    ),
    OnboardingPage(
        emoji = "🌿",
        title = "Всё готово!",
        subtitle = "Начни исследовать",
        description = "Твоя коллекция пуста — время это исправить. Выйди на улицу, найди первое растение и стань частью сообщества Herbify.",
        tip = "🎯 Первая цель: найди 5 разных растений",
        accentColor = NeonGreen
    )
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var currentPage by remember { mutableStateOf(0) }
    val totalPages = pages.size

    // Pulse animation for emoji
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val emojiScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "emojiPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Skip button
        if (currentPage < totalPages - 1) {
            TextButton(
                onClick = { currentPage = totalPages - 1 },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Text("Пропустить", color = TextSecondary, fontSize = 14.sp)
            }
        }

        // Main content
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it } + fadeOut())
                } else {
                    (slideInHorizontally { -it } + fadeIn()) togetherWith
                    (slideOutHorizontally { it } + fadeOut())
                }
            },
            label = "pageTransition",
            modifier = Modifier.fillMaxSize()
        ) { pageIdx ->
            val page = pages[pageIdx]
            OnboardingPageContent(
                page = page,
                emojiScale = emojiScale
            )
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Dot indicators
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(totalPages) { idx ->
                    val isActive = idx == currentPage
                    val dotColor = if (isActive) NeonGreen else DarkBorder
                    val dotWidth = if (isActive) 24.dp else 8.dp
                    Box(
                        modifier = Modifier
                            .animateContentSize(tween(200))
                            .height(8.dp)
                            .width(dotWidth)
                            .background(dotColor, RoundedCornerShape(4.dp))
                    )
                }
            }

            // Next / Finish button
            val isLast = currentPage == totalPages - 1
            Button(
                onClick = {
                    if (isLast) onFinish()
                    else currentPage++
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLast) NeonGreen else NeonGreen
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    if (isLast) "🌿  Начать играть" else "Далее  →",
                    color = DarkBg,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // Back button (except first page)
            if (currentPage > 0) {
                TextButton(onClick = { currentPage-- }) {
                    Text("← Назад", color = TextSecondary, fontSize = 13.sp)
                }
            } else {
                Spacer(Modifier.height(36.dp))
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage, emojiScale: Float) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp)
            .padding(top = 80.dp, bottom = 200.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Emoji with glow ring
        Box(
            modifier = Modifier
                .size(130.dp)
                .scale(emojiScale)
                .background(page.accentColor.copy(alpha = 0.08f), CircleShape)
                .border(1.dp, page.accentColor.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(95.dp)
                    .background(page.accentColor.copy(alpha = 0.12f), CircleShape)
                    .border(1.dp, page.accentColor.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(page.emoji, fontSize = 48.sp)
            }
        }

        Spacer(Modifier.height(36.dp))

        // Title
        Text(
            page.title,
            color = TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp
        )

        Spacer(Modifier.height(8.dp))

        // Subtitle badge
        Box(
            modifier = Modifier
                .background(page.accentColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .border(1.dp, page.accentColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            Text(
                page.subtitle,
                color = page.accentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(24.dp))

        // Description
        Text(
            page.description,
            color = TextSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        // Tip card
        page.tip?.let { tip ->
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCard, RoundedCornerShape(12.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    tip,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
