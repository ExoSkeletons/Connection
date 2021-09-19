package com.aviadl40.connection;

import android.os.AsyncTask;

import java.io.Closeable;
import java.io.IOException;

public abstract class BTSocketTask<Socket extends Closeable, Progress> extends AsyncTask<Object, Progress, Void> implements Closeable {
	final Socket socket;

	BTSocketTask(Socket socket) {
		this.socket = socket;
	}

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
