package com.example.chess

import kotlin.math.abs
import kotlin.random.Random

enum class PieceColor { WHITE, BLACK }
enum class PieceType { PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING }

data class ChessPiece(
    val type: PieceType,
    val color: PieceColor,
    val hasMoved: Boolean = false
) {
    val symbol: String
        get() = when (color) {
            PieceColor.WHITE -> when (type) {
                PieceType.KING -> "♔"
                PieceType.QUEEN -> "♕"
                PieceType.ROOK -> "♖"
                PieceType.BISHOP -> "♗"
                PieceType.KNIGHT -> "♘"
                PieceType.PAWN -> "♙"
            }
            PieceColor.BLACK -> when (type) {
                PieceType.KING -> "♚"
                PieceType.QUEEN -> "♛"
                PieceType.ROOK -> "♜"
                PieceType.BISHOP -> "♝"
                PieceType.KNIGHT -> "♞"
                PieceType.PAWN -> "♟"
            }
        }
}

data class Position(val row: Int, val col: Int) {
    val notation: String
        get() {
            val file = ('a'.code + col).toChar()
            val rank = 8 - row
            return "$file$rank"
        }
}

data class Move(
    val from: Position,
    val to: Position,
    val piece: ChessPiece,
    val captured: ChessPiece? = null,
    val isCheck: Boolean = false,
    val isCheckmate: Boolean = false
) {
    val notation: String
        get() {
            val p = if (piece.type == PieceType.PAWN) "" else piece.type.name.take(1)
            val capture = if (captured != null) "x" else ""
            val check = if (isCheckmate) "#" else if (isCheck) "+" else ""
            return "$p${from.notation}$capture${to.notation}$check"
        }
}

enum class GameMode { VS_AI_EASY, VS_AI_MEDIUM, VS_AI_HARD, PASS_AND_PLAY, LIVE_SERVER }
enum class GameStatus { IN_PROGRESS, CHECK, CHECKMATE, STALEMATE, DRAW }

data class ChessGameState(
    val board: Array<Array<ChessPiece?>> = createInitialBoard(),
    val turn: PieceColor = PieceColor.WHITE,
    val selectedPos: Position? = null,
    val validMoves: List<Position> = emptyList(),
    val moveHistory: List<Move> = emptyList(),
    val capturedByWhite: List<ChessPiece> = emptyList(),
    val capturedByBlack: List<ChessPiece> = emptyList(),
    val gameStatus: GameStatus = GameStatus.IN_PROGRESS,
    val gameMode: GameMode = GameMode.VS_AI_MEDIUM,
    val isFlipped: Boolean = false,
    val whiteTimeSeconds: Int = 600,
    val blackTimeSeconds: Int = 600,
    val winner: PieceColor? = null
) {
    companion object {
        fun createInitialBoard(): Array<Array<ChessPiece?>> {
            val b = Array(8) { arrayOfNulls<ChessPiece>(8) }
            // Black pieces (row 0, 1)
            b[0][0] = ChessPiece(PieceType.ROOK, PieceColor.BLACK)
            b[0][1] = ChessPiece(PieceType.KNIGHT, PieceColor.BLACK)
            b[0][2] = ChessPiece(PieceType.BISHOP, PieceColor.BLACK)
            b[0][3] = ChessPiece(PieceType.QUEEN, PieceColor.BLACK)
            b[0][4] = ChessPiece(PieceType.KING, PieceColor.BLACK)
            b[0][5] = ChessPiece(PieceType.BISHOP, PieceColor.BLACK)
            b[0][6] = ChessPiece(PieceType.KNIGHT, PieceColor.BLACK)
            b[0][7] = ChessPiece(PieceType.ROOK, PieceColor.BLACK)
            for (c in 0..7) b[1][c] = ChessPiece(PieceType.PAWN, PieceColor.BLACK)

            // White pieces (row 6, 7)
            for (c in 0..7) b[6][c] = ChessPiece(PieceType.PAWN, PieceColor.WHITE)
            b[7][0] = ChessPiece(PieceType.ROOK, PieceColor.WHITE)
            b[7][1] = ChessPiece(PieceType.KNIGHT, PieceColor.WHITE)
            b[7][2] = ChessPiece(PieceType.BISHOP, PieceColor.WHITE)
            b[7][3] = ChessPiece(PieceType.QUEEN, PieceColor.WHITE)
            b[7][4] = ChessPiece(PieceType.KING, PieceColor.WHITE)
            b[7][5] = ChessPiece(PieceType.BISHOP, PieceColor.WHITE)
            b[7][6] = ChessPiece(PieceType.KNIGHT, PieceColor.WHITE)
            b[7][7] = ChessPiece(PieceType.ROOK, PieceColor.WHITE)

            return b
        }
    }
}

object ChessLogic {

    fun getLegalMoves(board: Array<Array<ChessPiece?>>, from: Position): List<Position> {
        val piece = board[from.row][from.col] ?: return emptyList()
        val rawMoves = getRawMoves(board, from, piece)
        
        // Filter out moves that leave king in check
        return rawMoves.filter { to ->
            val simulated = simulateMove(board, from, to)
            !isKingInCheck(simulated, piece.color)
        }
    }

    private fun getRawMoves(board: Array<Array<ChessPiece?>>, from: Position, piece: ChessPiece): List<Position> {
        val moves = mutableListOf<Position>()
        val r = from.row
        val c = from.col
        val dir = if (piece.color == PieceColor.WHITE) -1 else 1

        when (piece.type) {
            PieceType.PAWN -> {
                // Forward 1
                val f1 = r + dir
                if (f1 in 0..7 && board[f1][c] == null) {
                    moves.add(Position(f1, c))
                    // Forward 2
                    val startRow = if (piece.color == PieceColor.WHITE) 6 else 1
                    val f2 = r + 2 * dir
                    if (r == startRow && board[f2][c] == null) {
                        moves.add(Position(f2, c))
                    }
                }
                // Diagonal captures
                for (dc in listOf(-1, 1)) {
                    val tr = r + dir
                    val tc = c + dc
                    if (tr in 0..7 && tc in 0..7) {
                        val target = board[tr][tc]
                        if (target != null && target.color != piece.color) {
                            moves.add(Position(tr, tc))
                        }
                    }
                }
            }
            PieceType.KNIGHT -> {
                val deltas = listOf(
                    -2 to -1, -2 to 1, -1 to -2, -1 to 2,
                    1 to -2, 1 to 2, 2 to -1, 2 to 1
                )
                for ((dr, dc) in deltas) {
                    val nr = r + dr
                    val nc = c + dc
                    if (nr in 0..7 && nc in 0..7) {
                        val target = board[nr][nc]
                        if (target == null || target.color != piece.color) {
                            moves.add(Position(nr, nc))
                        }
                    }
                }
            }
            PieceType.BISHOP -> addRayMoves(board, from, piece, listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1), moves)
            PieceType.ROOK -> addRayMoves(board, from, piece, listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1), moves)
            PieceType.QUEEN -> addRayMoves(board, from, piece, listOf(
                -1 to 0, 1 to 0, 0 to -1, 0 to 1,
                -1 to -1, -1 to 1, 1 to -1, 1 to 1
            ), moves)
            PieceType.KING -> {
                for (dr in -1..1) {
                    for (dc in -1..1) {
                        if (dr == 0 && dc == 0) continue
                        val nr = r + dr
                        val nc = c + dc
                        if (nr in 0..7 && nc in 0..7) {
                            val target = board[nr][nc]
                            if (target == null || target.color != piece.color) {
                                moves.add(Position(nr, nc))
                            }
                        }
                    }
                }
            }
        }
        return moves
    }

    private fun addRayMoves(
        board: Array<Array<ChessPiece?>>,
        from: Position,
        piece: ChessPiece,
        directions: List<Pair<Int, Int>>,
        moves: MutableList<Position>
    ) {
        for ((dr, dc) in directions) {
            var currR = from.row + dr
            var currC = from.col + dc
            while (currR in 0..7 && currC in 0..7) {
                val target = board[currR][currC]
                if (target == null) {
                    moves.add(Position(currR, currC))
                } else {
                    if (target.color != piece.color) {
                        moves.add(Position(currR, currC))
                    }
                    break
                }
                currR += dr
                currC += dc
            }
        }
    }

    private fun simulateMove(board: Array<Array<ChessPiece?>>, from: Position, to: Position): Array<Array<ChessPiece?>> {
        val newBoard = Array(8) { r -> Array(8) { c -> board[r][c] } }
        val p = newBoard[from.row][from.col]
        newBoard[from.row][from.col] = null
        newBoard[to.row][to.col] = p
        return newBoard
    }

    fun isKingInCheck(board: Array<Array<ChessPiece?>>, kingColor: PieceColor): Boolean {
        // Find king position
        var kingPos: Position? = null
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c]
                if (piece != null && piece.type == PieceType.KING && piece.color == kingColor) {
                    kingPos = Position(r, c)
                    break
                }
            }
            if (kingPos != null) break
        }
        if (kingPos == null) return false

        // Check if any opponent piece can attack kingPos
        val opponentColor = if (kingColor == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c]
                if (piece != null && piece.color == opponentColor) {
                    val rawMoves = getRawMoves(board, Position(r, c), piece)
                    if (rawMoves.contains(kingPos)) return true
                }
            }
        }
        return false
    }

    fun getAllLegalMoves(board: Array<Array<ChessPiece?>>, color: PieceColor): List<Pair<Position, Position>> {
        val list = mutableListOf<Pair<Position, Position>>()
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c]
                if (piece != null && piece.color == color) {
                    val from = Position(r, c)
                    val moves = getLegalMoves(board, from)
                    for (to in moves) {
                        list.add(from to to)
                    }
                }
            }
        }
        return list
    }

    fun applyMove(state: ChessGameState, from: Position, to: Position): ChessGameState {
        val newBoard = Array(8) { r -> Array(8) { c -> state.board[r][c] } }
        val movingPiece = newBoard[from.row][from.col] ?: return state
        val capturedPiece = newBoard[to.row][to.col]

        // Promote pawn to queen if reached opposite end
        val finalPiece = if (movingPiece.type == PieceType.PAWN && (to.row == 0 || to.row == 7)) {
            ChessPiece(PieceType.QUEEN, movingPiece.color, true)
        } else {
            movingPiece.copy(hasMoved = true)
        }

        newBoard[from.row][from.col] = null
        newBoard[to.row][to.col] = finalPiece

        val nextTurn = if (state.turn == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
        val isOpponentInCheck = isKingInCheck(newBoard, nextTurn)
        val opponentLegalMoves = getAllLegalMoves(newBoard, nextTurn)

        val newStatus = when {
            opponentLegalMoves.isEmpty() && isOpponentInCheck -> GameStatus.CHECKMATE
            opponentLegalMoves.isEmpty() && !isOpponentInCheck -> GameStatus.STALEMATE
            isOpponentInCheck -> GameStatus.CHECK
            else -> GameStatus.IN_PROGRESS
        }

        val move = Move(
            from = from,
            to = to,
            piece = movingPiece,
            captured = capturedPiece,
            isCheck = isOpponentInCheck,
            isCheckmate = newStatus == GameStatus.CHECKMATE
        )

        val newWhiteCaptured = if (capturedPiece != null && capturedPiece.color == PieceColor.BLACK) {
            state.capturedByWhite + capturedPiece
        } else state.capturedByWhite

        val newBlackCaptured = if (capturedPiece != null && capturedPiece.color == PieceColor.WHITE) {
            state.capturedByBlack + capturedPiece
        } else state.capturedByBlack

        val winner = if (newStatus == GameStatus.CHECKMATE) state.turn else null

        return state.copy(
            board = newBoard,
            turn = nextTurn,
            selectedPos = null,
            validMoves = emptyList(),
            moveHistory = state.moveHistory + move,
            capturedByWhite = newWhiteCaptured,
            capturedByBlack = newBlackCaptured,
            gameStatus = newStatus,
            winner = winner
        )
    }

    fun computeAiMove(state: ChessGameState): Pair<Position, Position>? {
        val legalMoves = getAllLegalMoves(state.board, state.turn)
        if (legalMoves.isEmpty()) return null

        return when (state.gameMode) {
            GameMode.VS_AI_EASY -> legalMoves.random()
            GameMode.VS_AI_MEDIUM -> {
                // Prioritize captures and checks
                val captureMoves = legalMoves.filter { (from, to) ->
                    state.board[to.row][to.col] != null
                }
                if (captureMoves.isNotEmpty() && Random.nextFloat() > 0.3f) {
                    captureMoves.maxByOrNull { (from, to) ->
                        getPieceValue(state.board[to.row][to.col]?.type)
                    } ?: captureMoves.random()
                } else {
                    legalMoves.random()
                }
            }
            GameMode.VS_AI_HARD -> {
                // Positional scoring + piece value
                legalMoves.maxByOrNull { (from, to) ->
                    val moving = state.board[from.row][from.col]
                    val captured = state.board[to.row][to.col]
                    var score = getPieceValue(captured?.type) * 10
                    // Center control bonus
                    val centerDist = abs(3.5 - to.row) + abs(3.5 - to.col)
                    score += ((7 - centerDist) * 2).toInt()
                    score
                } ?: legalMoves.random()
            }
            else -> legalMoves.random()
        }
    }

    private fun getPieceValue(type: PieceType?): Int {
        return when (type) {
            PieceType.PAWN -> 1
            PieceType.KNIGHT -> 3
            PieceType.BISHOP -> 3
            PieceType.ROOK -> 5
            PieceType.QUEEN -> 9
            PieceType.KING -> 100
            null -> 0
        }
    }
}
