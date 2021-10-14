package com.aviadl40.connection;

import com.aviadl40.utils.Utils;
import com.aviadl40.connection.game.managers.AudioManager;
import com.aviadl40.connection.game.managers.BluetoothManager;
import com.aviadl40.connection.game.managers.BluetoothManager.BluetoothConnectedDeviceInterface;
import com.aviadl40.connection.game.managers.BluetoothManager.BluetoothPairedDeviceInterface;
import com.aviadl40.connection.game.managers.PermissionsManager;
import com.aviadl40.connection.game.managers.ScreenManager;
import com.aviadl40.connection.game.screens.ClientBluetoothGameScreen.ClientBluetoothSetupScreen;
import com.aviadl40.connection.game.screens.HelpScreen;
import com.aviadl40.connection.game.screens.HostGameScreen.HostSetupScreen;
import com.aviadl40.connection.game.screens.LoadingScreen;
import com.aviadl40.connection.game.screens.SettingsScreen;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import java.util.UUID;

public final class Connection extends Game {
	public static final UUID BT_UUID = UUID.fromString("270af353-7ee2-438d-ba2f-c007c7c7880f");
	public static Connection instance;
	public static BluetoothManager<BluetoothPairedDeviceInterface, BluetoothConnectedDeviceInterface> btManager;
	private static PermissionsManager permManager;
	private static com.aviadl40.utils.Utils.LoadState loadState = com.aviadl40.utils.Utils.LoadState.UNLOADED;

	private final AssetManager assetManager = new AssetManager();

	private ScreenManager.UIScreen mainMenuScreen;

	public Connection(PermissionsManager PermManager, BluetoothManager btManager) {
		instance = this;
		Connection.permManager = PermManager;
		//noinspection unchecked
		Connection.btManager = btManager;
	}

	private void reloadAssets() {
		if (loadState == com.aviadl40.utils.Utils.LoadState.LOADED) dispose();
		loadState = com.aviadl40.utils.Utils.LoadState.LOADING;

		Settings.load();

		Gui.recreateBatches();

		assetManager.load(Gui.SKIN_PATH, Skin.class);
		GdxUtils.loadAssetFilesRecursively(assetManager, Gdx.files.internal(Const.Folder.music.getPath()), Const.Folder.music.extension, Music.class);
		assetManager.finishLoading();
		Gui.reload(assetManager.get(Gui.SKIN_PATH, Skin.class));
		AudioManager.reload(assetManager);

		GdxUtils.loadAssetFilesRecursively(assetManager, Gdx.files.internal(Const.Folder.textures.getPath()), Const.Folder.textures.extension, Texture.class);
		GdxUtils.loadAssetFilesRecursively(assetManager, Gdx.files.internal(Const.Folder.sound.getPath()), Const.Folder.sound.extension, Sound.class);
		ScreenManager.setScreen(new LoadingScreen(assetManager, "") {
			@Override
			protected void onFinish() {
				// Build resources
				loadState = com.aviadl40.utils.Utils.LoadState.LOADED;
				ui.addAction(Actions.sequence(
						Actions.fadeOut(.25f),
						Actions.delay(.5f),
						Actions.run(new Runnable() {
							@Override
							public void run() {
								home();
							}
						})
				));
			}
		});
	}

	@Override
	public void create() {
		if (loadState == com.aviadl40.utils.Utils.LoadState.LOADED)
			dispose();
		mainMenuScreen = new ScreenManager.UIScreen(null) {
			Label t2, t4;

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
						ScreenManager.setScreen(new HostSetupScreen(mainMenuScreen));
					}
				});
				playMenu.add(playHost).fill().expandX().row();
				// Play with Bluetooth
				if (btManager.bluetoothSupported() && (Settings.BT_READY || Settings.DEV_MODE)) {
					final TextButton playBT = new TextButton("Connect with\nBluetooth", Gui.skin());
					playBT.addListener(new ClickListener() {
						@Override
						public void clicked(InputEvent event, float x, float y) {
							ScreenManager.setScreen(new ClientBluetoothSetupScreen(mainMenuScreen));
						}
					});
					playBT.getLabel().setStyle(Gui.instance().labelStyles.subTextStyle);
					playMenu.add(playBT).fill().expandX().padTop(Gui.sparsity() * 2).row();
				}
				// Play with Bluetooth
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
				final TextButton settings = new TextButton("Settings", Gui.skin());
				settings.getLabel().setStyle(new Label.LabelStyle(Gui.instance().labelStyles.subTextStyle));
				settings.addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						ScreenManager.setScreen(new SettingsScreen(mainMenuScreen));
					}
				});
				bar.add(settings);
				//*
				final TextButton help = new TextButton("Help", Gui.skin());
				help.getLabel().setStyle(new Label.LabelStyle(Gui.instance().labelStyles.subTextStyle));
				help.addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						ScreenManager.setScreen(new HelpScreen(mainMenuScreen));
					}
				});
				bar.add(help).padLeft(Gui.sparsity());
				//*/
				GdxUtils.centerX(bar);
				bar.setY(Gui.sparsity());
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
						c.setName(com.aviadl40.utils.Utils.toSingleLine(c.getText().toString()));
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
		};
		Gdx.gl.glClearColor(Gui.BG.r, Gui.BG.g, Gui.BG.b, Gui.BG.a);
		Gdx.input.setCatchKey(Input.Keys.BACK, true);
		resume();
	}

	@Override
	public void dispose() {
		Gui.instance().dispose();
		Gui.disposeBatches();
		super.dispose();
		Settings.save();
		ScreenManager.current().dispose();
		assetManager.clear();
		loadState = com.aviadl40.utils.Utils.LoadState.UNLOADED;
	}

	@Override
	public void resume() {
		ScreenManager.setScreen(ScreenManager.current());
		if (loadState == Utils.LoadState.UNLOADED)
			reloadAssets();
		super.resume();
	}

	void home() {
		ScreenManager.setScreen(mainMenuScreen);
	}
}