package com.aviadl40.connection.game.screens;

import com.aviadl40.connection.GdxUtils;
import com.aviadl40.connection.Gui;
import com.aviadl40.connection.Utils;
import com.aviadl40.connection.game.managers.ScreenManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;

public abstract class LoadingScreen extends ScreenManager.UIScreen {
	private final AssetManager assetManager;
	private final Label loadingLabel = new Label("", Gui.instance().labelStyles.subTextStyle);
	private final String desc;
	private boolean finishedLoading = false;

	private LoadingScreen(ScreenManager.UIScreen prev, AssetManager assetManager, String desc) {
		super(prev);
		this.assetManager = assetManager;
		this.desc = desc;
		update(0);
	}

	protected LoadingScreen(AssetManager assetManager, String desc) {
		this(null, assetManager, desc);
	}

	@Override
	public void buildUI() {
		loadingLabel.setWrap(true);
		loadingLabel.setAlignment(Align.center);
		ui.addActor(loadingLabel);
	}

	@Override
	protected void update(float delta) {
		if (!assetManager.update()) {
			loadingLabel.setText("Loading " + desc + "... " + (int) (assetManager.getProgress() * 100) + "%");
		} else if (!finishedLoading) {
			loadingLabel.setText("Loading Complete.");
			onFinish();
			finishedLoading = true;
		}
		loadingLabel.setSize(
				Math.min(loadingLabel.getPrefWidth(), Gdx.graphics.getWidth() - Gui.sparsity() * 2),
				Math.min(loadingLabel.getPrefHeight(), Gdx.graphics.getHeight() - Gui.sparsity() * 2)
		);
		GdxUtils.centerXY(loadingLabel);
		super.update(delta);
	}

	protected abstract void onFinish();

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + loadingLabel.getText() + "]";
	}
}
