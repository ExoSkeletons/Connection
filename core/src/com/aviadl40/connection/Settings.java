package com.aviadl40.connection;

import com.badlogic.gdx.Gdx;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

@SuppressWarnings({"WeakerAccess"})
public final class Settings {
	public static boolean
			DEV_MODE = true,
			musicEnabled = true,
			sfxEnabled = true,
			drawBorders = false,
			moreInfo = false,
			BT_READY = true,
			NET_READY = false;

	public static boolean save() {
		try (ObjectOutputStream os = new ObjectOutputStream(Gdx.files.local(Const.FileList.settings.getPath()).write(false))) {
			os.writeBoolean(musicEnabled);
			os.writeBoolean(sfxEnabled);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static boolean load() {
		if (!Gdx.files.local(Const.FileList.settings.getPath()).exists())
			return save();
		ObjectInputStream is = null;
		try {
			is = new ObjectInputStream(Gdx.files.local(Const.FileList.settings.getPath()).read());
			musicEnabled = is.readBoolean();
			sfxEnabled = is.readBoolean();
		} catch (EOFException ignored) {
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException ignored) {
				}
		}
		return true;
	}
}
