package com.aviadl40.connection.game.screens;

import com.aviadl40.connection.GdxUtils;
import com.aviadl40.connection.Gui;
import com.aviadl40.connection.Settings;
import com.aviadl40.connection.Utils;
import com.aviadl40.connection.game.managers.AudioManager;
import com.aviadl40.connection.game.managers.ScreenManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

public final class SettingsScreen extends ScreenManager.UIScreen {
	public SettingsScreen(ScreenManager.UIScreen prev) {
		super(prev);
	}

	@Override
	protected void buildUI() {
		final Table settingsTable = new Table(Gui.skin());
		final CheckBox musicToggle = new CheckBox("Music", Gui.skin()), sfxToggle = new CheckBox("SFX", Gui.skin());
		musicToggle.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				Settings.musicEnabled = musicToggle.isChecked();
				Settings.save();
				if (!Settings.musicEnabled)
					AudioManager.stopMusic();
				else
					AudioManager.playTrack(AudioManager.Track.title, true);
			}
		});
		musicToggle.setChecked(Settings.musicEnabled);
		sfxToggle.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				Settings.sfxEnabled = sfxToggle.isChecked();
				Settings.save();
			}
		});
		sfxToggle.setChecked(Settings.sfxEnabled);
		final Label musicStatus = new Label("", Gui.skin()) {
			@Override
			public void act(float delta) {
				setText(Settings.musicEnabled ? "On" : "Off");
				super.act(delta);
			}
		}, sfxStatus = new Label("", Gui.skin()) {
			@Override
			public void act(float delta) {
				setText(Settings.sfxEnabled ? "On" : "Off");
				super.act(delta);
			}
		};

		settingsTable.add(musicToggle).padRight(Gui.sparsity());
		settingsTable.add(musicStatus).row();
		settingsTable.add(sfxToggle).padRight(Gui.sparsity());
		settingsTable.add(sfxStatus);
		settingsTable.center();
		settingsTable.setWidth(Gdx.graphics.getWidth() - Gui.sparsity() * 2);
		settingsTable.act(0);
		GdxUtils.centerXY(settingsTable);

		final Label
				title = new Label("Settings", Gui.instance().labelStyles.headerStyle),
				credits = new Label("Music by\nKevin Macleod incompetech.com\nSFX by\nmixkit.co, soundbible.com", Gui.instance().labelStyles.subTextStyle);
		GdxUtils.centerX(title);
		title.setY(Gdx.graphics.getHeight() - Gui.sparsity() - title.getHeight());

		credits.setAlignment(Align.center);
		GdxUtils.centerX(credits);
		credits.setY(Gui.sparsity());

		ui.addActor(title);
		ui.addActor(settingsTable);
		ui.addActor(credits);
	}
}
