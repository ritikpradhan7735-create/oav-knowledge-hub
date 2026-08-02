const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const path = require('path');
const { Chess } = require('chess.js');

const app = express();
const server = http.createServer(app);
const io = new Server(server);

app.use(express.static(path.join(__dirname, 'public')));

let matches = [];

io.on('connection', (socket) => {
    console.log('⚡ User connected:', socket.id);

    // Send active matches list on connect
    socket.emit('init-data', matches.map(({ id, p1, p2 }) => ({ id, p1, p2 })));

    // Handle Admin Match Creation
    socket.on('create-match', (match) => {
        const gameInstance = new Chess();
        const newMatch = { 
            id: match.id || "match_" + Date.now(),
            p1: match.p1 || "Player 1",
            p2: match.p2 || "Player 2",
            fen: 'start', 
            gameInstance 
        };
        matches.push(newMatch);
        io.emit('init-data', matches.map(({ id, p1, p2 }) => ({ id, p1, p2 })));
    });

    socket.on('join-match', (id) => {
        socket.join(id);
        const match = matches.find(m => m.id === id);
        if (match) {
            socket.emit('match-state', { matchId: id, fen: match.gameInstance.fen() });
        }
    });

    socket.on('chat-message', (data) => {
        io.to(data.matchId).emit('chat-message', data);
    });

    socket.on('leave-match', (id) => {
        socket.leave(id);
        console.log(`👤 Socket ${socket.id} left match ${id}`);
    });

    // Protected move event with try-catch
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
            }
        } catch (err) {
            console.error("Illegal move attempt caught:", err.message);
        }
    });

    socket.on('undo', (data) => {
        const match = matches.find(m => m.id === data.matchId);
        if (!match) return;

        match.gameInstance.undo();
        match.fen = match.gameInstance.fen();

        io.to(data.matchId).emit('undo', {
            matchId: data.matchId,
            fen: match.fen
        });
    });

    socket.on('reset', (data) => {
        const match = matches.find(m => m.id === data.matchId);
        if (!match) return;

        match.gameInstance.reset();
        match.fen = 'start';

        io.to(data.matchId).emit('reset', {
            matchId: data.matchId
        });
    });

    socket.on('disconnect', () => {
        console.log('❌ User disconnected:', socket.id);
    });
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
    console.log(`🚀 Server running on http://localhost:${PORT}`);
});