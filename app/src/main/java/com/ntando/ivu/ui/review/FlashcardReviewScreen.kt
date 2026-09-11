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
import androidx.compose.ui.draw.clip
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
    var badgeToShow by remember { mutableStateOf<com.ntando.ivu.data.entity.Badge?>(null) }

    LaunchedEffect(deckId) {
        viewModel.loadDueCards(deckId)
    }

    // Handle badge unlock feedback
    LaunchedEffect(uiState) {
        val state = uiState
        if (state is FlashcardReviewUiState.Success && state.newlyUnlockedBadges.isNotEmpty()) {
            badgeToShow = state.newlyUnlockedBadges.first()
        } else if (state is FlashcardReviewUiState.SessionComplete && state.newlyUnlockedBadges.isNotEmpty()) {
            badgeToShow = state.newlyUnlockedBadges.first()
        }
    }

    if (badgeToShow != null) {
        AlertDialog(
            onDismissRequest = { 
                badgeToShow = null 
                viewModel.clearUnlockedBadges()
            },
            title = { Text("🎉 Badge Unlocked!") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("🏅", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(badgeToShow?.displayName ?: "", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(badgeToShow?.description ?: "", textAlign = TextAlign.Center)
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        badgeToShow = null 
                        viewModel.clearUnlockedBadges()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE88A68))
                ) {
                    Text("Awesome!")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val state = uiState
                    if (state is FlashcardReviewUiState.Success) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LinearProgressIndicator(
                                progress = { (state.currentIndex + 1).toFloat() / state.cards.size },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFF7FB6A7),
                                trackColor = Color(0xFFEEEEEE)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "${state.currentIndex + 1}/${state.cards.size}",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
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
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        
                        val summaryText = if (state.reviewedCount > 0) {
                            stringResource(R.string.review_summary_format, state.reviewedCount, state.dueTomorrowCount)
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
        Spacer(modifier = Modifier.weight(1f))

        // Flip Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
                .clickable { onFlip() },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                if (rotation <= 90f) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = card.frontText,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF3D2B1F)
                        )
                        Text(
                            text = "(isiZulu)", // Should ideally be dynamic
                            fontSize = 16.sp,
                            color = Color.LightGray,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                } else {
                    // Back Text (Rotated 180)
                    Text(
                        text = card.backText,
                        modifier = Modifier.graphicsLayer { rotationY = 180f },
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        color = Color(0xFFE88A68)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Surface(
            modifier = Modifier.size(48.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = Color(0xFF7FB6A7)
        ) {}
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (!isFlipped) stringResource(R.string.tap_to_reveal) else stringResource(R.string.tap_to_flip),
            color = Color.LightGray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        // Rating Buttons
        if (isFlipped) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RatingButtonCol(stringResource(R.string.rating_again), "1m", "again", Color(0xFFF2D3D3), Modifier.weight(1f), onRate)
                RatingButtonCol(stringResource(R.string.rating_hard), "6m", "hard", Color(0xFFF2E6D3), Modifier.weight(1f), onRate)
                RatingButtonCol(stringResource(R.string.rating_good), "1d", "good", Color(0xFFD3F2E6), Modifier.weight(1f), onRate)
                RatingButtonCol(stringResource(R.string.rating_easy), "4d", "easy", Color(0xFFF2DCD3), Modifier.weight(1f), onRate)
            }
        }
    }
}

@Composable
fun RatingButtonCol(
    label: String,
    time: String,
    rating: String,
    color: Color,
    modifier: Modifier,
    onRate: (String) -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { onRate(rating) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color(0xFF3D2B1F)),
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(time, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
