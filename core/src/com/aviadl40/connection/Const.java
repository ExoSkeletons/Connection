package com.aviadl40.connection;

import com.aviadl40.connection.Utils.Extension;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

public final class Const {
	public enum Folder {
		textures(Extension.png),

		audio,
		music(audio, Extension.mp3),
		sound(audio, Extension.mp3),

		bin(Extension.bin);

		public final Extension extension;
		private final Folder parent;

		Folder(Folder parent, Extension extension) {
			this.parent = parent;
			this.extension = extension;
		}

		Folder(Folder parent) {
			this(parent, parent.extension);
		}

		Folder(Extension extension) {
			this(null, extension);
		}

		Folder() {
			this(null, null);
		}

		public String getPath() {
			return (parent == null
					? ""
					: parent.getPath()
			) + name() + "/";
		}

		public FileHandle getHandle(Files.FileType fileType) {
			return Gdx.files.getFileHandle(getPath(), fileType);
		}

		public String getChildPath(String fileName) {
			return getPath() + fileName + (extension == null ? "" : extension.toString());
		}

		public FileHandle getChildHandle(String fileName, Files.FileType fileType) {
			return Gdx.files.getFileHandle(getChildPath(fileName), fileType);
		}

		@Override
		public String toString() {
			return getPath();
		}
	}

	enum FileList {
		title(Folder.textures),
		settings(Folder.bin),
		;

		private final Folder parent;
		private final Extension extension;

		FileList(Folder parent, Extension extension) {
			this.parent = parent;
			this.extension = extension;
		}

		FileList(Folder parent) {
			this(parent, parent.extension);
		}

		public String getPath() {
			return parent.toString() + name() + (extension == null ? "" : extension.toString());
		}

		public FileHandle getHandle(Files.FileType type) {
			return Gdx.files.getFileHandle(getPath(), type);
		}

		@Override
		public String toString() {
			return getPath();
		}
	}
}