package com.aviadl40.gdxbt.android;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.aviadl40.gdxbt.core.BluetoothManager;
import com.aviadl40.gdxperms.core.PermissionsManager;
import com.aviadl40.gdxperms.core.PermissionsManager.Permission;
import com.aviadl40.gdxperms.core.PermissionsManager.PermissionRequestListener;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketException;
import java.util.UUID;

final class PairedDeviceAdapter extends DeviceAdapter implements BluetoothManager.BluetoothPairedDeviceInterface<AndroidBluetoothManager> {
	// Client connection task
	static final class ConnectToHostTask extends SocketTask<BluetoothSocket, Void, BluetoothSocket> {
		private final PairedDeviceAdapter pairedDevice;
		private final BluetoothManager btManager;

		private IOException exception;

		ConnectToHostTask(PairedDeviceAdapter pairedDevice, @NonNull BluetoothSocket connectionSocket, BluetoothManager btManager) {
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
				exception = e;
				cancel(true);
				return null;
			}
			return socket;
		}

		@Override
		protected void onCancelled(BluetoothSocket socket) {
			if (exception != null) exception = new SocketException("Connection Cancelled");
			pairedDevice.onConnectionFailed(btManager, exception);
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
	public Closeable connect(final UUID verificationUUID, final BluetoothManager btManager) {
		if (connectTask != null) connectTask.cancel(true);
		PermissionsManager permManager = btManager.getPermManager();
		if (!permManager.hasPermissions(Permission.BLUETOOTH_CONNECT)) {
			permManager.requestPermissions(Permission.BLUETOOTH_CONNECT, new PermissionRequestListener() {
				@Override
				public void OnGranted() {
					// FIXME: need to find a way to return a ref to the inner return of this connect call
					//  (for closing)
					connect(verificationUUID, btManager);
				}
			});
			return null;
		}
		try {
			BluetoothSocket connectionSocket = device.createRfcommSocketToServiceRecord(verificationUUID);
			connectTask = new ConnectToHostTask(this, connectionSocket, btManager);
			connectTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			// NOTE: we do not close the socket, as the connect task just got it and needs it open.
			// The connect task therefore is now the one in charge of closing the socket after it's done.
			return connectTask;
		} catch (IOException e) {
			onConnectionFailed(btManager, e);
			return null;
		}
	}

	private void onConnectionFailed(BluetoothManager btManager, IOException e) {
		BluetoothManager.BluetoothListener btListener = btManager.getBluetoothListener();
		if (btListener != null) btListener.onConnectionFailed(this, e);
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
