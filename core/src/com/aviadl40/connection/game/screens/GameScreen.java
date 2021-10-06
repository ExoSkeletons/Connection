package com.aviadl40.connection.game.screens;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.aviadl40.connection.Gui;
import com.aviadl40.connection.Settings;
import com.aviadl40.connection.Utils;
import com.aviadl40.connection.game.GameParameters;
import com.aviadl40.connection.game.managers.AudioManager;
import com.aviadl40.connection.game.managers.ScreenManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

import java.util.Comparator;
import java.util.HashMap;

@SuppressWarnings("ForLoopReplaceableByForEach")
public abstract class GameScreen extends ScreenManager.UIScreen {
	@SuppressWarnings("unused")
	static final class Move {
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

		byte x = 0, y = 0;
		byte i = 0;

		Move(Move move) {
			x = move.x;
			y = move.y;
			i = move.i;
		}

		Move() {
		}

		Move(byte x, byte y, byte i) {
			this.x = x;
			this.y = y;
			this.i = i;
		}

		@Override
		public String toString() {
			return "[" + x + "," + y + "," + i + "]";
		}
	}

	public static class Player {
		static final class PlayerNameTextField extends TextField {
			PlayerNameTextField(String text, Skin skin) {
				super(text, skin);
			}

			@Override
			protected void drawText(Batch batch, BitmapFont font, float x, float y) {
				Color fc = font.getColor();
				font.setColor(getColor());
				super.drawText(batch, font, x, y);
				font.setColor(fc);
			}
		}

		static final class PlayerTextFieldListener implements TextField.TextFieldListener {
			private final byte pi;
			private final SetupScreen setup;

			PlayerTextFieldListener(byte pi, SetupScreen setup) {
				this.pi = pi;
				this.setup = setup;
			}

			@Override
			public void keyTyped(TextField textField, char c) {
				if (c == '\n') {
					if (textField.getStage() != null && textField.getStage().getKeyboardFocus() == textField) {
						textField.getOnscreenKeyboard().show(false);
						textField.getStage().setKeyboardFocus(null);
					}
					setup.changePlayerName(pi, textField.getText());
				}
			}
		}

		@NonNull
		public String name;
		@NonNull
		Color color = new Color(MathUtils.random(), MathUtils.random(), MathUtils.random(), 1);

		Player(@NonNull String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	static abstract class Human extends Player {
		Human(@NonNull String name) {
			super(name);
		}
	}

	static class Bot extends Player {
		Bot() {
			super("Bot");
		}

		@SuppressWarnings("ManualArrayCopy")
		@Nullable
		Move calculateMove(final Player[][][] board, final Array<Player> players, int mpi, byte[][] pieces) {
			final Player[][][] sandbox = new Player[board.length][board[0].length][board[0][0].length];
			for (byte x = 0; x < board.length; x++)
				for (byte y = 0; y < board[x].length; y++)
					for (byte i = 0; i < board[x][y].length; i++)
						sandbox[x][y][i] = board[x][y][i];
			byte[][] sandboxPieces = new byte[pieces.length][board[0][0].length];
			for (byte pi = 0; pi < players.size; pi++)
				for (byte i = 0; i < pieces[pi].length; i++)
					sandboxPieces[pi][i] = pieces[pi][i];
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
							int pi = mpi;
							do {
								if (isMovePossible(m, board, pieces[pi])) {
									prev = sandbox[x][y][i]; // not really necessary, just good practice
									sandbox[x][y][i] = p = players.get(pi);
									if (checkWin(sandbox) == p) {
										if (p == this) // Win
											return m;
										winningMoves.get(p).add(new Move(m));
									}
									sandbox[x][y][i] = prev;
									if (p == this)
										possibleMoves.add(new Move(m));
								}
								pi = Utils.getNextIndex(players.items, pi);
							} while (pi != mpi);
						}
					}
				}
			}

			// Try to block upcoming Player's win
			{
				Array<Move> moves;
				int pi = mpi, turns = 0;
				do {
					pi = Utils.getNextIndex(players.items, pi);
					turns++;
					if (turns <= (moves = winningMoves.get(players.get(pi))).size)
						for (Move m : moves)
							if (isMovePossible(m, board, pieces[mpi]))
								return m;
				} while (pi != mpi);
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
						sandboxPieces[mpi][i] = (byte) board.length;
					// Test
					for (byte x = 0; x < board.length; x++) {
						p = board[x][m.y][m.i];
						sandboxPieces[mpi][m.i]--;
						if ((p != null && p != this) || (sandboxPieces[mpi][m.i] <= 0)) { // Path blocked or No more pieces
							pathPossible = false;
							break;
						}
					}
					if (pathPossible)
						pathAmount++;
					// Reset
					pathPossible = true;
					for (byte i = 0; i < board.length; i++)
						sandboxPieces[mpi][i] = (byte) board.length;
					// Test
					for (byte y = 0; y < board.length; y++) {
						p = board[m.x][y][m.i];
						sandboxPieces[mpi][m.i]--;
						if ((p != null && p != this) || (sandboxPieces[mpi][m.i] <= 0)) { // Path blocked or No more pieces
							pathPossible = false;
							break;
						}
					}
					if (pathPossible)
						pathAmount++;
					// Reset
					pathPossible = true;
					for (byte i = 0; i < board.length; i++)
						sandboxPieces[mpi][i] = (byte) board.length;
					// Test
					for (byte i = 0; i < board.length; i++) {
						p = board[m.x][m.y][i];
						sandboxPieces[mpi][i]--;
						if ((p != null && p != this) || (sandboxPieces[mpi][i] <= 0)) { // Path blocked or No more pieces
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
							sandboxPieces[mpi][i] = (byte) board.length;
						// Test
						for (byte b = 0; b < board.length; b++) {
							p = board[b][b][m.i]; // ++ /
							sandboxPieces[mpi][m.i]--;
							if ((p != null && p != this) || (sandboxPieces[mpi][m.i] <= 0)) { // Path blocked or No more pieces
								pathPossible = false;
								break;
							}
						}
						if (pathPossible)
							pathAmount++;
						// Reset
						pathPossible = true;
						for (byte i = 0; i < board.length; i++)
							sandboxPieces[mpi][i] = (byte) board.length;
						// Test
						for (byte b = 0; b < board.length; b++) {
							p = board[b][board.length - 1 - b][m.i]; // +- \
							sandboxPieces[mpi][m.i]--;
							if ((p != null && p != this) || (sandboxPieces[mpi][m.i] <= 0)) { // Path blocked or No more pieces
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
							sandboxPieces[mpi][i] = (byte) board.length;
						// Test
						for (byte b = 0; b < board.length; b++) {
							p = board[m.x][b][b]; // ++ /
							sandboxPieces[mpi][b]--;
							if ((p != null && p != this) || (sandboxPieces[mpi][b] <= 0)) { // Path blocked or No more pieces
								pathPossible = false;
								break;
							}
						}
						if (pathPossible)
							pathAmount++;
						// Reset
						pathPossible = true;
						for (byte i = 0; i < board.length; i++)
							sandboxPieces[mpi][i] = (byte) board.length;
						// Test
						for (byte b = 0; b < board.length; b++) {
							p = board[m.x][b][board.length - 1 - b]; // +- \
							sandboxPieces[mpi][board.length - 1 - b]--;
							if ((p != null && p != this) || (sandboxPieces[mpi][board.length - 1 - b] <= 0)) { // Path blocked or No more pieces
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
							sandboxPieces[mpi][i] = (byte) board.length;
						// Test
						for (byte b = 0; b < board.length; b++) {
							p = board[b][m.y][b]; // ++ /
							sandboxPieces[mpi][b]--;
							if ((p != null && p != this) || (sandboxPieces[mpi][b] <= 0)) { // Path blocked or No more pieces
								pathPossible = false;
								break;
							}
						}
						if (pathPossible)
							pathAmount++;
						// Reset
						pathPossible = true;
						for (byte i = 0; i < board.length; i++)
							sandboxPieces[mpi][i] = (byte) board.length;
						// Test
						for (byte b = 0; b < board.length; b++) {
							p = board[b][m.y][board.length - 1 - b]; // +- \
							sandboxPieces[mpi][board.length - 1 - b]--;
							if ((p != null && p != this) || (sandboxPieces[mpi][board.length - 1 - b] <= 0)) { // Path blocked or No more pieces
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
							sandboxPieces[mpi][i] = (byte) board.length;
						// Test
						for (byte b = 0; b < board.length; b++) {
							p = board[b][b][b]; // +++
							sandboxPieces[mpi][b]--;
							if ((p != null && p != this) || (sandboxPieces[mpi][b] <= 0)) { // Path blocked or No more pieces
								pathPossible = false;
								break;
							}
						}
						if (pathPossible)
							pathAmount++;
						// Reset
						pathPossible = true;
						for (byte i = 0; i < board.length; i++)
							sandboxPieces[mpi][i] = (byte) board.length;
						// Test
						for (byte b = 0; b < board.length; b++) {
							p = board[b][board.length - 1 - b][b]; // +-+ /
							sandboxPieces[mpi][b]--;
							if ((p != null && p != this) || (sandboxPieces[mpi][b] <= 0)) { // Path blocked or No more pieces
								pathPossible = false;
								break;
							}
						}
						if (pathPossible)
							pathAmount++;
						// Reset
						pathPossible = true;
						for (byte i = 0; i < board.length; i++)
							sandboxPieces[mpi][i] = (byte) board.length;
						// Test
						for (byte b = 0; b < board.length; b++) {
							p = board[board.length - 1 - b][b][b]; // -++
							sandboxPieces[mpi][b]--;
							if ((p != null && p != this) || (sandboxPieces[mpi][b] <= 0)) { // Path blocked or No more pieces
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

	static final class LocalPlayer extends Human {
		LocalPlayer() {
			super("Player");
		}
	}

	static abstract class SetupScreen<G extends GameScreen> extends ScreenManager.UIScreen {
		@NonNull
		final GameParameters params = new GameParameters();
		final Table sizeTools = new Table(Gui.skin());
		final Label title = new Label("", Gui.instance().labelStyles.headerStyle);
		final Table tools = new Table(Gui.skin());
		private final Table playerListTable = new Table(Gui.skin());
		private final ScrollPane playerListScroll = new ScrollPane(playerListTable);
		final Table playerTools = new Table(Gui.skin()) {
			@Override
			public void act(float delta) {
				playerListTable.setHeight(playerListTable.getPrefHeight());
				playerListScroll.setHeight(Math.min(playerListTable.getHeight(), Gdx.graphics.getHeight() * .2f));
				getCell(playerListScroll).height(playerListScroll.getHeight());
				playerListTable.invalidateHierarchy();
				super.act(delta);
			}
		}, addPlayerTools = new Table();
		private final Label sizeLabel = new Label("", Gui.skin());

		SetupScreen(ScreenManager.UIScreen prev) {
			super(prev);
		}

		protected abstract G newGame(@NonNull GameParameters params) throws IllegalArgumentException;

		final void startGame() {
			G game = newGame(params);
			if (game != null) {
				game.restart();
				ScreenManager.setScreen(game);
			}
		}

		final void updatePlayerList() {
			playerListTable.clearChildren();
			Player p;
			for (int pi = 0; pi < params.players.size; pi++) {
				final Actor a;
				p = params.players.get(pi);
				if (p instanceof LocalPlayer) {
					a = new Player.PlayerNameTextField(p.name, Gui.skin());
					((TextField) a).setTextFieldListener(new Player.PlayerTextFieldListener((byte) pi, this));
					((TextField) a).setMaxLength(20);
					((TextField) a).setOnlyFontChars(true);
					((TextField) a).setFocusTraversal(false);
				} else
					a = new Label(p.name, Gui.skin());
				a.setColor(p.color);
				playerListTable.add(a).growX();
				Cell c = playerListTable.add().fill();
				if (canRemovePlayer(p)) {
					final TextButton removePlayer = new TextButton("-", Gui.skin());
					final int pi_ = pi;
					removePlayer.addListener(new ClickListener() {
						@Override
						public void clicked(InputEvent event, float x, float y) {
							removePlayer(pi_);
						}
					});
					c.setActor(removePlayer).minWidth((Gui.buttonSizeSmall())).row();
				}
				playerListTable.row();
			}
		}

		void addPlayer(Player p) {
			if (params.players.size < Byte.MAX_VALUE - 1)
				params.players.add(p);
			updatePlayerList();
			playerListScroll.layout();
			playerListScroll.scrollTo(0, 0, 0, 0);
		}

		final void addPlayer() {
			Human h = new LocalPlayer();
			byte num = 1;
			for (Player p : params.players)
				if (p instanceof Human)
					num++;
			h.name = "Player " + num;
			addPlayer(h);
		}

		final void addBot() {
			Bot b = new Bot();
			byte num = 1;
			for (Player p : params.players)
				if (p.getClass() == b.getClass())
					num++;
			b.name = "Bot " + num;
			addPlayer(b);
		}

		protected boolean canRemovePlayer(Player p) {
			return false;
		}

		void removePlayer(int pi) {
			params.players.removeIndex(pi);
			updatePlayerList();
		}

		void changePlayerName(byte pi, String newName) {
			params.players.get(pi).name = newName;
			updatePlayerList();
		}

		void changePlayerColor(byte pi, Color newColor) {
			params.players.get(pi).color = newColor;
			updatePlayerList();
		}

		void changeBoardSize(byte newSize) {
			params.size = newSize;
			sizeLabel.setText("Board size: " + params.size);
			sizeLabel.pack();
		}

		@Override
		protected void buildUI() {
			// Title
			title.setAlignment(Align.center);
			tools.add(title).fill().spaceBottom(Gui.sparsityBig()).row();

			// Players
			playerTools.add(new Label("Players", Gui.skin())).row();
			playerTools.add(playerListScroll).growX().row();
			playerListScroll.setScrollingDisabled(true, false);
			playerListScroll.setOverscroll(false, true);

			final TextButton addHuman = new TextButton("Add Player", Gui.skin());
			addHuman.getLabel().setStyle(new Label.LabelStyle(Gui.instance().labelStyles.subTextStyle));
			addHuman.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					addPlayer();
				}
			});
			addPlayerTools.add(addHuman).fill().growX().minWidth(Gui.buttonSizeSmall()).padRight(Gui.sparsity() / 2);
			playerTools.add(addPlayerTools).fill().spaceBottom(Gui.sparsityBig()).row();

			tools.add(playerTools).growX().expandY().spaceBottom(Gui.sparsityBig()).row();
			updatePlayerList();

			// Size
			sizeTools.add(sizeLabel);
			tools.add(sizeTools).growX().row();
			changeBoardSize(params.size);

			tools.setWidth(Gdx.graphics.getWidth() - Gui.sparsityBig() * 2);
			Utils.centerXY(tools);
			tools.center();
			tools.act(0);
			ui.addActor(tools);
		}

		@Override
		public void show() {
			super.show();
			updatePlayerList();
		}
	}

	private static final float
			RING_SPARSITY_PERCENT = 1f,
			SELECT_PITCH_RANGE = .5f;

	private static float pieceSparsity() {
		return Gui.sparsity() * .75f;
	}

	static float getBoardSize() {
		return Math.min(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()) - Gui.sparsity() * 2;
	}

	private static float getBoardX() {
		return (Gdx.graphics.getWidth() - getBoardSize()) / 2;
	}

	private static float getBoardY() {
		return (Gdx.graphics.getHeight() - getBoardSize()) / 2;
	}

	private static float getRadius(byte boardSize) {
		return ((getBoardSize() - pieceSparsity() * (boardSize - 1)) / 2) / boardSize;
	}

	private static float getRingWidth(byte boardSize) {
		return (getRadius(boardSize) * (1 - RING_SPARSITY_PERCENT / 2)) / boardSize;
	}

	private static int segmentAmount(byte boardSize) {
		return (int) Gui.getScale() * 10 / boardSize;
	}

	private static void drawPiece(ShapeRenderer renderer, byte boardSize, float cx, float cy, byte i, Color color) {
		renderer.setColor(color);
		float r = getRadius(boardSize) - i * getRingWidth(boardSize) * (1 + RING_SPARSITY_PERCENT);
		renderer.circle(cx, cy, r, segmentAmount(boardSize));
		renderer.setColor(Gui.BG);
		renderer.circle(cx, cy, r - getRingWidth(boardSize), segmentAmount(boardSize));
	}

	static void drawPiece(ShapeRenderer renderer, byte boardSize, byte x, byte y, byte i, Color color) {
		drawPiece(
				renderer,
				boardSize,
				(Gdx.graphics.getWidth() - getBoardSize()) / 2 + getRadius(boardSize) + (getRadius(boardSize) * 2 + pieceSparsity()) * x,
				Gdx.graphics.getHeight() - ((Gdx.graphics.getHeight() - getBoardSize()) / 2 + getRadius(boardSize) + (getRadius(boardSize) * 2 + pieceSparsity()) * y),
				i,
				color
		);
	}

	static boolean isMovePossible(Move move, Player[][][] board, byte[] myPieces) {
		return move != null && myPieces[move.i] > 0 && board[move.x][move.y][move.i] == null;
	}

	private static Player checkWin(Player[][][] board) {
		Player p;
		// Search for line of same size
		for (byte i = 0; i < board.length; i++) {
			// Col |
			for (byte x = 0; x < board.length; x++) {
				p = board[x][0][i];
				for (byte y = 0; y < board.length; y++)
					if (board[x][y][i] != p) {
						p = null;
						break;
					}
				if (p != null)
					return p;
			}
			// Row -
			for (byte y = 0; y < board.length; y++) {
				p = board[0][y][i];
				for (byte x = 0; x < board.length; x++)
					if (board[x][y][i] != p) {
						p = null;
						break;
					}
				if (p != null)
					return p;
			}
			// Diagonal \
			p = board[0][0][i];
			for (byte b = 1; b < board.length; b++)
				if (board[b][b][i] != p) {
					p = null;
					break;
				}
			if (p != null)
				return p;
			// Diagonal /
			p = board[0][board.length - 1][i];
			for (byte b = 1; b < board.length; b++)
				if (board[b][board.length - 1 - b][i] != p) {
					p = null;
					break;
				}
			if (p != null)
				return p;
		}
		// Search for line of increasing size
		{
			// Col |
			for (byte x = 0, i; x < board.length; x++) {
				p = board[x][0][i = 0];
				for (byte y = 0; y < board.length; y++, i++)
					if (board[x][y][i] != p) {
						p = null;
						break;
					}
				if (p != null)
					return p;
			}
			// Row -
			for (byte y = 0, i; y < board.length; y++) {
				p = board[0][y][i = 0];
				for (byte x = 0; x < board.length; x++, i++)
					if (board[x][y][i] != p) {
						p = null;
						break;
					}
				if (p != null)
					return p;
			}
			// Diagonal \
			p = board[0][0][0];
			for (byte b = 1, i = 1; b < board.length; b++, i++)
				if (board[b][b][i] != p) {
					p = null;
					break;
				}
			if (p != null)
				return p;
			// Diagonal /
			p = board[0][board.length - 1][0];
			for (byte b = 1, i = 1; b < board.length; b++, i++)
				if (board[b][board.length - 1 - b][i] != p) {
					p = null;
					break;
				}
			if (p != null)
				return p;
		}
		// Search for line of decreasing size
		{
			// Col |
			for (byte x = 0, i; x < board.length; x++) {
				p = board[x][0][i = (byte) (board.length - 1)];
				for (byte y = 0; y < board.length; y++, i--)
					if (board[x][y][i] != p) {
						p = null;
						break;
					}
				if (p != null)
					return p;
			}
			// Row -
			for (byte y = 0, i; y < board.length; y++) {
				p = board[0][y][i = (byte) (board.length - 1)];
				for (byte x = 0; x < board.length; x++, i--)
					if (board[x][y][i] != p) {
						p = null;
						break;
					}
				if (p != null)
					return p;
			}
			// Diagonal \
			p = board[0][0][(byte) (board.length - 1)];
			for (byte b = 1, i = (byte) (board.length - 2); b < board.length; b++, i--)
				if (board[b][b][i] != p) {
					p = null;
					break;
				}
			if (p != null)
				return p;
			// Diagonal /
			p = board[0][board.length - 1][(byte) (board.length - 1)];
			for (byte b = 1, i = (byte) (board.length - 2); b < board.length; b++, i--)
				if (board[b][board.length - 1 - b][i] != p) {
					p = null;
					break;
				}
			if (p != null)
				return p;
		}

		// Search for stack
		for (byte x = 0; x < board.length; x++)
			for (byte y = 0; y < board.length; y++) {
				p = board[x][y][0];
				if (p != null) {
					for (byte i = 0; i < board.length; i++)
						if (board[x][y][i] != p) {
							p = null;
							break;
						}
					if (p != null)
						return p;
				}
			}

		return null;
	}

	private static void applyMove(@Nullable final Move move, final Array<Player> players, final byte pi, final Player[][][] board, final byte[][] pieces) {
		if (move == null)
			return;
		board[move.x][move.y][move.i] = players.get(pi);
		pieces[pi][move.i]--;
	}

	@NonNull
	final Player[][][] board;
	final byte[][] pieces;
	@NonNull
	final GameParameters params;
	private final Table subtitle = new Table();
	private final Label player = new Label("Player ", Gui.skin()), currentPlayer = new Label("", Gui.skin()), nextPlayer = new Label("", Gui.instance().labelStyles.subTextStyle);
	private final Label action = new Label("", Gui.skin());
	private int pi = -1;
	private boolean inputSuspended = false;
	@Nullable
	private Player winner = null;
	@Nullable
	private Move currentMove = null;

	GameScreen(ScreenManager.UIScreen prev, @NonNull GameParameters params) {
		super(prev);
		this.params = params;
		board = new Player[params.size][params.size][params.size];
		pieces = new byte[params.players.size][params.size];
		reset();
	}

	private void resumeGame() {
		inputSuspended = false;
	}

	void restart() {
		reset().start();
	}

	private GameScreen reset() {
		ui.getRoot().clearActions();
		winner = null;
		pi = -1;
		for (byte x = 0; x < params.size; x++)
			for (byte y = 0; y < params.size; y++)
				for (byte i = 0; i < params.size; i++)
					board[x][y][i] = null;
		for (byte pi = 0; pi < params.players.size; pi++)
			for (byte i = 0; i < params.size; i++)
				pieces[pi][i] = params.size;
		inputSuspended = true;
		return this;
	}

	private void start() {
		resumeGame();
		nextPlayer();
	}

	private void printBoard() {
		for (byte y = 0; y < params.size; y++) {
			for (byte x = 0; x < params.size; x++) {
				System.out.print("[");
				byte i = 0;
				for (; i < params.size - 1; i++) {
					System.out.print(board[x][y][i] + "[");
				}
				System.out.print(board[x][y][i]);
				for (i--; i >= 0; i--) {
					System.out.print("]" + board[x][y][i]);
				}
				System.out.print("] ");
			}
			System.out.print("\n");
		}
	}

	final byte getPI() {
		return (byte) pi;
	}

	protected void makeMove(Move move) {
		if (!(params.players.get(getPI()) instanceof LocalPlayer))
			getSelectSound(move.i).play();
		applyMove(move, params.players, getPI(), board, pieces);
		nextPlayer();
	}

	private void nextPlayer() {
		currentMove = null;
		inputSuspended = true;
		subtitle.setVisible(false);

		Player p = checkWin(board), next;
		if (p != null) {
			winner = p;
			if (Settings.moreInfo)
				printBoard();

			currentPlayer.setText(winner.name);
			currentPlayer.setColor(winner.color);
			action.setText(" Wins");

			AudioManager.newSFXGame(p instanceof LocalPlayer ? "win" : "lose").play();
		} else {
			pi = (byte) Utils.getNextIndex(params.players.items, getPI());
			boolean draw = true;
			for (byte i = 0; i < params.size && draw; i++)
				draw = pieces[getPI()][i] == 0;
			if (!draw) {
				draw = true;
				for (byte x = 0; x < params.size && draw; x++)
					for (byte y = 0; y < params.size && draw; y++)
						for (byte i = 0; i < params.size && draw; i++)
							//noinspection IfStatementMissingBreakInLoop
							if (board[x][y][i] == null)
								draw = false;
			}
			if (draw) {
				player.setText("");
				currentPlayer.setText("");
				action.setText("Draw");

				AudioManager.newSFXGame("draw").play();
			} else {
				p = params.players.get(getPI());
				next = params.players.get(Utils.getNextIndex(params.players.items, getPI()));

				currentPlayer.setColor(p.color);
				currentPlayer.setText(p.name);
				action.setText("'s Turn");
				nextPlayer.setColor(next.color);
				nextPlayer.setText(next.name);
				subtitle.setVisible(true);

				AudioManager.newSFXGame(p instanceof LocalPlayer ? "next_l" : "next").play();

				if (p instanceof Bot) {
					ui.addAction(Actions.sequence(
							Actions.delay(.5f),
							Actions.run(new Runnable() {
								@Override
								public void run() {
									Move move = ((Bot) params.players.get(getPI())).calculateMove(board, params.players, getPI(), pieces);
									if (move != null) makeMove(move);
								}
							})
					));
				} else if (p instanceof LocalPlayer) inputSuspended = false;
			}
		}
	}

	void removePlayer(byte pi) {
		params.players.removeIndex(pi);
		if (pi == getPI()) nextPlayer();
	}

	@Override
	protected void buildUI() {
		player.setStyle(Gui.skin().get(Label.LabelStyle.class));
		final Label next = new Label("Next: ", nextPlayer.getStyle());
		currentPlayer.setStyle(player.getStyle());
		action.setStyle(player.getStyle());
		nextPlayer.setStyle(next.getStyle());
		final Table header = new Table(Gui.skin()) {
			@Override
			public void act(float delta) {
				pack();
				Utils.centerX(this);
				Utils.centerY(this, (Gdx.graphics.getHeight() + getBoardSize()) / 2 + Gui.sparsity(), Gdx.graphics.getHeight() - Gui.sparsity());
				super.act(delta);
			}
		}, title = new Table(Gui.skin());
		subtitle.setSkin(Gui.skin());
		// title.add(player);
		title.add(currentPlayer);
		title.add(action);
		subtitle.add(next);
		subtitle.add(nextPlayer);
		header.add(title).padBottom(Gui.sparsity()).row();
		header.add(subtitle);

		final TextButton quit = new TextButton("Quit", Gui.skin());
		quit.setPosition(Gui.sparsity(), Gui.sparsity());
		quit.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				back();
			}
		});
		final TextButton confirmMove = new TextButton("Go", Gui.skin()) {
			@Override
			public void act(float delta) {
				setVisible(!inputSuspended && currentMove != null);
				super.act(delta);
			}
		};
		confirmMove.setSize(Gdx.graphics.getWidth() - Gui.sparsity() * 2, confirmMove.getPrefHeight() + Gui.sparsity());
		Utils.centerX(confirmMove);
		Utils.centerY(confirmMove, quit.getY() + quit.getHeight() + Gui.sparsity(), (Gdx.graphics.getHeight() - getBoardSize()) / 2 - Gui.sparsity());
		confirmMove.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				if (getPI() >= 0 && isMovePossible(currentMove, board, pieces[getPI()]))
					makeMove(currentMove);
				event.handle();
			}
		});

		ui.addActor(header);
		ui.addActor(confirmMove);
		ui.addActor(quit);

		ui.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				if (!event.isHandled())
					if (!inputSuspended) {
						Move move = new Move();
						if (getBoardX() <= x && x < getBoardX() + getBoardSize() && getBoardY() <= y && y < getBoardY() + getBoardSize()) {
							x -= getBoardX();
							y -= getBoardY();
							move.x = (byte) (x / getBoardSize() * params.size);
							move.y = (byte) (((getBoardSize() - y) / getBoardSize() * params.size));
							move.i = currentMove == null ? 0 : currentMove.i;
							if (currentMove != null && (currentMove.x == move.x && currentMove.y == move.y))
								do
									move.i = (byte) (move.i == params.size - 1 ? 0 : move.i + 1);
								while (!isMovePossible(move, board, pieces[getPI()]) && move.i != currentMove.i);
							else
								for (byte i = 0; i < params.size && !isMovePossible(move, board, pieces[getPI()]); i++)
									move.i = i;

							selectMove(move);
						}
					}
			}
		});

		ui.act(0);
	}

	void selectMove(Move move) {
		if (isMovePossible(move, board, pieces[getPI()])) {
			currentMove = move;
			getSelectSound(currentMove.i).play();
		}
	}

	@Override
	protected void draw() {
		ShapeRenderer renderer = Gui.SR;
		renderer.begin(ShapeRenderer.ShapeType.Filled);
		for (byte y = 0; y < params.size; y++)
			for (byte x = 0; x < params.size; x++)
				for (byte i = 0; i < params.size; i++) {
					Color c;
					if (currentMove != null && getPI() >= 0 && x == currentMove.x && y == currentMove.y && i == currentMove.i)
						c = Color.BLACK;
					else if (board[x][y][i] != null)
						c = board[x][y][i].color;
					else
						c = Color.WHITE;
					drawPiece(renderer, params.size, x, y, i, c);
				}
		renderer.end();
		super.draw();
	}

	@Override
	public final boolean back() {
		onQuit();
		return super.back();
	}

	protected void onQuit() {
	}

	private AudioManager.SoundAdapter getSelectSound(int i) {
		AudioManager.SoundAdapter s = AudioManager.newSFXGame("select");
		s.setPitch(1 - SELECT_PITCH_RANGE / 2 + SELECT_PITCH_RANGE * ((float) i / (params.size - 1)));
		return s;
	}
}
