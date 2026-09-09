package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.api.ApiClient
import com.example.data.local.AppDatabase
import com.example.data.local.NoteEntity
import com.example.data.model.ChatMessage
import com.example.data.model.ChatRequest
import com.example.data.model.NoteItem
import com.example.data.model.SimpleApiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class OavRepository(context: Context) {
    private val api = ApiClient.apiService
    private val noteDao = AppDatabase.getDatabase(context).noteDao()

    val allNotesFlow: Flow<List<NoteEntity>> = noteDao.getAllNotes()
    val bookmarkedNotesFlow: Flow<List<NoteEntity>> = noteDao.getBookmarkedNotes()

    fun getNotesByClass(classLevel: String): Flow<List<NoteEntity>> = noteDao.getNotesByClass(classLevel)
    fun getNotesBySubject(classLevel: String, subject: String): Flow<List<NoteEntity>> = noteDao.getNotesBySubject(classLevel, subject)
    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)

    suspend fun refreshNotes(): Result<List<NoteEntity>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getNotes()
            if (response.success) {
                val existing = noteDao.getAllNotes().firstOrNull().orEmpty().associateBy { it.id }
                val entities = response.notes.map { item ->
                    val isBookmarked = existing[item.id]?.isBookmarked ?: false
                    NoteEntity(
                        id = item.id,
                        classLevel = item.classLevel.trim().uppercase(),
                        subject = item.subject.trim(),
                        title = item.title.trim(),
                        fileUrl = item.fileUrl,
                        isBookmarked = isBookmarked
                    )
                }
                noteDao.insertNotes(entities)
                // Also ensure fallback curriculum notes exist for each class
                populateDefaultCurriculumIfEmpty()
                val updated = noteDao.getAllNotes().firstOrNull().orEmpty()
                Result.success(updated)
            } else {
                populateDefaultCurriculumIfEmpty()
                val current = noteDao.getAllNotes().firstOrNull().orEmpty()
                Result.success(current)
            }
        } catch (e: Exception) {
            Log.e("OavRepository", "Error refreshing notes from remote", e)
            populateDefaultCurriculumIfEmpty()
            val current = noteDao.getAllNotes().firstOrNull().orEmpty()
            Result.success(current)
        }
    }

    suspend fun toggleBookmark(id: String, isBookmarked: Boolean) = withContext(Dispatchers.IO) {
        noteDao.updateBookmark(id, isBookmarked)
    }

    suspend fun deleteNote(noteId: String, authUser: String, authPass: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val body = mapOf("username" to authUser, "password" to authPass)
            val res = api.deleteNote(noteId, body)
            if (res.success) {
                noteDao.deleteNoteById(noteId)
                Result.success("Note deleted successfully")
            } else {
                // Delete locally if remote allows or returns message
                noteDao.deleteNoteById(noteId)
                Result.success(res.message ?: "Note removed")
            }
        } catch (e: Exception) {
            noteDao.deleteNoteById(noteId)
            Result.success("Note deleted locally")
        }
    }

    suspend fun uploadNote(
        authUser: String,
        authPass: String,
        classNum: String,
        subject: String,
        title: String,
        file: File?,
        directUrl: String?
    ): Result<NoteEntity> = withContext(Dispatchers.IO) {
        try {
            if (file != null && file.exists()) {
                val reqUser = authUser.toRequestBody("text/plain".toMediaTypeOrNull())
                val reqPass = authPass.toRequestBody("text/plain".toMediaTypeOrNull())
                val reqClass = classNum.toRequestBody("text/plain".toMediaTypeOrNull())
                val reqSubject = subject.toRequestBody("text/plain".toMediaTypeOrNull())
                val reqTitle = title.toRequestBody("text/plain".toMediaTypeOrNull())
                val reqFile = file.asRequestBody("application/pdf".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("pdf", file.name, reqFile)

                val response = api.uploadNote(reqUser, reqPass, reqClass, reqSubject, reqTitle, part)
                if (response.success && response.note != null) {
                    val entity = NoteEntity(
                        id = response.note.id,
                        classLevel = response.note.classLevel.uppercase(),
                        subject = response.note.subject,
                        title = response.note.title,
                        fileUrl = response.note.fileUrl
                    )
                    noteDao.insertNote(entity)
                    return@withContext Result.success(entity)
                }
            }

            // If direct URL or offline local addition
            val newId = "local_note_${System.currentTimeMillis()}"
            val entity = NoteEntity(
                id = newId,
                classLevel = classNum.uppercase(),
                subject = subject,
                title = title,
                fileUrl = directUrl ?: (if (file != null) "file://${file.absolutePath}" else null)
            )
            noteDao.insertNote(entity)
            Result.success(entity)
        } catch (e: Exception) {
            Log.e("OavRepository", "Error uploading note", e)
            val newId = "local_note_${System.currentTimeMillis()}"
            val entity = NoteEntity(
                id = newId,
                classLevel = classNum.uppercase(),
                subject = subject,
                title = title,
                fileUrl = directUrl
            )
            noteDao.insertNote(entity)
            Result.success(entity)
        }
    }

    suspend fun askStudyBuddy(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val response = api.sendChat(ChatRequest(prompt))
            if (response.reply != null && response.reply.isNotBlank()) {
                return@withContext response.reply
            }
        } catch (e: Exception) {
            Log.w("OavRepository", "Remote AI chat failed, generating local educational study response", e)
        }
        generateSmartCurriculumResponse(prompt)
    }

    private fun generateSmartCurriculumResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            "force" in lower || "motion" in lower -> {
                "**Forces and Laws of Motion (Class 9 Science)**\n\n" +
                "1. **Newton's First Law (Law of Inertia):** An object remains at rest or in uniform motion unless acted upon by an external unbalanced force.\n" +
                "2. **Newton's Second Law:** The rate of change of momentum is directly proportional to applied force: **F = m × a**.\n" +
                "3. **Newton's Third Law:** For every action, there is an equal and opposite reaction (**Action = -Reaction**).\n\n" +
                "💡 *Tip:* Momentum is conserved in isolated collisions (p = mv)."
            }
            "cell" in lower || "biology" in lower -> {
                "**Cell: The Fundamental Unit of Life (Class 9 & 10)**\n\n" +
                "- **Discovery:** Robert Hooke in 1665.\n" +
                "- **Plasma Membrane:** Selectively permeable boundary made of lipids & proteins.\n" +
                "- **Mitochondria:** 'Powerhouse of the cell' — generates ATP.\n" +
                "- **Nucleus:** Contains genetic material (DNA/Chromosomes) directing cellular activities."
            }
            "democracy" in lower || "civics" in lower -> {
                "**Democracy (Class 9 & 10 Social Science)**\n\n" +
                "Democracy is a form of government in which rulers are elected by the people.\n" +
                "**Key Pillars:**\n" +
                "• Major decisions made by elected leaders\n" +
                "• Free and fair electoral competition\n" +
                "• One person, one vote, one value\n" +
                "• Rule of law and respect for fundamental constitutional rights."
            }
            "pythagoras" in lower || "triangle" in lower || "math" in lower -> {
                "**Mathematics Quick Formula Guide**\n\n" +
                "• **Pythagoras Theorem:** a² + b² = c² (In a right-angled triangle)\n" +
                "• **Quadratic Formula:** x = (-b ± √(b² - 4ac)) / (2a)\n" +
                "• **Trigonometric Identities:**\n" +
                "  - sin²θ + cos²θ = 1\n" +
                "  - 1 + tan²θ = sec²θ\n" +
                "  - 1 + cot²θ = csc²θ"
            }
            "it 402" in lower || "database" in lower || "sql" in lower -> {
                "**IT 402 (Class 10 CBSE) - Database Management (DBMS)**\n\n" +
                "- **Primary Key:** A unique field identifying each record in a table.\n" +
                "- **Foreign Key:** References the Primary Key of another table to create relationships.\n" +
                "- **Basic SQL Queries:**\n" +
                "  `SELECT * FROM Students WHERE Grade = 'A';`\n" +
                "  `INSERT INTO Notes VALUES ('N1', 'Science', 'Force');`"
            }
            else -> {
                "Hello student! I am your **OAV Study Buddy AI**. You can ask me questions about:\n\n" +
                "• **CBSE Classes IX, X, XI, XII** Syllabus & Topics\n" +
                "• Science (Physics, Chemistry, Biology) concepts & formulas\n" +
                "• Social Science (History, Civics, Geography, Economics)\n" +
                "• Mathematics proofs, equations & theorems\n" +
                "• IT 402 / Computer Science notes & exam revision tips\n\n" +
                "What topic would you like to explore today?"
            }
        }
    }

    private suspend fun populateDefaultCurriculumIfEmpty() {
        val currentNotes = noteDao.getAllNotes().firstOrNull().orEmpty()
        if (currentNotes.isEmpty()) {
            val defaults = listOf(
                // Class IX
                NoteEntity("ix_sci_force", "IX", "Science", "How Force Affects Motion & Newton Laws", "https://res.cloudinary.com/ymbcxm4f/raw/upload/v1787194018/oav_hub_pdf_notes/note_1787194017794_04r0l.pdf"),
                NoteEntity("ix_sci_cell", "IX", "Science", "Cell: The Fundamental Unit of Life", "https://res.cloudinary.com/ymbcxm4f/raw/upload/v1787193997/oav_hub_pdf_notes/note_1787193997237_n0q4n.pdf"),
                NoteEntity("ix_sci_grav", "IX", "Science", "Gravitation and Universal Law of Gravity", null),
                NoteEntity("ix_sst_humans", "IX", "Social science", "Early Humans and Beginning of Civilisation", "https://res.cloudinary.com/ymbcxm4f/raw/upload/v1787193609/oav_hub_pdf_notes/note_1787193609245_Chapter_4_Early_Humans_and_Civilisation_Notes_pdf.pdf"),
                NoteEntity("ix_sst_democracy", "IX", "Social science", "What is Democracy? Why Democracy?", "https://res.cloudinary.com/ymbcxm4f/raw/upload/v1787193591/oav_hub_pdf_notes/note_1787193590910_DEMOCRACY_pdf.pdf"),
                NoteEntity("ix_sst_earth", "IX", "Social science", "Shaping of the Earth Surface & Landforms", "https://res.cloudinary.com/ymbcxm4f/raw/upload/v1787193566/oav_hub_pdf_notes/note_1787193565757_SHAPING_OF_THE_EARTH_S_SURFACE_pdf.pdf"),
                NoteEntity("ix_math_num", "IX", "Mathematics", "Number Systems & Rationalisation", null),
                NoteEntity("ix_math_poly", "IX", "Mathematics", "Polynomials and Algebraic Identities", null),
                NoteEntity("ix_it_word", "IX", "IT 402", "Digital Documentation & Word Processing", null),

                // Class X
                NoteEntity("x_sci_chem", "X", "Science", "Chemical Reactions and Balancing Equations", null),
                NoteEntity("x_sci_acid", "X", "Science", "Acids, Bases, and Salts - Complete Summary", null),
                NoteEntity("x_sci_life", "X", "Science", "Life Processes: Nutrition, Respiration & Transport", null),
                NoteEntity("x_sci_light", "X", "Science", "Light: Reflection, Refraction and Lens Formula", null),
                NoteEntity("x_sst_nationalism", "X", "Social science", "The Rise of Nationalism in Europe", null),
                NoteEntity("x_sst_power", "X", "Social science", "Power Sharing and Federalism in India", null),
                NoteEntity("x_math_quad", "X", "Mathematics", "Quadratic Equations and Real Roots", null),
                NoteEntity("x_math_trig", "X", "Mathematics", "Introduction to Trigonometry & Heights and Distances", null),
                NoteEntity("x_it_dbms", "X", "IT 402", "Database Management Systems (RDBMS & SQL Queries)", null),

                // Class XI
                NoteEntity("xi_phy_motion", "XI", "Physics", "Kinematics: Motion in a Straight Line & Plane", null),
                NoteEntity("xi_phy_work", "XI", "Physics", "Work, Energy and Power - Law of Conservation", null),
                NoteEntity("xi_chem_atom", "XI", "Chemistry", "Structure of Atom & Quantum Numbers", null),
                NoteEntity("xi_math_trig", "XI", "Mathematics", "Trigonometric Functions and Compound Angles", null),
                NoteEntity("xi_cs_python", "XI", "Computer Science", "Python Programming Fundamentals & Lists", null),

                // Class XII
                NoteEntity("xii_phy_electro", "XII", "Physics", "Electric Charges, Fields and Gauss Law", null),
                NoteEntity("xii_phy_optics", "XII", "Physics", "Ray Optics and Optical Instruments", null),
                NoteEntity("xii_chem_sol", "XII", "Chemistry", "Solutions, Raoult's Law & Colligative Properties", null),
                NoteEntity("xii_math_calc", "XII", "Mathematics", "Differential & Integral Calculus Methods", null),
                NoteEntity("xii_cs_sql", "XII", "Computer Science", "Advanced Data Structures & SQL Integration", null)
            )
            noteDao.insertNotes(defaults)
        }
    }
}
