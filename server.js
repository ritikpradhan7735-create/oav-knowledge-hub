require("dotenv").config();
const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const path = require('path');
const fs = require('fs');
const { Chess } = require('chess.js');
const multer = require('multer');
const cloudinary = require('cloudinary').v2;

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
  cors: { origin: '*', methods: ['GET', 'POST'] }
});

const PORT = process.env.PORT || 3000;
const DATA = path.join(__dirname, 'notes.json');
const UPLOADS = path.join(__dirname, 'uploads');

fs.mkdirSync(UPLOADS, { recursive: true });

cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
  api_key: process.env.CLOUDINARY_API_KEY,
  api_secret: process.env.CLOUDINARY_API_SECRET
});

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 20 * 1024 * 1024 }
});

app.use(express.json({ limit: '10mb' }));
app.use((req, res, next) => {
  res.header('Access-Control-Allow-Origin', '*');
  res.header('Access-Control-Allow-Headers', 'Origin, X-Requested-With, Content-Type, Accept, Authorization');
  res.header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  if (req.method === 'OPTIONS') return res.sendStatus(204);
  next();
});

app.use(express.static(path.join(__dirname, 'public')));
app.use('/uploads', express.static(UPLOADS));

const adminOk = (body = {}) => {
  const user = body.username || body.user;
  const pass = body.password || body.pass;
  return !!(
    process.env.ADMIN_USER &&
    process.env.ADMIN_PASS &&
    user === process.env.ADMIN_USER &&
    pass === process.env.ADMIN_PASS
  );
};

const cloudReady = () =>
  !!(
    process.env.CLOUDINARY_CLOUD_NAME &&
    process.env.CLOUDINARY_API_KEY &&
    process.env.CLOUDINARY_API_SECRET
  );

const readLocal = () => {
  try {
    return JSON.parse(fs.readFileSync(DATA, 'utf8'));
  } catch {
    return [];
  }
};

const writeLocal = (notes) => {
  fs.writeFileSync(DATA, JSON.stringify(notes, null, 2));
};

// -------------------- OAV HUB API --------------------
app.post('/api/admin/login', (req, res) => {
  if (!process.env.ADMIN_USER || !process.env.ADMIN_PASS) {
    return res.status(500).json({
      success: false,
      message: 'Admin credentials are not configured on the server.'
    });
  }

  if (adminOk(req.body)) {
    return res.json({ success: true });
  }

  return res.status(401).json({
    success: false,
    message: 'Invalid admin username or password.'
  });
});


app.get('/api/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'OAV Hub',
    cloudinary: cloudReady()
  });
});

app.get('/api/notes', async (req, res) => {
  if (cloudReady()) {
    try {
      const result = await cloudinary.search
        .expression('resource_type:raw AND folder:oav_hub_pdf_notes')
        .with_field('context')
        .sort_by('created_at', 'desc')
        .max_results(500)
        .execute();

      const notes = result.resources.map((file) => {
        const ctx = file.context || {};
        return {
          id: file.public_id,
          class: ctx.classNum || 'IX',
          subject: ctx.subject || 'General',
          title: ctx.title || file.filename,
          fileUrl: file.secure_url
        };
      });

      return res.json({ success: true, notes });
    } catch (err) {
      console.error('Cloudinary notes error:', err.message);
    }
  }

  res.json({ success: true, notes: readLocal() });
});

app.post('/api/upload-note', upload.single('pdf'), async (req, res) => {
  if (!adminOk(req.body)) {
    return res.status(401).json({
      success: false,
      message: 'Invalid admin credentials. Set ADMIN_USER and ADMIN_PASS in Render Environment.'
    });
  }

  if (!req.file || !/pdf$/i.test(req.file.originalname)) {
    return res.status(400).json({
      success: false,
      message: 'Please select a PDF file.'
    });
  }

  const note = {
    id: `note_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`,
    class: req.body.classNum || 'IX',
    subject: (req.body.subject || 'General').trim(),
    title: (req.body.title || req.file.originalname).trim()
  };

  try {
    if (cloudReady()) {
      const result = await new Promise((resolve, reject) => {
        const stream = cloudinary.uploader.upload_stream(
          {
            resource_type: 'raw',
            folder: 'oav_hub_pdf_notes',
            public_id: note.id,
            format: 'pdf',
            context: {
              classNum: note.class,
              subject: note.subject,
              title: note.title
            }
          },
          (error, result) => (error ? reject(error) : resolve(result))
        );

        stream.end(req.file.buffer);
      });

      note.id = result.public_id;
      note.fileUrl = result.secure_url;
    } else {
      const filename = `${note.id}.pdf`;
      fs.writeFileSync(path.join(UPLOADS, filename), req.file.buffer);
      note.fileUrl = `/uploads/${filename}`;

      const notes = readLocal();
      notes.unshift(note);
      writeLocal(notes);
    }

    res.json({ success: true, note });
  } catch (err) {
    console.error('Upload error:', err);
    res.status(500).json({
      success: false,
      message: 'Upload failed. Check storage configuration.'
    });
  }
});

// Open a PDF in the browser instead of forcing Cloudinary raw files to download.
// The note id is passed in the query string because Cloudinary public IDs contain '/'.
app.get('/api/view-note', async (req, res) => {
  const id = String(req.query.id || '').trim();
  if (!id) return res.status(400).send('Missing note id.');

  try {
    if (cloudReady()) {
      const resource = await cloudinary.api.resource(id, { resource_type: 'raw' });
      const response = await fetch(resource.secure_url);
      if (!response.ok) throw new Error(`Cloudinary returned ${response.status}`);

      res.setHeader('Content-Type', 'application/pdf');
      res.setHeader('Content-Disposition', 'inline');
      res.setHeader('Cache-Control', 'public, max-age=300');
      if (response.body) {
        return require('stream').Readable.fromWeb(response.body).pipe(res);
      }

      const buffer = Buffer.from(await response.arrayBuffer());
      return res.end(buffer);
    }

    const notes = readLocal();
    const note = notes.find((n) => n.id === id);
    if (!note?.fileUrl?.startsWith('/uploads/')) return res.status(404).send('Note not found.');

    const filename = path.basename(note.fileUrl);
    const localFile = path.join(UPLOADS, filename);
    if (!fs.existsSync(localFile)) return res.status(404).send('PDF not found.');

    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader('Content-Disposition', 'inline');
    return res.sendFile(localFile);
  } catch (err) {
    console.error('View note error:', err.message);
    res.status(404).send('Unable to open this PDF.');
  }
});

// Delete a note from Cloudinary (or local storage). Query-string id safely supports
// Cloudinary public IDs such as oav_hub_pdf_notes/note_12345_xxxxx.
app.delete('/api/delete-note', async (req, res) => {
  if (!adminOk(req.body)) {
    return res.status(401).json({
      success: false,
      message: 'Invalid admin credentials.'
    });
  }

  const id = String(req.query.id || '').trim();
  if (!id) return res.status(400).json({ success: false, message: 'Missing note id.' });

  try {
    if (cloudReady()) {
      const result = await cloudinary.uploader.destroy(id, { resource_type: 'raw' });
      if (result.result === 'not found') {
        console.warn('Cloudinary note not found:', id);
      }
    }

    const notes = readLocal();
    const note = notes.find((n) => n.id === id);

    if (note?.fileUrl?.startsWith('/uploads/')) {
      const filename = path.basename(note.fileUrl);
      const localFile = path.join(UPLOADS, filename);
      if (fs.existsSync(localFile)) fs.unlinkSync(localFile);
    }

    writeLocal(notes.filter((n) => n.id !== id));
    res.json({ success: true });
  } catch (err) {
    console.error('Delete error:', err);
    res.status(500).json({
      success: false,
      message: 'Delete failed.'
    });
  }
});

// Gemini Study Buddy
app.post('/api/chat', async (req, res) => {
  const prompt = (req.body.prompt || '').trim();

  if (!prompt) {
    return res.status(400).json({
      success: false,
      reply: 'Please type a question.'
    });
  }

  const key = process.env.GEMINI_API_KEY;

  if (!key) {
    return res.json({
      success: false,
      reply: 'Study Buddy AI is not configured yet. Add GEMINI_API_KEY in Render Environment Variables.'
    });
  }

  try {
    const model = process.env.GEMINI_MODEL || 'gemini-1.5-flash';

    const response = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${key}`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          contents: [
            {
              parts: [
                {
                  text: `You are Study Buddy for Indian school students. Give accurate, concise, easy-to-understand educational help. Question: ${prompt}`
                }
              ]
            }
          ]
        })
      }
    );

    const data = await response.json();
    const reply = data.candidates?.[0]?.content?.parts?.[0]?.text;

    res.json({
      success: !!reply,
      reply: reply || data.error?.message || 'No answer generated.'
    });
  } catch (err) {
    console.error('AI error:', err);
    res.status(500).json({
      success: false,
      reply: 'Unable to reach Study Buddy right now.'
    });
  }
});

// -------------------- CHESS SERVER --------------------

const matches = [];

const BOT_MATCH_ID = 'bot_match_nipun_permanent';

const permanentBotMatch = {
  id: BOT_MATCH_ID,
  p1: 'Waiting...',
  p2: '🤖 Nipun',
  p1Joined: false,
  p2Joined: true,
  fen: 'start',
  isBot: true,
  gameInstance: new Chess()
};

matches.push(permanentBotMatch);

const PIECE_VALUES = {
  p: 10,
  n: 30,
  b: 35,
  r: 50,
  q: 90,
  k: 1000
};

const PAWN_TABLE = [
  [0, 0, 0, 0, 0, 0, 0, 0],
  [5, 5, 5, 5, 5, 5, 5, 5],
  [1, 1, 2, 3, 3, 2, 1, 1],
  [0, 0, 2, 5, 5, 2, 0, 0],
  [0, 0, 0, 4, 4, 0, 0, 0],
  [0, -1, -1, 2, 2, -1, -1, 0],
  [0, 1, 1, -2, -2, 1, 1, 0],
  [0, 0, 0, 0, 0, 0, 0, 0]
];

const KNIGHT_TABLE = [
  [-5, -4, -3, -3, -3, -3, -4, -5],
  [-4, -2, 0, 0, 0, 0, -2, -4],
  [-3, 0, 3, 4, 4, 3, 0, -3],
  [-3, 1, 4, 5, 5, 4, 1, -3],
  [-3, 0, 4, 5, 5, 4, 0, -3],
  [-3, 1, 3, 4, 4, 3, 1, -3],
  [-4, -2, 0, 1, 1, 0, -2, -4],
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

        if (piece.type === 'p') {
          posVal =
            piece.color === 'w'
              ? PAWN_TABLE[r][c]
              : PAWN_TABLE[7 - r][c];
        }

        if (piece.type === 'n') {
          posVal =
            piece.color === 'w'
              ? KNIGHT_TABLE[r][c]
              : KNIGHT_TABLE[7 - r][c];
        }

        const score = val + posVal;
        totalEvaluation += piece.color === 'w' ? score : -score;
      }
    }
  }

  return totalEvaluation;
}

function getInstantBotMove(game) {
  const moves = game.moves({ verbose: true });
  if (!moves.length) return null;

  let bestMove = null;
  let bestValue = Infinity;

  for (const move of moves) {
    game.move(move);

    let score = evaluateBoard(game);

    if (game.isCheckmate()) score -= 5000;
    else if (game.isCheck()) score -= 15;

    game.undo();

    if (score < bestValue) {
      bestValue = score;
      bestMove = move;
    }
  }

  return bestMove || moves[Math.floor(Math.random() * moves.length)];
}

function makeBotMove(match) {
  if (!match.isBot || match.gameInstance.isGameOver()) return;

  if (match.gameInstance.turn() === 'b') {
    process.nextTick(() => {
      const bestMove = getInstantBotMove(match.gameInstance);
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
    });
  }
}

function toPublicMatch(match) {
  return {
    id: match.id,
    p1: match.p1,
    p2: match.p2,
    p1Joined: match.p1Joined,
    p2Joined: match.p2Joined,
    isBot: match.isBot || false,
    fen: match.fen || 'start'
  };
}

function broadcastMatches() {
  io.emit('init-data', matches.map(toPublicMatch));
}

function releaseSeat(socket) {
  if (!socket.seatInfo) return;

  const { matchId, color } = socket.seatInfo;
  const match = matches.find((m) => m.id === matchId);

  if (match) {
    if (color === 'w') {
      match.p1Joined = false;
      if (match.isBot) match.p1 = 'Waiting...';
    }

    if (color === 'b') {
      match.p2Joined = false;
    }

    broadcastMatches();
  }

  socket.seatInfo = null;
}

app.get('/api/matches', (req, res) => {
  res.json({ matches: matches.map(toPublicMatch) });
});

io.on('connection', (socket) => {
  socket.emit('init-data', matches.map(toPublicMatch));

  socket.on('claim-seat', ({ matchId, color, name }) => {
    const match = matches.find((m) => m.id === matchId);
    if (!match) return;

    const newName = name ? name.trim() : '';

    if (match.isBot) {
      match.gameInstance.reset();
      match.fen = 'start';
      match.p1Joined = true;
      match.p1 = newName || 'Player';
    } else {
      if (color === 'w') {
        match.p1Joined = true;
        match.p1 = newName || match.p1;
      } else if (color === 'b') {
        match.p2Joined = true;
        match.p2 = newName || match.p2;
      }
    }

    socket.seatInfo = {
      matchId,
      color: match.isBot ? 'w' : color
    };

    io.to(matchId).emit('reset', {
      matchId: match.id,
      fen: match.fen
    });

    broadcastMatches();
  });

  socket.on('join-match', (id) => {
    socket.join(id);

    const match = matches.find((m) => m.id === id);

    if (match) {
      socket.emit('match-state', {
        matchId: id,
        fen: match.gameInstance.fen()
      });
    }
  });

  socket.on('move', (data) => {
    const match = matches.find((m) => m.id === data.matchId);
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

// SPA fallback for OAV Hub pages
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

server.listen(PORT, () => {
  console.log(`🚀 OAV Hub server running on port ${PORT}`);
});
