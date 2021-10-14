package com.aviadl40.connection.game.screens;

import com.aviadl40.connection.GdxUtils;
import com.aviadl40.connection.Gui;
import com.aviadl40.connection.game.managers.ScreenManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

public final class HelpScreen extends ScreenManager.UIScreen {
	private static final byte size = 3;
	private static final Array<Color> COLORS = Array.with(
			Color.RED, Color.BLUE, Color.BROWN, Color.CYAN,
			Color.YELLOW, Color.GREEN, Color.GRAY, Color.GOLD,
			Color.CORAL, Color.FOREST, Color.LIME,
			Color.PURPLE, Color.MAGENTA, Color.PINK
	);
	private static Color rowSame, diaSame, one, diaSize;

	public HelpScreen(ScreenManager.UIScreen prev) {
		super(prev);
	}

	@Override
	protected void buildUI() {
		Label text = new Label(
				"Create a line across the board, straight or diagonal- of increasing, decreasing or same size to win.",
				Gui.instance().labelStyles.subTextStyle
		);
		text.setAlignment(Align.center);
		text.setWrap(true);
		text.setWidth(Gdx.graphics.getWidth() - Gui.sparsity() * 2);
		GdxUtils.centerX(text);
		GdxUtils.centerY(text, (Gdx.graphics.getHeight() + GameScreen.getBoardSize()) / 2 + Gui.sparsity(), Gdx.graphics.getHeight() - Gui.sparsity());
		ui.addActor(text);

		TextButton exit = new TextButton("Got it!", Gui.skin());
		exit.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				back();
			}
		});
		GdxUtils.centerX(exit);
		GdxUtils.centerY(exit, Gui.sparsity(), (Gdx.graphics.getHeight() - GameScreen.getBoardSize()) / 2 - Gui.sparsity());
		ui.addActor(exit);
	}

	@Override
	protected void draw() {
		Gui.SR.begin(ShapeRenderer.ShapeType.Filled);
		Color c;
		for (byte x = 0; x < size; x++)
			for (byte y = 0; y < size; y++)
				for (byte i = 0; i < size; i++) {
					c = Color.WHITE;
					if (y == size - 1 && x == 1)
						c = one;
					else if (y == 1 && i == size - 1)
						c = rowSame;
					else if (x == y && i == 0) {
						c = diaSame;
					} else if (x == size - 1 - y && i == y)
						c = diaSize;

					GameScreen.drawPiece(
							Gui.SR, size, x, y, i,
							c
					);
				}
		Gui.SR.end();

		super.draw();
	}

	@Override
	public void show() {
		COLORS.shuffle();
		rowSame = COLORS.get(0);
		diaSame = COLORS.get(1);
		one = COLORS.get(2);
		diaSize = COLORS.get(3);
		super.show();
	}
}
