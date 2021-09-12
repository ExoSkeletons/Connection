package com.aviadl40.connection.game.managers;

import com.aviadl40.connection.Settings;
import com.aviadl40.connection.Const;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Array;

public final class AudioManager {
	public enum Track {
		title,
		game,;

		public String getPath() {
			return Const.Folder.music.getChildPath(name());
		}

		public Music getMusic(AssetManager assetManager) {
			return assetManager.get(getPath(), Music.class);
		}

		public boolean isLoaded(AssetManager assetManager) {
			return assetManager.isLoaded(getPath(), Music.class);
		}
	}

	@SuppressWarnings("unused")
	public static final class SoundAdapter {
		static final SoundAdapter SILENCE = new SoundAdapter(null);

		private final Sound sound;
		private long id = -1;
		private float volume = 1, pitch = 1, pan = 0;
		private boolean looping = false;

		private SoundAdapter(Sound sound) {
			this.sound = sound;
		}

		private void validate() {
			if (!isValid() && !isSilent())
				sound.pause(id = sound.play());
		}

		private boolean isValid() {
			return id >= 0;
		}

		private boolean isSilent() {
			return sound == null;
		}

		public void play() {
			resume();
		}

		public void stop() {
			if (isValid())
				sound.stop(id);
		}

		void pause() {
			if (isValid())
				sound.pause(id);
		}

		void resume() {
			validate();
			if (isValid() && !isSilent()) {
				sound.setLooping(id, looping);
				sound.setVolume(id, volume);
				sound.setPitch(id, pitch);
				sound.setPan(id, pan, volume);
				sound.resume(id);
			}
		}

		public boolean isLooping() {
			return looping;
		}

		public SoundAdapter setLooping(boolean looping) {
			this.looping = looping;
			return this;
		}

		public float getVolume() {
			return volume;
		}

		public SoundAdapter setVolume(float volume) {
			this.volume = volume;
			return this;
		}

		public float getPitch() {
			return pitch;
		}

		public SoundAdapter setPitch(float pitch) {
			this.pitch = pitch;
			return this;
		}

		public float getPan() {
			return pan;
		}

		public SoundAdapter setPan(float pan) {
			this.pan = pan;
			return this;
		}
	}

	private static AssetManager assetManager;
	private static Track current = null;

	public static void reload(AssetManager assetManager) {
		AudioManager.assetManager = assetManager;
		stopMusic();
	}

	private static boolean trackValid(Track track) {
		return track != null && track.isLoaded(assetManager);
	}

	public static Array<Track> validTracks() {
		Array<Track> tracks = new Array<>();
		for (Track track : Track.values())
			if (trackValid(track))
				tracks.add(track);
		return tracks;
	}

	public static void playTrack(Track track, boolean forceRestart) {
		if (current != track || forceRestart) {
			if (trackValid(current)) {
				Music m = current.getMusic(assetManager);
				m.pause();
				m.setPosition(0);
			}
			current = track;
			if (!Settings.musicEnabled)
				return;
			if (trackValid(current)) {
				Music m = current.getMusic(assetManager);
				m.setLooping(true);
				m.setPosition(0);
				m.play();
			}
		}
	}

	public static void playRandom(Track... exclude) {
		Array<Track> tracks = new Array<>(validTracks());
		for (Track t : exclude)
			tracks.removeValue(t, true);
		playTrack(tracks.random(), false);
	}

	public static Track nowPlaying() {
		return current;
	}

	public static void stopMusic() {
		playTrack(null, true);
	}

	private static SoundAdapter newSFX(String... SFXDesc) {
		if (!Settings.sfxEnabled)
			return SoundAdapter.SILENCE;
		try {
			String soundPath = SFXDesc[0];
			for (int i = 1; i < SFXDesc.length; i++)
				soundPath += "/" + SFXDesc[i];
			return new SoundAdapter(assetManager.get(Const.Folder.sound.getChildPath(soundPath), Sound.class));
		} catch (Exception e) {
			if (Settings.DEV_MODE)
				System.err.println(e.getMessage());
			return SoundAdapter.SILENCE;
		}
	}

	public static SoundAdapter newSFXGame(String name) {
		return newSFX("game", name);
	}

	public static SoundAdapter newSFXActions(String name) {
		return newSFX("actions", name);
	}

	public static SoundAdapter newSFXMisc(String name) {
		return newSFX("misc", name);
	}
}