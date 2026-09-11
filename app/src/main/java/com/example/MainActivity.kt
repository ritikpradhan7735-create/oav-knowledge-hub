package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.OavDrawerContent
import com.example.ui.components.OavTopBar
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SiteBg
import com.example.viewmodel.AppScreen
import com.example.viewmodel.OavViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: OavViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                OavAppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun OavAppRoot(viewModel: OavViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val allNotes by viewModel.filteredNotes.collectAsStateWithLifecycle()
    val bookmarkedNotes by viewModel.bookmarkedNotes.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val adminState by viewModel.adminState.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()
    val chessState by viewModel.chessState.collectAsStateWithLifecycle()
    val uploadMessage by viewModel.uploadMessage.collectAsStateWithLifecycle()
    val isUploading by viewModel.isUploading.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Back button handling
    BackHandler(enabled = drawerState.isOpen || currentScreen !is AppScreen.Home) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            when (currentScreen) {
                is AppScreen.PdfViewer -> {
                    val note = (currentScreen as AppScreen.PdfViewer).note
                    viewModel.navigateTo(AppScreen.NotesList(note.classLevel, note.subject))
                }
                is AppScreen.NotesList -> {
                    val cls = (currentScreen as AppScreen.NotesList).classLevel
                    viewModel.navigateTo(AppScreen.SubjectSelection(cls))
                }
                is AppScreen.SubjectSelection -> {
                    viewModel.navigateTo(AppScreen.ClassSelection)
                }
                else -> {
                    viewModel.navigateTo(AppScreen.Home)
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SiteBg,
                modifier = Modifier.fillMaxHeight()
            ) {
                OavDrawerContent(
                    currentScreen = currentScreen,
                    adminState = adminState,
                    onNavigate = { screen ->
                        viewModel.navigateTo(screen)
                    },
                    onCloseDrawer = {
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (currentScreen !is AppScreen.PdfViewer) {
                    OavTopBar(
                        adminState = adminState,
                        onMenuClick = {
                            scope.launch {
                                if (drawerState.isOpen) drawerState.close() else drawerState.open()
                            }
                        },
                        onHomeClick = {
                            viewModel.navigateTo(AppScreen.Home)
                        },
                        onSearchClick = {
                            viewModel.navigateTo(AppScreen.Home)
                        },
                        onAdminClick = {
                            viewModel.navigateTo(AppScreen.AdminPortal)
                        },
                        currentScreen = currentScreen
                    )
                }
            },
            containerColor = SiteBg
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                    when (screen) {
                        is AppScreen.Home -> {
                            HomeScreen(
                                notes = allNotes,
                                adminState = adminState,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                onNavigate = { viewModel.navigateTo(it) },
                                onToggleBookmark = { viewModel.toggleBookmark(it) },
                                onRefresh = { viewModel.refreshNotes() },
                                isRefreshing = isRefreshing
                            )
                        }
                        is AppScreen.ClassSelection -> {
                            ClassSelectionScreen(
                                notes = allNotes,
                                onSelectClass = { classNum ->
                                    viewModel.navigateTo(AppScreen.SubjectSelection(classNum))
                                },
                                onBack = { viewModel.navigateTo(AppScreen.Home) }
                            )
                        }
                        is AppScreen.SubjectSelection -> {
                            SubjectSelectionScreen(
                                classLevel = screen.classLevel,
                                notes = allNotes,
                                adminState = adminState,
                                onSelectSubject = { subject ->
                                    viewModel.navigateTo(AppScreen.NotesList(screen.classLevel, subject))
                                },
                                onSelectOtherClass = { otherCls ->
                                    viewModel.navigateTo(AppScreen.SubjectSelection(otherCls))
                                },
                                onOpenUploadModal = {
                                    viewModel.navigateTo(AppScreen.AdminPortal)
                                },
                                onBack = { viewModel.navigateTo(AppScreen.ClassSelection) }
                            )
                        }
                        is AppScreen.NotesList -> {
                            NotesListScreen(
                                classLevel = screen.classLevel,
                                subject = screen.subject,
                                notes = allNotes,
                                adminState = adminState,
                                onOpenNote = { note ->
                                    viewModel.navigateTo(AppScreen.PdfViewer(note))
                                },
                                onToggleBookmark = { viewModel.toggleBookmark(it) },
                                onDeleteNote = { noteId ->
                                    viewModel.deleteNote(noteId)
                                },
                                onBack = {
                                    viewModel.navigateTo(AppScreen.SubjectSelection(screen.classLevel))
                                }
                            )
                        }
                        is AppScreen.PdfViewer -> {
                            PdfViewerScreen(
                                note = screen.note,
                                onToggleBookmark = { viewModel.toggleBookmark(it) },
                                onBack = {
                                    viewModel.navigateTo(AppScreen.NotesList(screen.note.classLevel, screen.note.subject))
                                }
                            )
                        }
                        is AppScreen.Chess -> {
                            ChessScreen(
                                chessState = chessState,
                                onSquareClicked = { r, c -> viewModel.onChessSquareClicked(r, c) },
                                onResetGame = { viewModel.resetChessGame() },
                                onSetMode = { mode -> viewModel.setChessGameMode(mode) },
                                onToggleFlip = { viewModel.toggleChessBoardFlip() },
                                onBack = { viewModel.navigateTo(AppScreen.Home) }
                            )
                        }
                        is AppScreen.StudyBuddy -> {
                            StudyBuddyScreen(
                                messages = chatMessages,
                                isLoading = isChatLoading,
                                onSendMessage = { prompt -> viewModel.sendChatMessage(prompt) },
                                onClearChat = { viewModel.clearChat() },
                                onBack = { viewModel.navigateTo(AppScreen.Home) }
                            )
                        }
                        is AppScreen.Bookmarks -> {
                            BookmarksScreen(
                                bookmarkedNotes = bookmarkedNotes,
                                onOpenNote = { note -> viewModel.navigateTo(AppScreen.PdfViewer(note)) },
                                onToggleBookmark = { viewModel.toggleBookmark(it) },
                                onBack = { viewModel.navigateTo(AppScreen.Home) }
                            )
                        }
                        is AppScreen.AdminPortal -> {
                            AdminScreen(
                                adminState = adminState,
                                allNotes = allNotes,
                                isUploading = isUploading,
                                uploadMessage = uploadMessage,
                                onLogin = { user, pass -> viewModel.loginAdmin(user, pass) },
                                onLogout = { viewModel.logoutAdmin() },
                                onUploadNote = { cls, subj, title, url ->
                                    viewModel.uploadNote(cls, subj, title, url, null)
                                },
                                onDeleteNote = { noteId -> viewModel.deleteNote(noteId) },
                                onClearUploadMessage = { viewModel.clearUploadMessage() },
                                onBack = { viewModel.navigateTo(AppScreen.Home) }
                            )
                        }
                    }
                }
            }
        }
    }
}
