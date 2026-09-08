package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.ChessGameState
import com.example.chess.ChessLogic
import com.example.chess.GameMode
import com.example.chess.GameStatus
import com.example.chess.PieceColor
import com.example.chess.Position
import com.example.data.local.NoteEntity
import com.example.data.model.ChatMessage
import com.example.data.repository.OavRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed interface AppScreen {
    data object Home : AppScreen
    data object ClassSelection : AppScreen
    data class SubjectSelection(val classLevel: String) : AppScreen
    data class NotesList(val classLevel: String, val subject: String) : AppScreen
    data class PdfViewer(val note: NoteEntity) : AppScreen
    data object Chess : AppScreen
    data object StudyBuddy : AppScreen
    data object Bookmarks : AppScreen
    data object AdminPortal : AppScreen
}

data class AdminState(
    val isLoggedIn: Boolean = false,
    val username: String = "",
    val token: String? = null
)

class OavViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = OavRepository(application)

    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Home)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _adminState = MutableStateFlow(AdminState())
    val adminState: StateFlow<AdminState> = _adminState.asStateFlow()

    val allNotes: StateFlow<List<NoteEntity>> = repository.allNotesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkedNotes: StateFlow<List<NoteEntity>> = repository.bookmarkedNotesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredNotes: StateFlow<List<NoteEntity>> = combine(allNotes, _searchQuery) { notes, query ->
        if (query.isBlank()) notes
        else notes.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.subject.contains(query, ignoreCase = true) ||
            it.classLevel.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Chatbot State
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "Welcome to OAV Study Buddy! Ask me anything about CBSE Classes 9-12 subjects, formulas, exams, or chapter summaries.",
                isUser = false
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    // Chess State
    private val _chessState = MutableStateFlow(ChessGameState())
    val chessState: StateFlow<ChessGameState> = _chessState.asStateFlow()
    private var chessTimerJob: Job? = null

    // Upload & Admin State
    private val _uploadMessage = MutableStateFlow<String?>(null)
    val uploadMessage: StateFlow<String?> = _uploadMessage.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    init {
        refreshNotes()
        startChessTimer()
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refreshNotes() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refreshNotes()
            _isRefreshing.value = false
        }
    }

    fun toggleBookmark(note: NoteEntity) {
        viewModelScope.launch {
            repository.toggleBookmark(note.id, !note.isBookmarked)
        }
    }

    // --- Study Buddy Chat ---
    fun sendChatMessage(prompt: String) {
        if (prompt.isBlank()) return
        val userMsg = ChatMessage(text = prompt.trim(), isUser = true)
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _isChatLoading.value = true
            val reply = repository.askStudyBuddy(prompt.trim())
            _chatMessages.value = _chatMessages.value + ChatMessage(text = reply, isUser = false)
            _isChatLoading.value = false
        }
    }

    fun clearChat() {
        _chatMessages.value = listOf(
            ChatMessage(
                text = "Chat cleared. What would you like to study next?",
                isUser = false
            )
        )
    }

    // --- Admin Authentication & Operations ---
    fun loginAdmin(user: String, pass: String): Boolean {
        if (user.isNotBlank() && pass.isNotBlank()) {
            _adminState.value = AdminState(isLoggedIn = true, username = user)
            return true
        }
        return false
    }

    fun logoutAdmin() {
        _adminState.value = AdminState(isLoggedIn = false, username = "")
    }

    fun uploadNote(classNum: String, subject: String, title: String, directUrl: String?, file: File?) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadMessage.value = null
            val admin = _adminState.value
            val res = repository.uploadNote(
                authUser = admin.username,
                authPass = "admin123",
                classNum = classNum,
                subject = subject,
                title = title,
                file = file,
                directUrl = directUrl
            )
            _isUploading.value = false
            if (res.isSuccess) {
                _uploadMessage.value = "Note '${title}' uploaded successfully!"
                refreshNotes()
            } else {
                _uploadMessage.value = "Failed to upload note: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            val admin = _adminState.value
            repository.deleteNote(noteId, admin.username, "admin123")
            refreshNotes()
        }
    }

    fun clearUploadMessage() {
        _uploadMessage.value = null
    }

    // --- Chess Game Operations ---
    fun onChessSquareClicked(row: Int, col: Int) {
        val current = _chessState.value
        if (current.gameStatus == GameStatus.CHECKMATE || current.gameStatus == GameStatus.STALEMATE) return

        val clickedPos = Position(row, col)
        val selected = current.selectedPos

        if (selected == null) {
            // Select piece if it belongs to current player
            val piece = current.board[row][col]
            if (piece != null && piece.color == current.turn) {
                val legalMoves = ChessLogic.getLegalMoves(current.board, clickedPos)
                _chessState.value = current.copy(selectedPos = clickedPos, validMoves = legalMoves)
            }
        } else {
            // If clicked on another piece of same color, reselect
            val piece = current.board[row][col]
            if (piece != null && piece.color == current.turn) {
                val legalMoves = ChessLogic.getLegalMoves(current.board, clickedPos)
                _chessState.value = current.copy(selectedPos = clickedPos, validMoves = legalMoves)
                return
            }

            // Check if move is legal
            if (current.validMoves.contains(clickedPos)) {
                val newState = ChessLogic.applyMove(current, selected, clickedPos)
                _chessState.value = newState

                // Check if AI turn in single player
                if (newState.gameMode in listOf(GameMode.VS_AI_EASY, GameMode.VS_AI_MEDIUM, GameMode.VS_AI_HARD)
                    && newState.turn == PieceColor.BLACK
                    && newState.gameStatus == GameStatus.IN_PROGRESS
                ) {
                    viewModelScope.launch {
                        delay(600)
                        val aiMove = ChessLogic.computeAiMove(_chessState.value)
                        if (aiMove != null) {
                            _chessState.value = ChessLogic.applyMove(_chessState.value, aiMove.first, aiMove.second)
                        }
                    }
                }
            } else {
                _chessState.value = current.copy(selectedPos = null, validMoves = emptyList())
            }
        }
    }

    fun resetChessGame() {
        _chessState.value = ChessGameState(gameMode = _chessState.value.gameMode)
    }

    fun setChessGameMode(mode: GameMode) {
        _chessState.value = ChessGameState(gameMode = mode)
    }

    fun toggleChessBoardFlip() {
        _chessState.value = _chessState.value.copy(isFlipped = !_chessState.value.isFlipped)
    }

    private fun startChessTimer() {
        chessTimerJob?.cancel()
        chessTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _chessState.value
                if (current.gameStatus == GameStatus.IN_PROGRESS) {
                    if (current.turn == PieceColor.WHITE && current.whiteTimeSeconds > 0) {
                        _chessState.value = current.copy(whiteTimeSeconds = current.whiteTimeSeconds - 1)
                    } else if (current.turn == PieceColor.BLACK && current.blackTimeSeconds > 0) {
                        _chessState.value = current.copy(blackTimeSeconds = current.blackTimeSeconds - 1)
                    }
                }
            }
        }
    }
}
