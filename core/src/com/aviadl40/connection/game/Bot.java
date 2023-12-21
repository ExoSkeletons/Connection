package com.aviadl40.connection.game;

import android.support.annotation.Nullable;

import com.aviadl40.connection.game.screens.GameScreen;
import com.aviadl40.utils.Utils;
import com.badlogic.gdx.utils.Array;

import java.util.HashMap;

public class Bot extends Player {
	public Bot() {
		super("Bot");
	}

	@SuppressWarnings("ManualArrayCopy")
	@Nullable
	public Move calculateMove(final Player[][][] board, final Array<Player> players, int mPI, byte[][] piecesAvailable) {
		final Player[][][] sandbox = new Player[board.length][board[0].length][board[0][0].length];
		for (byte x = 0; x < board.length; x++)
			for (byte y = 0; y < board[x].length; y++)
				for (byte i = 0; i < board[x][y].length; i++)
					sandbox[x][y][i] = board[x][y][i];
		byte[][] sandboxPieces = new byte[piecesAvailable.length][board[0][0].length];
		for (byte pi = 0; pi < players.size; pi++)
			for (byte i = 0; i < piecesAvailable[pi].length; i++)
				sandboxPieces[pi][i] = piecesAvailable[pi][i];
		final Array<Move> possibleMoves = new Array<>();
		final HashMap<Player, Array<Move>> winningMoves = new HashMap<>();
		for (Player p : players)
			winningMoves.put(p, new Array<Move>());

		{
			Move m = new Move();
			Player p, prev;
			for (byte x = 0; x < board.length; x++) {
				m.x = x;
				for (byte y = 0; y < board[x].length; y++) {
					m.y = y;
					for (byte i = 0; i < board[x][y].length; i++) {
						m.i = i;
						int pi = mPI;
						do {
							if (GameScreen.isMovePossible(m, board, piecesAvailable[pi])) {
								prev = sandbox[x][y][i]; // not really necessary, just good practice
								sandbox[x][y][i] = p = players.get(pi);
								if (GameScreen.checkWin(sandbox) == p) {
									if (p == this) // Win
										return m;
									winningMoves.get(p).add(new Move(m));
								}
								sandbox[x][y][i] = prev;
								if (p == this)
									possibleMoves.add(new Move(m));
							}
							pi = Utils.getNextIndex(players.items, pi);
						} while (pi != mPI);
					}
				}
			}
		}

		// Try to block upcoming Player's win
		{
			Array<Move> moves;
			int pi = mPI, turns = 0;
			do {
				pi = Utils.getNextIndex(players.items, pi);
				turns++;
				if (turns <= (moves = winningMoves.get(players.get(pi))).size)
					for (Move m : moves)
						if (GameScreen.isMovePossible(m, board, piecesAvailable[mPI]))
							return m;
			} while (pi != mPI);
		}

		{
			boolean pathPossible;
			byte pathAmount, largestAmount = 0;
			Move move = null;
			Player p;
			for (Move m : possibleMoves) {
				pathAmount = 0;

				// Reset
				pathPossible = true;
				for (byte i = 0; i < board.length; i++)
					sandboxPieces[mPI][i] = (byte) board.length;
				// Test
				for (byte x = 0; x < board.length; x++) {
					p = board[x][m.y][m.i];
					sandboxPieces[mPI][m.i]--;
					if ((p != null && p != this) || (sandboxPieces[mPI][m.i] <= 0)) { // Path blocked or No more pieces
						pathPossible = false;
						break;
					}
				}
				if (pathPossible)
					pathAmount++;
				// Reset
				pathPossible = true;
				for (byte i = 0; i < board.length; i++)
					sandboxPieces[mPI][i] = (byte) board.length;
				// Test
				for (byte y = 0; y < board.length; y++) {
					p = board[m.x][y][m.i];
					sandboxPieces[mPI][m.i]--;
					if ((p != null && p != this) || (sandboxPieces[mPI][m.i] <= 0)) { // Path blocked or No more pieces
						pathPossible = false;
						break;
					}
				}
				if (pathPossible)
					pathAmount++;
				// Reset
				pathPossible = true;
				for (byte i = 0; i < board.length; i++)
					sandboxPieces[mPI][i] = (byte) board.length;
				// Test
				for (byte i = 0; i < board.length; i++) {
					p = board[m.x][m.y][i];
					sandboxPieces[mPI][i]--;
					if ((p != null && p != this) || (sandboxPieces[mPI][i] <= 0)) { // Path blocked or No more pieces
						pathPossible = false;
						break;
					}
				}
				if (pathPossible)
					pathAmount++;

				if (m.x == m.y) {
					// Reset
					pathPossible = true;
					for (byte i = 0; i < board.length; i++)
						sandboxPieces[mPI][i] = (byte) board.length;
					// Test
					for (byte b = 0; b < board.length; b++) {
						p = board[b][b][m.i]; // ++ /
						sandboxPieces[mPI][m.i]--;
						if ((p != null && p != this) || (sandboxPieces[mPI][m.i] <= 0)) { // Path blocked or No more pieces
							pathPossible = false;
							break;
						}
					}
					if (pathPossible)
						pathAmount++;
					// Reset
					pathPossible = true;
					for (byte i = 0; i < board.length; i++)
						sandboxPieces[mPI][i] = (byte) board.length;
					// Test
					for (byte b = 0; b < board.length; b++) {
						p = board[b][board.length - 1 - b][m.i]; // +- \
						sandboxPieces[mPI][m.i]--;
						if ((p != null && p != this) || (sandboxPieces[mPI][m.i] <= 0)) { // Path blocked or No more pieces
							pathPossible = false;
							break;
						}
					}
					if (pathPossible)
						pathAmount++;
				}
				if (m.y == m.i) {
					// Reset
					pathPossible = true;
					for (byte i = 0; i < board.length; i++)
						sandboxPieces[mPI][i] = (byte) board.length;
					// Test
					for (byte b = 0; b < board.length; b++) {
						p = board[m.x][b][b]; // ++ /
						sandboxPieces[mPI][b]--;
						if ((p != null && p != this) || (sandboxPieces[mPI][b] <= 0)) { // Path blocked or No more pieces
							pathPossible = false;
							break;
						}
					}
					if (pathPossible)
						pathAmount++;
					// Reset
					pathPossible = true;
					for (byte i = 0; i < board.length; i++)
						sandboxPieces[mPI][i] = (byte) board.length;
					// Test
					for (byte b = 0; b < board.length; b++) {
						p = board[m.x][b][board.length - 1 - b]; // +- \
						sandboxPieces[mPI][board.length - 1 - b]--;
						if ((p != null && p != this) || (sandboxPieces[mPI][board.length - 1 - b] <= 0)) { // Path blocked or No more pieces
							pathPossible = false;
							break;
						}
					}
					if (pathPossible)
						pathAmount++;
				}
				if (m.i == m.x) {
					// Reset
					pathPossible = true;
					for (byte i = 0; i < board.length; i++)
						sandboxPieces[mPI][i] = (byte) board.length;
					// Test
					for (byte b = 0; b < board.length; b++) {
						p = board[b][m.y][b]; // ++ /
						sandboxPieces[mPI][b]--;
						if ((p != null && p != this) || (sandboxPieces[mPI][b] <= 0)) { // Path blocked or No more pieces
							pathPossible = false;
							break;
						}
					}
					if (pathPossible)
						pathAmount++;
					// Reset
					pathPossible = true;
					for (byte i = 0; i < board.length; i++)
						sandboxPieces[mPI][i] = (byte) board.length;
					// Test
					for (byte b = 0; b < board.length; b++) {
						p = board[b][m.y][board.length - 1 - b]; // +- \
						sandboxPieces[mPI][board.length - 1 - b]--;
						if ((p != null && p != this) || (sandboxPieces[mPI][board.length - 1 - b] <= 0)) { // Path blocked or No more pieces
							pathPossible = false;
							break;
						}
					}
					if (pathPossible)
						pathAmount++;
				}

				if (m.x == m.y && m.i == m.x) {
					// Reset
					pathPossible = true;
					for (byte i = 0; i < board.length; i++)
						sandboxPieces[mPI][i] = (byte) board.length;
					// Test
					for (byte b = 0; b < board.length; b++) {
						p = board[b][b][b]; // +++
						sandboxPieces[mPI][b]--;
						if ((p != null && p != this) || (sandboxPieces[mPI][b] <= 0)) { // Path blocked or No more pieces
							pathPossible = false;
							break;
						}
					}
					if (pathPossible)
						pathAmount++;
					// Reset
					pathPossible = true;
					for (byte i = 0; i < board.length; i++)
						sandboxPieces[mPI][i] = (byte) board.length;
					// Test
					for (byte b = 0; b < board.length; b++) {
						p = board[b][board.length - 1 - b][b]; // +-+ /
						sandboxPieces[mPI][b]--;
						if ((p != null && p != this) || (sandboxPieces[mPI][b] <= 0)) { // Path blocked or No more pieces
							pathPossible = false;
							break;
						}
					}
					if (pathPossible)
						pathAmount++;
					// Reset
					pathPossible = true;
					for (byte i = 0; i < board.length; i++)
						sandboxPieces[mPI][i] = (byte) board.length;
					// Test
					for (byte b = 0; b < board.length; b++) {
						p = board[board.length - 1 - b][b][b]; // -++
						sandboxPieces[mPI][b]--;
						if ((p != null && p != this) || (sandboxPieces[mPI][b] <= 0)) { // Path blocked or No more pieces
							pathPossible = false;
							break;
						}
					}
					if (pathPossible)
						pathAmount++;
				}


				if (pathAmount > largestAmount) {
					largestAmount = pathAmount;
					move = m;
				}
			}
			if (move != null)
				return move;
		}

		// Simple case
		possibleMoves.shuffle();
		possibleMoves.sort(new Move.QualityComparator(board));
		return possibleMoves.size == 0 ? null : possibleMoves.get(possibleMoves.size - 1);
	}
}
