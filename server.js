const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const path = require('path');
const { Chess } = require('chess.js');
const multer = require('multer');
const cloudinary = require('cloudinary').v2;

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
    cors: {
        origin: '*',
        methods: ['GET', 'POST']
    }
});

// Configure Cloudinary
cloudinary.config({
    cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
    api_key: process.env.CLOUDINARY_API_KEY,
    api_secret: process.env.CLOUDINARY_API_SECRET
});

// Configure Multer for in-memory buffer storage
const upload = multer({ storage: multer.memoryStorage() });

app.use(express.json({ limit: '10mb' }));
app.use((req, res, next) => {
    res.header('Access-Control-Allow-Origin', '*');
    res.header('Access-Control-Allow-Headers', 'Origin, X-Requested-With, Content-Type, Accept, Authorization');
    res.header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
    if (req.method === 'OPTIONS') return res.sendStatus(204);
    next();
});
app.use(express.static(path.join(__dirname, 'public')));

// In-Memory State
let notes = [];
let matches = [];
let tournaments = [];

// --- CHESS BOT EVALUATION & MINIMAX LOGIC ---
const PIECE_VALUES = { p: 10, n: 30, b: 35, r: 50, q: 90, k: 1000 };

const PAWN_TABLE = [
    [0,  0,  0,  0,  0,  0,  0,  0],
    [5,  5,  5,  5,  5,  5,  5,  5],
    [1,  1,  2,  3,  3,  2,  1,  1],
    [0,  0,  2,  5,  5,  2,  0,  0],
    [0,  0,  0,  4,  4,  0,  0,  0],
    [0, -1, -1,  2,  2, -1, -1,  0],
    [0,  1,  1, -2, -2,  1,  1,  0],
    [0,  0,  0,  0,  0,  0,  0,  0]
];

const KNIGHT_TABLE = [
    [-5, -4, -3, -3, -3, -3, -4, -5],
    [-4, -2,  0,  0,  0,  0, -2, -4],
    [-3,  0,  3,  4,  4,  3,  0, -3],
    [-3,  1,  4,  5,  5,  4,  1, -3],
    [-3,  0,  4,  5,  5,  4,  0, -3],
    [-3,  1,  3,  4,  4,  3,  1, -3],
    [-4, -2,  0,  1,  1,  0, -2, -4],
    [-5, -4, -3, -3, -3, -3, -4, -5]
];

function evaluateBoard(game) {
    let totalEvaluation = 0;
    const board = game.board();

    for (let r = 0; r < 8; r++) {
        for (let c = 0; c < 8; c++) {
            const piece = board[r][c];
            if (piece) {
                const val = PIECE_VALUES[piece.type];
                let posVal = 0;

                if (piece.type === 'p') posVal = piece.color === 'w' ? PAWN_TABLE[r][c] : PAWN_TABLE[7 - r][c];
                if (piece.type === 'n') posVal = piece.color === 'w' ? KNIGHT_TABLE[r][c] : KNIGHT_TABLE[7 - r][c];

                const score = val + posVal;
                totalEvaluation += (piece.color === 'w' ? score : -score);
            }
        }
    }
    return totalEvaluation;
}

function minimax(game, depth, alpha, beta, isMaximizing) {
    if (depth === 0 || game.isGameOver()) {
        return evaluateBoard(game);
    }

    const moves = game.moves({ verbose: true });

    if (isMaximizing) {
        let maxEval = -Infinity;
        for (let move of moves) {
            game.move(move);
            let evaluation = minimax(game, depth - 1, alpha, beta, false);
            game.undo();
            maxEval = Math.max(maxEval, evaluation);
            alpha = Math.max(alpha, evaluation);
            if (beta <= alpha) break;
        }
        return maxEval;
    } else {
        let minEval = Infinity;
        for (let move of moves) {
            game.move(move);
            let evaluation = minimax(game, depth - 1, alpha, beta, true);
            game.undo();
            minEval = Math.min(minEval, evaluation);
            beta = Math.min(beta, evaluation);
            if (beta <= alpha) break;
        }
        return minEval;
    }
}

function getHardBotMove(game) {
    const moves = game.moves({ verbose: true });
    let bestMove = null;
    let bestValue = Infinity;

    for (let move of moves) {
        game.move(move);
        let boardValue = minimax(game, 3, -Infinity, Infinity, true);
        game.undo();

        if (boardValue < bestValue) {
            bestValue = boardValue;
            bestMove = move;
        }
    }

    return bestMove || moves[Math.floor(Math.random() * moves.length)];
}

function makeBotMove(match) {
    if (!match.isBot || match.gameInstance.isGameOver()) return;

    if (match.gameInstance.turn() === 'b') {
        setTimeout(() => {
            const bestMove = getHardBotMove(match.gameInstance);
            if (!bestMove) return;

            const moveResult = match.gameInstance.move(bestMove);

            if (moveResult) {
                match.fen = match.gameInstance.fen();
                io.to(match.id).emit('move', {
                    matchId: match.id,
                    move: moveResult,
                    fen: match.fen
                });
            }
        }, 400);
    }
}

// Helpers
function toPublicMatch(match) {
    return {
        id: match.id,
        p1: match.p1,
        p2: match.p2,
        p1Joined: match.p1Joined,
        p2Joined: match.p2Joined,
        tournamentId: match.tournamentId || null,
        isBot: match.isBot || false,
        fen: match.fen || 'start'
    };
}

function createMatchRecord({ p1, p2, tournamentId = null, isBot = false }) {
    const gameInstance = new Chess();
    const newMatch = {
        id: 'match_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8),
        p1,
        p2: isBot ? '🤖 Grandmaster Bot (HARD)' : p2,
        p1Joined: false,
        p2Joined: isBot ? true : false,
        fen: 'start',
        tournamentId,
        isBot,
        gameInstance
    };

    matches.push(newMatch);
    broadcastMatches();
    return newMatch;
}

function broadcastMatches() {
    io.emit('init-data', matches.map(toPublicMatch));
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

function checkAdminAuth(reqBody) {
    const ADMIN_USER = process.env.ADMIN_USER || 'admin';
    const ADMIN_PASS = process.env.ADMIN_PASS || 'admin123';
    const user = reqBody.username || reqBody.user;
    const pass = reqBody.password || reqBody.pass;
    return user === ADMIN_USER && pass === ADMIN_PASS;
}

// --- API ROUTES ---

app.get('/api/health', (req, res) => {
    res.json({ status: 'ok', service: 'oav-hub-backend' });
});

// Gemini Study Buddy AI
app.post('/api/chat', async (req, res) => {
    const apiKey = process.env.GEMINI_API_KEY;
    const model = process.env.GEMINI_MODEL || 'gemini-1.5-flash';
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
                contents: [{
                    parts: [{ text: `System: You are Study Buddy, a helpful AI tutor for CBSE Class 9 to 12 students. Answer clearly in concise terms.\nUser Question: ${userPrompt}` }]
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

// Permanent Cloudinary PDF Uploads
app.get('/api/notes', (req, res) => {
    res.json({ success: true, notes });
});

app.post('/api/upload-note', upload.single('pdf'), async (req, res) => {
    if (!checkAdminAuth(req.body)) {
        return res.status(401).json({ success: false, message: 'Invalid Admin Credentials!' });
    }

    if (!req.file) {
        return res.status(400).json({ success: false, message: 'No PDF file selected!' });
    }

    try {
        const uploadStream = cloudinary.uploader.upload_stream(
            {
                resource_type: 'raw',
                folder: 'oav_hub_pdf_notes',
                public_id: `note_${Date.now()}_${req.file.originalname.replace(/[^a-zA-Z0-9]/g, '_')}`,
                format: 'pdf'
            },
            (error, result) => {
                if (error) {
                    console.error('Cloudinary Upload Error:', error);
                    return res.status(500).json({ success: false, message: 'Cloudinary upload failed.' });
                }

                const newNote = {
                    id: 'note_' + Date.now(),
                    class: req.body.classNum,
                    subject: req.body.subject,
                    title: req.body.title,
                    fileUrl: result.secure_url
                };

                notes.unshift(newNote);
                res.json({ success: true, note: newNote });
            }
        );

        uploadStream.end(req.file.buffer);
    } catch (err) {
        console.error(err);
        res.status(500).json({ success: false, message: 'Server error during PDF upload.' });
    }
});

app.delete('/api/delete-note/:id', (req, res) => {
    if (!checkAdminAuth(req.body)) {
        return res.status(401).json({ success: false, message: 'Invalid Admin Credentials!' });
    }

    const noteId = req.params.id;
    notes = notes.filter(n => n.id !== noteId);
    res.json({ success: true, message: 'Note deleted successfully.' });
});

// Chess Endpoints
app.get('/api/matches', (req, res) => {
    res.json({ matches: matches.map(toPublicMatch) });
});

app.post('/api/bot-match', (req, res) => {
    const { playerName } = req.body || {};
    const match = createMatchRecord({
        p1: playerName || 'Player',
        p2: '🤖 Grandmaster Bot (HARD)',
        isBot: true
    });
    res.status(201).json({ success: true, match: toPublicMatch(match) });
});

// --- SOCKET.IO EVENTS ---
io.on('connection', (socket) => {
    socket.emit('init-data', matches.map(toPublicMatch));

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
                io.to(data.matchId).emit('move', {
                    matchId: data.matchId,
                    move: moveResult,
                    fen: match.fen
                });

                if (match.isBot) {
                    makeBotMove(match);
                }
            }
        } catch (err) {
            console.error('Illegal move caught:', err.message);
        }
    });

    socket.on('disconnect', () => {
        releaseSeat(socket);
    });
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
    console.log(`🚀 Server running on http://localhost:${PORT}`);
});