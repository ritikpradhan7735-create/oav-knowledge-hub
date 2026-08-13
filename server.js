const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const path = require('path');
const { Chess } = require('chess.js');
const multer = require('multer');
const fs = require('fs');

const app = express();
const server = http.createServer(app);
const io = new Server(server);

// Ensure upload directory exists
const uploadDir = path.join(__dirname, 'public', 'uploads');
if (!fs.existsSync(uploadDir)) {
    fs.mkdirSync(uploadDir, { recursive: true });
}

// Multer Storage Configuration for PDF Uploads
const storage = multer.diskStorage({
    destination: (req, file, cb) => cb(null, uploadDir),
    filename: (req, file, cb) => cb(null, Date.now() + '-' + file.originalname.replace(/\s+/g, '_'))
});
const upload = multer({ storage });

app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// Persistent Local Notes Storage (notes.json)
const notesFilePath = path.join(__dirname, 'notes.json');

function loadNotes() {
    try {
        if (fs.existsSync(notesFilePath)) {
            const data = fs.readFileSync(notesFilePath, 'utf8');
            return JSON.parse(data);
        }
    } catch (err) {
        console.error("Error reading notes.json:", err);
    }
    return [];
}

function saveNotes(notes) {
    try {
        fs.writeFileSync(notesFilePath, JSON.stringify(notes, null, 2), 'utf8');
    } catch (err) {
        console.error("Error saving notes.json:", err);
    }
}

let notesDatabase = loadNotes();

// API: Fetch Notes
app.get('/api/notes', (req, res) => {
    res.json({ success: true, notes: notesDatabase });
});

// API: Admin Upload Note
app.post('/api/upload-note', upload.single('pdf'), (req, res) => {
    const { username, password, classNum, subject, title } = req.body;
    
    const ADMIN_USER = process.env.ADMIN_USER || "admin";
    const ADMIN_PASS = process.env.ADMIN_PASS || "admin123";

    if (username !== ADMIN_USER || password !== ADMIN_PASS) {
        return res.status(401).json({ success: false, message: "Invalid Admin Credentials!" });
    }

    if (!req.file) {
        return res.status(400).json({ success: false, message: "No PDF file provided." });
    }

    const newNote = {
        id: 'note_' + Date.now(),
        class: classNum,
        subject: subject,
        title: title,
        fileUrl: `/uploads/${req.file.filename}`
    };

    notesDatabase.unshift(newNote);
    saveNotes(notesDatabase);

    res.json({ success: true, note: newNote });
});

// API: Admin Delete Note
app.delete('/api/delete-note/:id', (req, res) => {
    const { username, password } = req.body;
    const noteId = req.params.id;

    const ADMIN_USER = process.env.ADMIN_USER || "admin";
    const ADMIN_PASS = process.env.ADMIN_PASS || "admin123";

    if (username !== ADMIN_USER || password !== ADMIN_PASS) {
        return res.status(401).json({ success: false, message: "Unauthorized Admin!" });
    }

    const noteIndex = notesDatabase.findIndex(n => n.id === noteId);
    if (noteIndex !== -1) {
        const deletedNote = notesDatabase.splice(noteIndex, 1)[0];

        if (deletedNote.fileUrl) {
            const filePath = path.join(__dirname, 'public', deletedNote.fileUrl);
            if (fs.existsSync(filePath)) {
                fs.unlinkSync(filePath);
            }
        }

        saveNotes(notesDatabase);
        return res.json({ success: true, message: "Note deleted successfully!" });
    }

    res.status(404).json({ success: false, message: "Note not found." });
});

// API: Gemini AI Chat Relay Endpoint
app.post('/api/chat', async (req, res) => {
    const apiKey = process.env.GEMINI_API_KEY || "AQ.Ab8RN6KTpX-r8OCBr4oUTcUaGQj8Ce6jAsSLBPhZGIFRRM_shA";
    const model = process.env.GEMINI_MODEL || 'gemini-3.1-flash-lite';
    const userPrompt = req.body.prompt;

    if (!userPrompt) {
        return res.status(400).json({ success: false, reply: "Please enter a prompt." });
    }

    try {
        const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;

        const response = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                generationConfig: {
                    temperature: 0.3,
                    topP: 0.9,
                    maxOutputTokens: 140
                },
                contents: [{
                    parts: [{
                        text: `System: You are Study Buddy, an encouraging AI tutor for CBSE Class 9 to 12 students. Answer clearly in 2-3 short sentences and avoid extra details.\nUser Question: ${userPrompt}`
                    }]
                }]
            })
        });

        const data = await response.json();

        if (data.error) {
            console.error("Gemini API Error:", data.error);
            return res.json({ success: false, reply: `API Error: ${data.error.message}` });
        }

        const reply = data.candidates?.[0]?.content?.parts?.[0]?.text;

        if (reply) {
            return res.json({ success: true, reply });
        } else {
            return res.json({ success: false, reply: "No text response generated." });
        }

    } catch (err) {
        console.error("Server Fetch Error:", err);
        return res.status(500).json({ success: false, reply: "Failed to connect to AI server." });
    }
});

// SOCKET.IO CHESS LOBBY
let matches = [];

function broadcastMatches() {
    io.emit('init-data', matches.map(({ id, p1, p2, p1Joined, p2Joined }) => ({ id, p1, p2, p1Joined, p2Joined })));
}

function releaseSeat(socket) {
    if (!socket.seatInfo) return;
    const { matchId, color } = socket.seatInfo;
    const match = matches.find(m => m.id === matchId);
    if (match) {
        if (color === 'w') match.p1Joined = false;
        if (color === 'b') match.p2Joined = false;
        broadcastMatches();
    }
    socket.seatInfo = null;
}

io.on('connection', (socket) => {
    socket.emit('init-data', matches.map(({ id, p1, p2, p1Joined, p2Joined }) => ({ id, p1, p2, p1Joined, p2Joined })));

    socket.on('claim-seat', ({ matchId, color }) => {
        const match = matches.find(m => m.id === matchId);
        if (!match) return;

        if (color === 'w') match.p1Joined = true;
        if (color === 'b') match.p2Joined = true;

        socket.seatInfo = { matchId, color };
        broadcastMatches();
    });

    socket.on('join-match', (id) => {
        socket.join(id);
        const match = matches.find(m => m.id === id);
        if (match) {
            socket.emit('match-state', { matchId: id, fen: match.gameInstance.fen() });
        }
    });

    socket.on('move', (data) => {
        const match = matches.find(m => m.id === data.matchId);
        if (!match) return;
        try {
            const moveResult = match.gameInstance.move({ 
                from: data.move.from, 
                to: data.move.to, 
                promotion: data.move.promotion || 'q' 
            });
            if (moveResult) {
                match.fen = match.gameInstance.fen();
                io.to(data.matchId).emit('move', { matchId: data.matchId, move: moveResult, fen: match.fen });
            }
        } catch (err) {}
    });

    socket.on('leave-match', (id) => {
        socket.leave(id);
        releaseSeat(socket);
    });

    socket.on('disconnect', () => {
        releaseSeat(socket);
    });
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => console.log(`🚀 Server running on http://localhost:${PORT}`));