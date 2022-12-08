package com.aviadl40.connection;

import com.aviadl40.connection.game.managers.AudioManager;
import com.aviadl40.connection.game.managers.ScreenManager;
import com.aviadl40.connection.game.screens.LoadingScreen;
import com.aviadl40.connection.game.screens.MainMenuScreen;
import com.aviadl40.gdxbt.core.BluetoothManager;
import com.aviadl40.gdxbt.core.BluetoothManager.BluetoothConnectedDeviceInterface;
import com.aviadl40.gdxbt.core.BluetoothManager.BluetoothPairedDeviceInterface;
import com.aviadl40.gdxperms.core.PermissionsManager;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import java.util.UUID;

import static com.aviadl40.utils.Utils.LoadState;

public final class Connection extends Game {
	public static final UUID BT_UUID = UUID.fromString("270af353-7ee2-438d-ba2f-c007c7c7880f");
	private static final String POLICY = "https://pastebin.com/2U6gWmuL";
	public static Connection instance;
	public static BluetoothManager<BluetoothPairedDeviceInterface, BluetoothConnectedDeviceInterface> btManager;
	private static PermissionsManager permManager;
	private static LoadState loadState = LoadState.UNLOADED;

	private final AssetManager assetManager = new AssetManager();

	private ScreenManager.UIScreen mainMenuScreen;

	public Connection(PermissionsManager PermManager, BluetoothManager btManager) {
		instance = this;
		Connection.permManager = PermManager;
		//noinspection unchecked
		Connection.btManager = btManager;
	}

	private void reloadAssets() {
		if (loadState == LoadState.LOADED) dispose();
		loadState = LoadState.LOADING;

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
				loadState = LoadState.LOADED;
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
		if (loadState == LoadState.LOADED)
			dispose();
		mainMenuScreen = new MainMenuScreen(btManager, POLICY);
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
		loadState = LoadState.UNLOADED;
	}

	@Override
	public void resume() {
		ScreenManager.setScreen(ScreenManager.current());
		if (loadState == LoadState.UNLOADED)
			reloadAssets();
		super.resume();
	}

	void home() {
		ScreenManager.setScreen(mainMenuScreen);
	}
}