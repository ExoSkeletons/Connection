package com.aviadl40.gdxbt.android;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.aviadl40.gdxbt.core.BluetoothManager;

import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;

final class PairedDeviceAdapter extends DeviceAdapter implements BluetoothManager.BluetoothPairedDeviceInterface {
	// Client connection task
	static final class ConnectToHostTask extends SocketTask<BluetoothSocket, Void, BluetoothSocket> {
		private final PairedDeviceAdapter pairedDevice;
		private final BluetoothManager<?, BluetoothManager.BluetoothConnectedDeviceInterface> btManager;

		ConnectToHostTask(PairedDeviceAdapter pairedDevice, @NonNull BluetoothSocket connectionSocket, BluetoothManager<?, BluetoothManager.BluetoothConnectedDeviceInterface> btManager) {
			super(connectionSocket);
			this.pairedDevice = pairedDevice;
			this.btManager = btManager;
		}

		@Override
		protected BluetoothSocket doInBackground(BluetoothSocket socket) {
			try {
				// Try and connect to host
				// NOTE: Cancelling the task closes the socket and closing the socket aborts
				// the blocking done by connect() so we do not need to worry.
				socket.connect();
			} catch (IOException e) {
				e.printStackTrace();
				cancel(true);
				BluetoothManager.BluetoothListener btListener = btManager.getBluetoothListener();
				if (btListener != null) btListener.onConnectionFailed(pairedDevice, e);
				return null;
			}
			return socket;
		}

		@Override
		protected void onPostExecute(BluetoothSocket socket) {
			ConnectedDeviceAdapter connected = new ConnectedDeviceAdapter(socket);
			btManager.getConnectedDevices().add(connected);
			BluetoothManager.BluetoothListener btListener = btManager.getBluetoothListener();
			if (btListener != null) btListener.onConnectedToDevice(connected);
		}
	}

	@NonNull
	private final BluetoothDevice device;
	@Nullable
	private ConnectToHostTask connectTask = null;

	PairedDeviceAdapter(@NonNull BluetoothDevice device) {
		this.device = device;
	}

	@Override
	public Closeable connect(UUID verificationUUID, BluetoothManager<?, BluetoothManager.BluetoothConnectedDeviceInterface> btManager) {
		if (connectTask != null) connectTask.cancel(true);
		try {
			BluetoothSocket connectionSocket = device.createRfcommSocketToServiceRecord(verificationUUID);
			connectTask = new ConnectToHostTask(this, connectionSocket, btManager);
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
