package com.ntando.ivu.ui.review

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntando.ivu.R
import com.ntando.ivu.viewmodel.FlashcardReviewUiState
import com.ntando.ivu.viewmodel.FlashcardReviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardReviewScreen(
    viewModel: FlashcardReviewViewModel,
    deckId: String,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(deckId) {
        viewModel.loadDueCards(deckId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.review_session), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFFF8F0)
                )
            )
        },
        containerColor = Color(0xFFFFF8F0)
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (val state = uiState) {
                is FlashcardReviewUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFFE88A68)
                    )
                }
                is FlashcardReviewUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message, color = Color.Red, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadDueCards(deckId) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE88A68))
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                is FlashcardReviewUiState.Success -> {
                    ReviewContent(
                        cards = state.cards,
                        currentIndex = state.currentIndex,
                        isFlipped = state.isFlipped,
                        onFlip = { viewModel.flipCard() },
                        onRate = { viewModel.submitReview(it) }
                    )
                }
                is FlashcardReviewUiState.SessionComplete -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎉\n" + stringResource(R.string.session_complete),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF3D2B1F)
                        )
                        
                        val summaryText = if (state.reviewedCount > 0) {
                            "You reviewed ${state.reviewedCount} cards.\nNext review: ${state.dueTomorrowCount} cards due tomorrow."
                        } else {
                            stringResource(R.string.session_complete_msg)
                        }
                        
                        Text(
                            text = summaryText,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE88A68)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.back_to_decks), modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewContent(
    cards: List<com.ntando.ivu.network.Flashcard>,
    currentIndex: Int,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onRate: (String) -> Unit
) {
    val card = cards[currentIndex]
    
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "cardFlip"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress Indicator
        LinearProgressIndicator(
            progress = { (currentIndex + 1).toFloat() / cards.size },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = Color(0xFFE88A68),
            trackColor = Color(0xFFE0E0E0),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        Text(
            text = stringResource(R.string.review_progress_format, currentIndex + 1, cards.size),
            modifier = Modifier.padding(top = 12.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.weight(1f))

        // Flip Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
                .clickable { onFlip() },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                if (rotation <= 90f) {
                    // Front Text
                    Text(
                        text = card.frontText,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF3D2B1F)
                    )
                } else {
                    // Back Text (Rotated 180)
                    Text(
                        text = card.backText,
                        modifier = Modifier.graphicsLayer { rotationY = 180f },
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        color = Color(0xFFE88A68)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Rating Buttons
        if (isFlipped) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RatingButton(stringResource(R.string.rating_again), "again", Color(0xFFE57373), Modifier.weight(1f), onRate)
                RatingButton(stringResource(R.string.rating_hard), "hard", Color(0xFFFFB74D), Modifier.weight(1f), onRate)
                RatingButton(stringResource(R.string.rating_good), "good", Color(0xFF81C784), Modifier.weight(1f), onRate)
                RatingButton(stringResource(R.string.rating_easy), "easy", Color(0xFF64B5F6), Modifier.weight(1f), onRate)
            }
        } else {
            Text(
                text = stringResource(R.string.tap_to_flip),
                color = Color.Gray,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun RatingButton(
    label: String,
    rating: String,
    color: Color,
    modifier: Modifier,
    onRate: (String) -> Unit
) {
    Button(
        onClick = { onRate(rating) },
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
