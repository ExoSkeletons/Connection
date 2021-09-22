package com.aviadl40.connection;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.aviadl40.connection.game.managers.BluetoothManager;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

abstract class BTDeviceAdapter {
	public static final class BTConnectedDeviceAdapter extends BTDeviceAdapter implements BluetoothManager.BluetoothConnectedDeviceInterface {
		private BluetoothSocket socket;

		BTConnectedDeviceAdapter(BluetoothSocket socket) {
			this.socket = socket;
		}

		@Override
		public void closeConnection() {
			if (socket != null)
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					socket = null;
				}
		}

		@NonNull
		InputStream getInputStream() throws IOException {
			if (socket == null) throw new IOException("Socket not opened.");
			return socket.getInputStream();
		}

		@NonNull
		OutputStream getOutputStream() throws IOException {
			if (socket == null) throw new IOException("Socket not opened.");
			return socket.getOutputStream();
		}

		@Override
		public String getName() {
			return getDevice().getName();
		}

		@NonNull
		@Override
		BluetoothDevice getDevice() {
			return socket.getRemoteDevice();
		}
	}

	static final class BTPairedDeviceAdapter extends BTDeviceAdapter implements BluetoothManager.BluetoothPairedDeviceInterface {
		// Client connection task
		static final class BTConnectToHostTask extends BTSocketTask<BluetoothSocket, Void> {
			private final BluetoothManager.BluetoothListener btListener;

			BTConnectToHostTask(@NonNull BluetoothSocket connectionSocket, BluetoothManager.BluetoothListener btListener) {
				super(connectionSocket);
				this.btListener = btListener;
			}

			@Override
			protected Void doInBackground(Object... params) {
				try {
					// Try and connect to host
					// NOTE: Cancelling the task closes the socket and closing the socket aborts
					// the blocking done by connect() so we do not need to worry.
					socket.connect();
				} catch (IOException e) {
					e.printStackTrace();
					cancel(true);
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void aVoid) {
				BTConnectedDeviceAdapter connected = new BTConnectedDeviceAdapter(socket);
				btListener.onDeviceConnected(connected);
				btListener.onConnectedToDevice(connected);
			}
		}

		@NonNull
		private final BluetoothDevice device;
		@Nullable
		private BTConnectToHostTask connectTask = null;

		BTPairedDeviceAdapter(@NonNull BluetoothDevice device) {
			this.device = device;
		}

		@Override
		public Closeable connect(UUID verificationUUID, BluetoothManager.BluetoothListener listener) {
			if (connectTask != null) connectTask.cancel(true);
			try {
				BluetoothSocket connectionSocket = device.createRfcommSocketToServiceRecord(verificationUUID);
				connectTask = new BTConnectToHostTask(connectionSocket, listener);
				connectTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				// NOTE: we do not close the socket, as the connect task just got it and needs it open.
				// The connect task therefore is now the one in charge of closing the socket after it's done.
				return connectTask;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public String getName() {
			return device.getName();
		}

		@NonNull
		@Override
		BluetoothDevice getDevice() {
			return device;
		}
	}

	@NonNull
	abstract BluetoothDevice getDevice();

	@Override
	public boolean equals(@Nullable Object obj) {
		return obj instanceof BTDeviceAdapter && getDevice().equals(((BTDeviceAdapter) obj).getDevice());
	}
}
