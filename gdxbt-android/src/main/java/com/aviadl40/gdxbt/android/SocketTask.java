package com.aviadl40.gdxbt.android;

import android.os.AsyncTask;

import java.io.Closeable;
import java.io.IOException;

public abstract class SocketTask<Socket extends Closeable, Progress, Result> extends AsyncTask<Void, Progress, Result> implements Closeable {
	private final Socket socket;

	SocketTask(Socket socket) {
		this.socket = socket;
	}

	@Override
	protected final Result doInBackground(Void... args) {
		return doInBackground(socket);
	}

	protected abstract Result doInBackground(Socket socket);

	@Override
	protected void onCancelled() {
		close();
	}

	@Override
	public void close() {
		try {
			socket.close();
			if (getStatus() != Status.FINISHED)
				cancel(true);
		} catch (IOException ignored) {
		}
	}
}
