package com.aviadl40.connection.game.screens;

import android.support.annotation.NonNull;

import com.aviadl40.connection.Connection;
import com.aviadl40.connection.GdxUtils;
import com.aviadl40.connection.Gui;
import com.aviadl40.connection.Settings;
import com.aviadl40.connection.game.managers.AudioManager;
import com.aviadl40.connection.game.managers.ScreenManager;
import com.aviadl40.connection.game.managers.ScreenManager.UIScreen;
import com.aviadl40.gdxbt.core.BluetoothManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import static com.aviadl40.utils.Utils.toSingleLine;

public class MainMenuScreen extends UIScreen {
	@NonNull
	private final BluetoothManager btManager;

	private Label t2, t4;

	public MainMenuScreen(@NonNull BluetoothManager btManager) {
		super(null);
		this.btManager = btManager;
	}

	@Override
	public void buildUI() {
		/* Title */
		final Table title = new Table(Gui.skin());
		final Label
				t1 = new Label("C", Gui.instance().labelStyles.titleStyle),
				t3 = new Label("nnecti", t1.getStyle()),
				t5 = new Label("n", t1.getStyle());
		t2 = new Label("o", t1.getStyle());
		t4 = new Label(t2.getText(), t2.getStyle());
		title.add(t1);
		title.add(t2).padLeft(Gui.getScale() * .4f).padRight(Gui.getScale() * .4f);
		title.add(t3);
		title.add(t4).padLeft(Gui.getScale() * .4f).padRight(Gui.getScale() * .4f);
		title.add(t5);
		title.pack();
		GdxUtils.centerX(title);
		title.setY(Gdx.graphics.getHeight() - Gui.sparsity() - title.getHeight() * title.getScaleY());
		ui.addActor(title);
		/* Play */
		final Table playMenu = new Table(Gui.skin());
		playMenu.setWidth(Gdx.graphics.getWidth() - Gui.sparsity() * 2);
		// Play as Host
		final TextButton playHost = new TextButton("Play", Gui.skin());
		playHost.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				ScreenManager.setScreen(new HostGameScreen.HostSetupScreen(MainMenuScreen.this));
			}
		});
		playMenu.add(playHost).fill().expandX().row();
		// Play with Bluetooth
		if (btManager.bluetoothSupported() && (Settings.BT_READY || Settings.DEV_MODE)) {
			final TextButton playBT = new TextButton("Connect with\nBluetooth", Gui.skin());
			playBT.addListener(new ClickListener() {
				@Override
				public void clicked(final InputEvent event, final float x, final float y) {
					ScreenManager.setScreen(new ClientBluetoothGameScreen.ClientBluetoothSetupScreen(MainMenuScreen.this));
				}
			});
			playBT.getLabel().setStyle(Gui.instance().labelStyles.subTextStyle);
			playMenu.add(playBT).fill().expandX().padTop(Gui.sparsity() * 2).row();
		}
		// Play with Online
		if (Settings.NET_READY) {
			final TextButton playNet = new TextButton("Play Online", Gui.skin());
			playNet.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
				}
			});
			playMenu.add(playNet).fill().expandX().padTop(Gui.sparsity() * 2).row();
		}
		playMenu.setHeight(playMenu.getPrefHeight());
		GdxUtils.centerXY(playMenu);
		ui.addActor(playMenu);
		/* Bottom bar */
		final Table bar = new Table(Gui.skin());
		final TextButton.TextButtonStyle barStyle = new TextButton.TextButtonStyle(Gui.skin().get(TextButton.TextButtonStyle.class));
		barStyle.font = Gui.instance().labelStyles.subTextStyle.font;
		final TextButton settings = new TextButton("Settings", barStyle);
		settings.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				ScreenManager.setScreen(new SettingsScreen(MainMenuScreen.this));
			}
		});
		bar.add(settings).spaceRight(Gui.sparsity());
		//*
		final TextButton help = new TextButton("Help", barStyle);
		help.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				ScreenManager.setScreen(new HelpScreen(MainMenuScreen.this));
			}
		});
		bar.add(help).row();
		final TextButton pp = new TextButton("Privacy Policy", barStyle);
		pp.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				Gdx.net.openURI(Connection.POLICY);
			}
		});
		bar.add(pp).colspan(2);
		//*/
		GdxUtils.centerX(bar);
		bar.setY(Gui.sparsityBig());
		ui.addActor(bar);
		/* Dev tools */
		if (Settings.DEV_MODE) {
			final Group debugButtons = new Group();
			// Borders
			final CheckBox toggleBorders = new CheckBox("draw\nborders", Gui.skin()) {
				@Override
				public void act(float delta) {
					super.act(delta);
					setChecked(Settings.drawBorders);
				}
			};
			toggleBorders.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent changeEvent, Actor actor) {
					Settings.drawBorders = toggleBorders.isChecked();
				}
			});
			debugButtons.addActor(toggleBorders);
			// More info
			final CheckBox toggleExtraInfo = new CheckBox("extra\ninfo", Gui.skin()) {
				@Override
				public void act(float delta) {
					super.act(delta);
					setChecked(Settings.moreInfo);
				}
			};
			toggleExtraInfo.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent changeEvent, Actor actor) {
					Settings.moreInfo = toggleExtraInfo.isChecked();
				}
			});
			debugButtons.addActor(toggleExtraInfo);
			CheckBox c;
			for (int i = 0; i < debugButtons.getChildren().size; i++) {
				c = (CheckBox) debugButtons.getChildren().get(i);
				c.setSize(Gui.buttonSize(), Gui.buttonSize());
				c.setX(Gui.sparsity() + (c.getWidth() + Gui.sparsity()) * i);
				c.setY(Gdx.graphics.getHeight() - (Gui.buttonSize() + Gui.sparsity()) * 2);
				c.setName(toSingleLine(c.getText().toString()));
			}
			ui.addActor(debugButtons);
		}
	}

	@Override
	public void show() {
		super.show();
		t2.setColor(new Color((float) Math.random(), (float) Math.random(), (float) Math.random(), 1));
		t4.setColor(new Color((float) Math.random(), (float) Math.random(), (float) Math.random(), 1));

		AudioManager.playTrack(AudioManager.Track.title, false);

		btManager.setBluetoothListener(null);
	}

	@Override
	public boolean back() {
		Gdx.app.exit();
		return false;
	}
}
