const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const path = require('path');
const { Chess } = require('chess.js');
const multer = require('multer');
const fs = require('fs');
const cloudinary = require('cloudinary').v2;
try { require('dotenv').config(); } catch (e) {}

const app = express();
const server = http.createServer(app);
const io = new Server(server);

// Configure Cloudinary for permanent storage
cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME || 'ymbcxm4f',
  api_key: process.env.CLOUDINARY_API_KEY || 'YOUR_API_KEY',
  api_secret: process.env.CLOUDINARY_API_SECRET || 'YOUR_API_SECRET'
});

// Multer temporary local upload setup
const upload = multer({ dest: 'uploads/' });

app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ limit: '50mb', extended: true }));
app.use(express.static(path.join(__dirname, 'public')));

// Persistent Local Notes Storage (notes.json fallback)
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

function getAdminCredentials() {
    const ADMIN_USER = String(process.env.ADMIN_USER || process.env.ADMIN_USERNAME || 'admin').trim();
    const ADMIN_PASS = String(process.env.ADMIN_PASS || process.env.ADMIN_PASSWORD || 'admin123').trim();
    return { ADMIN_USER, ADMIN_PASS };
}

// API: Fetch Notes
app.get('/api/notes', (req, res) => {
    res.json({ success: true, notes: notesDatabase });
});

// API: Admin Upload Note to Cloudinary
app.post('/api/upload-note', upload.single('pdf'), async (req, res) => {
    try {
        const { username, password, classNum, subject, title } = req.body;
        const { ADMIN_USER, ADMIN_PASS } = getAdminCredentials();

        if (username !== ADMIN_USER || password !== ADMIN_PASS) {
            if (req.file && fs.existsSync(req.file.path)) fs.unlinkSync(req.file.path);
            return res.status(401).json({ success: false, message: "Invalid Admin Credentials!" });
        }

        if (!req.file) {
            return res.status(400).json({ success: false, message: "No PDF file provided." });
        }

        // Clean title for public_id
        const safeTitle = String(title || 'study-note').trim().replace(/[^a-zA-Z0-9-_]+/g, '_').slice(0, 50) || 'note';

        // Upload to Cloudinary with simplified parameters
        const result = await cloudinary.uploader.upload(req.file.path, {
            resource_type: 'auto',
            folder: 'pdf_notes',
            public_id: `${safeTitle}_${Date.now()}`
        });

        // Delete temporary local file
        if (fs.existsSync(req.file.path)) {
            fs.unlinkSync(req.file.path);
        }

        const newNote = {
            id: 'note_' + Date.now(),
            class: classNum,
            subject: subject,
            title: title,
            fileUrl: result.secure_url
        };

        notesDatabase.unshift(newNote);
        saveNotes(notesDatabase);

        res.json({ success: true, note: newNote });
    } catch (err) {
        if (req.file && fs.existsSync(req.file.path)) fs.unlinkSync(req.file.path);
        console.error("Cloudinary Error Detail:", err);
        res.status(500).json({ success: false, message: "Server error during upload." });
    }
});

// API: Admin Delete Note
app.delete('/api/delete-note/:id', (req, res) => {
    const { username, password } = req.body;
    const noteId = req.params.id;
    const { ADMIN_USER, ADMIN_PASS } = getAdminCredentials();

    if (username !== ADMIN_USER || password !== ADMIN_PASS) {
        return res.status(401).json({ success: false, message: "Unauthorized Admin!" });
    }

    const noteIndex = notesDatabase.findIndex(n => n.id === noteId);
    if (noteIndex !== -1) {
        notesDatabase.splice(noteIndex, 1);
        saveNotes(notesDatabase);
        return res.json({ success: true, message: "Note deleted successfully!" });
    }

    res.status(404).json({ success: false, message: "Note not found." });
});

// API: Gemini AI Chat Endpoint
app.post('/api/chat', async (req, res) => {
    const apiKey = process.env.GEMINI_API_KEY || process.env.GOOGLE_API_KEY;
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
                generationConfig: { temperature: 0.3, topP: 0.9, maxOutputTokens: 140 },
                contents: [{
                    parts: [{ text: `System: You are Study Buddy, an encouraging AI tutor for CBSE Class 9 to 12 students. Answer clearly in 2-3 short sentences.\nUser Question: ${userPrompt}` }]
                }]
            })
        });

        const data = await response.json();
        const reply = data.candidates?.[0]?.content?.parts?.[0]?.text;

        if (reply) {
            return res.json({ success: true, reply });
        } else {
            return res.json({ success: false, reply: "No response generated." });
        }
    } catch (err) {
        return res.status(500).json({ success: false, reply: "Failed to connect to AI server." });
    }
});

// Socket.io Chess Lobby logic
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

// Start listening
const PORT = process.env.PORT || 3000;
server.listen(PORT, () => console.log(`🚀 Server running on http://localhost:${PORT}`));