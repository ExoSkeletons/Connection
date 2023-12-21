package com.aviadl40.connection.game;

import android.support.annotation.NonNull;

import com.aviadl40.connection.game.screens.GameScreen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

public class Player {
	public static abstract class Human extends Player {
		protected Human(@NonNull String name) {
			super(name);
		}
	}

	public static final class LocalPlayer extends Human {
		public LocalPlayer() {
			super("Player");
		}
	}

	@NonNull
	public String name;
	@NonNull
	public
	Color color = new Color(MathUtils.random(), MathUtils.random(), MathUtils.random(), 1);

	Player(@NonNull String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}
