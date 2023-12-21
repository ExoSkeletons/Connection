package com.aviadl40.connection.game;

import com.aviadl40.connection.game.screens.GameScreen;

import java.util.Comparator;

@SuppressWarnings("unused")
public final class Move {
	static class QualityComparator implements Comparator<Move> {
		private static int judgeQuality(Move m, Player[][][] board) {
			int q = 0, size = board.length;
			if (m.x == (float) (size - 1) / 2)
				q++;
			if (m.y == (float) (size - 1) / 2)
				q++;
			if (m.i == (float) (size - 1) / 2)
				q += 2;

			if (m.x == 0 || m.x == size - 1)
				q++;
			if (m.y == 0 || m.y == size - 1)
				q++;
			if (m.i == 0 || m.i == size - 1)
				q++;

			if (m.x == m.y || m.x == size - 1 - m.y)
				q++;
			if (m.y == m.i || m.y == size - 1 - m.i)
				q++;
			if (m.i == m.x || m.i == size - 1 - m.x)
				q++;

			return q;
		}

		private final Player[][][] board;

		QualityComparator(Player[][][] board) {
			this.board = board;
		}

		@Override
		public int compare(Move m1, Move m2) {
			return judgeQuality(m1, board) - judgeQuality(m2, board);
		}
	}

	public byte x = 0, y = 0;
	public byte i = 0;

	Move(Move move) {
		x = move.x;
		y = move.y;
		i = move.i;
	}

	public Move() {
	}

	public Move(byte x, byte y, byte i) {
		this.x = x;
		this.y = y;
		this.i = i;
	}

	@Override
	public String toString() {
		return "[" + x + "," + y + "," + i + "]";
	}
}
