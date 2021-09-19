package com.aviadl40.connection;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.aviadl40.connection.game.managers.BluetoothManager;

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
		public void disconnect() {
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

		@Override
		public void close() throws IOException {
			socket.close();
		}

		@NonNull
		@Override
		BluetoothDevice getDevice() {
			return socket.getRemoteDevice();
		}
	}

	static final class BTPairedDeviceAdapter extends BTDeviceAdapter implements BluetoothManager.BluetoothPairedDeviceInterface {
		// Client connection task
		static final class BTConnectToHostTask extends AsyncTask<Object, Void, Void> {
			@NonNull
			private final BluetoothSocket connectionSocket;
			private final BluetoothManager.BluetoothListener btListener;

			BTConnectToHostTask(@NonNull BluetoothSocket connectionSocket, BluetoothManager.BluetoothListener btListener) {
				this.connectionSocket = connectionSocket;
				this.btListener = btListener;
			}

			@Override
			protected Void doInBackground(Object... params) {
				try {
					connectionSocket.connect();
				} catch (IOException e) {
					e.printStackTrace();
					cancel(true);
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void aVoid) {
				BTConnectedDeviceAdapter connected = new BTConnectedDeviceAdapter(connectionSocket);
				btListener.onDeviceConnected(connected);
				btListener.onConnectedToDevice(connected);
			}

			@Override
			protected void onCancelled() {
				try {
					connectionSocket.close();
				} catch (IOException ignored) {
				}
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
		public void connect(UUID verificationUUID, BluetoothManager.BluetoothListener listener) {
			if (connectTask != null) connectTask.cancel(true);
			try (BluetoothSocket connectionSocket = device.createRfcommSocketToServiceRecord(verificationUUID)) {
				connectTask = new BTConnectToHostTask(connectionSocket, listener);
				connectTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} catch (IOException e) {
				e.printStackTrace();
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
}
