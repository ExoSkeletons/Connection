package com.aviadl40.connection.game;

import com.aviadl40.connection.game.screens.GameScreen;
import com.badlogic.gdx.utils.Array;

public final class GameParameters {
	public static final byte MAX_SIZE = 9;

	public byte size = 3;
	public Array<GameScreen.Player> players = new Array<>();

	public GameParameters() {
	}
}
