package com.ntando.ivu.ui.quiz

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntando.ivu.R
import com.ntando.ivu.data.model.QuizDirection
import com.ntando.ivu.data.model.QuizMode
import com.ntando.ivu.data.model.QuizQuestionType
import com.ntando.ivu.ui.components.PronunciationButton
import com.ntando.ivu.util.SpeechManager
import com.ntando.ivu.viewmodel.MissedQuestion
import com.ntando.ivu.viewmodel.QuizUiState
import com.ntando.ivu.viewmodel.QuizViewModel

private const val TAG = "IVU_QUIZ"

/**
 * Main Composable layout for Quiz Mode hosting setup, active questions, and results views.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    viewModel: QuizViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLeaveConfirmationDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val speechManager = remember { SpeechManager(context) }
    DisposableEffect(Unit) {
        onDispose {
            speechManager.shutdown()
        }
    }

    // Intercept back presses mid-quiz to request exit confirmation
    if (uiState is QuizUiState.InProgress) {
        BackHandler {
            showLeaveConfirmationDialog = true
        }
    }

    if (showLeaveConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirmationDialog = false },
            title = { Text(stringResource(R.string.dialog_leave_quiz_title)) },
            text = { Text(stringResource(R.string.dialog_leave_quiz_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveConfirmationDialog = false
                    onBack()
                }) {
                    Text(stringResource(R.string.btn_leave), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirmationDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val titleText = when (val state = uiState) {
                        is QuizUiState.Setup -> state.deckTitle
                        is QuizUiState.InProgress -> state.deckTitle
                        is QuizUiState.Finished -> state.deckTitle
                        is QuizUiState.Empty -> state.deckTitle
                        else -> stringResource(R.string.title_quiz_mode)
                    }
                    Text(
                        text = titleText,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState is QuizUiState.InProgress) {
                                showLeaveConfirmationDialog = true
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (val state = uiState) {
                is QuizUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is QuizUiState.Empty -> {
                    QuizEmptyView(deckTitle = state.deckTitle, onBack = onBack)
                }
                is QuizUiState.Setup -> {
                    QuizSetupView(
                        state = state,
                        onUpdateSetup = { mode, direction, length ->
                            viewModel.updateSetup(mode, direction, length)
                        },
                        onStartQuiz = { viewModel.startQuiz() }
                    )
                }
                is QuizUiState.InProgress -> {
                    QuizQuestionView(
                        state = state,
                        speechManager = speechManager,
                        onAnswerChanged = { viewModel.onAnswerChanged(it) },
                        onSubmitAnswer = { viewModel.submitAnswer() },
                        onNextQuestion = { viewModel.nextQuestion() }
                    )
                }
                is QuizUiState.Finished -> {
                    QuizResultsView(
                        state = state,
                        speechManager = speechManager,
                        onRetryMissed = { viewModel.retryMissed() },
                        onPlayAgain = { viewModel.resetToSetup() },
                        onBack = onBack
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizEmptyView(
    deckTitle: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = deckTitle,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.empty_quiz_deck),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onBack,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.btn_back_to_deck))
        }
    }
}

@Composable
private fun QuizSetupView(
    state: QuizUiState.Setup,
    onUpdateSetup: (QuizMode, QuizDirection, Int) -> Unit,
    onStartQuiz: () -> Unit
) {
    var mode by remember(state) { mutableStateOf(state.selectedMode) }
    var direction by remember(state) { mutableStateOf(state.selectedDirection) }
    var length by remember(state) { mutableStateOf(state.selectedLength) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = stringResource(R.string.title_quiz_mode),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Question Type Selector
        Text(
            text = stringResource(R.string.label_theme),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            val isMcAvailable = state.isMultipleChoiceAvailable

            FilterChip(
                selected = mode == QuizMode.MULTIPLE_CHOICE,
                onClick = {
                    if (isMcAvailable) {
                        mode = QuizMode.MULTIPLE_CHOICE
                        onUpdateSetup(mode, direction, length)
                    }
                },
                enabled = isMcAvailable,
                label = { Text(stringResource(R.string.label_mode_multiple_choice)) },
                modifier = Modifier.padding(end = 8.dp)
            )
            FilterChip(
                selected = mode == QuizMode.TYPE_ANSWER,
                onClick = {
                    mode = QuizMode.TYPE_ANSWER
                    onUpdateSetup(mode, direction, length)
                },
                label = { Text(stringResource(R.string.label_mode_type_answer)) },
                modifier = Modifier.padding(end = 8.dp)
            )
            FilterChip(
                selected = mode == QuizMode.MIXED,
                onClick = {
                    mode = QuizMode.MIXED
                    onUpdateSetup(mode, direction, length)
                },
                label = { Text(stringResource(R.string.label_mode_mixed)) }
            )
        }

        if (!state.isMultipleChoiceAvailable) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.mc_disabled_explanation),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Direction Selector
        Text(
            text = stringResource(R.string.select_language),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            FilterChip(
                selected = direction == QuizDirection.FRONT_TO_BACK,
                onClick = {
                    direction = QuizDirection.FRONT_TO_BACK
                    onUpdateSetup(mode, direction, length)
                },
                label = { Text(stringResource(R.string.label_direction_front_to_back)) },
                modifier = Modifier.padding(end = 8.dp)
            )
            FilterChip(
                selected = direction == QuizDirection.BACK_TO_FRONT,
                onClick = {
                    direction = QuizDirection.BACK_TO_FRONT
                    onUpdateSetup(mode, direction, length)
                },
                label = { Text(stringResource(R.string.label_direction_back_to_front)) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Length Selector
        Text(
            text = stringResource(R.string.label_quiz_length),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            val maxCards = state.cardCount
            val lengths = listOf(5, 10, maxCards).distinct().sorted()

            lengths.forEach { l ->
                val labelText = if (l == maxCards) stringResource(R.string.label_length_all, l) else l.toString()
                FilterChip(
                    selected = length == l,
                    onClick = {
                        length = l
                        onUpdateSetup(mode, direction, length)
                    },
                    label = { Text(labelText) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onStartQuiz,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = stringResource(R.string.btn_start_quiz),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun QuizQuestionView(
    state: QuizUiState.InProgress,
    speechManager: SpeechManager,
    onAnswerChanged: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onNextQuestion: () -> Unit
) {
    val question = state.currentQuestion
    val progress = (state.questionIndex + 1).toFloat() / state.totalQuestions.toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        // Progress Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "${state.questionIndex + 1} of ${state.totalQuestions}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Question Prompt Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.deckTitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    PronunciationButton(
                        speechManager = speechManager,
                        textToSpeak = question.promptText,
                        languageCode = "en",
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = question.promptText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    softWrap = true,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Question Answers
        if (question.questionType == QuizQuestionType.MULTIPLE_CHOICE) {
            Column(modifier = Modifier.weight(1f)) {
                question.options.forEach { option ->
                    val isSelected = state.userAnswer == option
                    val normOption = option.trim().lowercase()
                    val normCorrect = question.correctAnswer.trim().lowercase()
                    val isOptionCorrect = normOption == normCorrect

                    val bgColor = when {
                        !state.isChecked -> if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                        isSelected && state.isCorrect -> Color(0xFFD3F2E6)
                        isSelected && !state.isCorrect -> Color(0xFFF2D3D3)
                        isOptionCorrect -> Color(0xFFD3F2E6)
                        else -> MaterialTheme.colorScheme.surface
                    }

                    val borderColor = when {
                        !state.isChecked -> if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        isSelected && state.isCorrect -> Color(0xFF2E7D32)
                        isSelected && !state.isCorrect -> Color(0xFFC62828)
                        isOptionCorrect -> Color(0xFF2E7D32)
                        else -> Color.Transparent
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                            .clickable(enabled = !state.isChecked) {
                                onAnswerChanged(option)
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = bgColor)
                    ) {
                        Text(
                            text = option,
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        } else {
            Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = state.userAnswer,
                    onValueChange = onAnswerChanged,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isChecked,
                    label = { Text(stringResource(R.string.label_mode_type_answer)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (state.userAnswer.isNotBlank() && !state.isChecked) {
                            onSubmitAnswer()
                        }
                    })
                )

                AnimatedVisibility(visible = state.isChecked) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        val bannerColor = if (state.isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                        val bannerText = if (state.isCorrect) stringResource(R.string.label_correct) else stringResource(R.string.label_incorrect)

                        Text(
                            text = bannerText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = bannerColor
                        )

                        if (!state.isCorrect) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.label_correct_answer_is, question.correctAnswer),
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }

        // Bottom Action Button
        if (!state.isChecked) {
            Button(
                onClick = onSubmitAnswer,
                enabled = state.userAnswer.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_check_answer),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Button(
                onClick = onNextQuestion,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_next_question),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun QuizResultsView(
    state: QuizUiState.Finished,
    speechManager: SpeechManager,
    onRetryMissed: () -> Unit,
    onPlayAgain: () -> Unit,
    onBack: () -> Unit
) {
    val percentage = if (state.totalQuestions > 0) {
        ((state.score.toFloat() / state.totalQuestions.toFloat()) * 100).toInt()
    } else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.label_quiz_completed),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${state.score} / ${state.totalQuestions}",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "$percentage%",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                if (state.xpEarned > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.label_xp_earned, state.xpEarned),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (state.missedQuestions.isNotEmpty()) {
            Text(
                text = stringResource(R.string.label_missed_questions),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(state.missedQuestions) { missed ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = missed.question.promptText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.label_correct_answer_is, missed.question.correctAnswer),
                                    fontSize = 14.sp,
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.weight(1f)
                                )
                                PronunciationButton(
                                    speechManager = speechManager,
                                    textToSpeak = missed.question.correctAnswer,
                                    languageCode = "en",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            if (missed.userAnswer.isNotBlank()) {
                                Text(
                                    text = stringResource(R.string.label_your_answer, missed.userAnswer),
                                    fontSize = 14.sp,
                                    color = Color(0xFFC62828)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            if (state.missedQuestions.isNotEmpty()) {
                Button(
                    onClick = onRetryMissed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.btn_retry_missed), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedButton(
                onClick = onPlayAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.btn_play_again), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_back_to_deck), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
