package com.aviadl40.gdxbt.android;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.aviadl40.gdxbt.core.BluetoothManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class ConnectedDeviceAdapter extends DeviceAdapter implements BluetoothManager.BluetoothConnectedDeviceInterface {
	@Nullable
	private BluetoothSocket socket;

	ConnectedDeviceAdapter(@Nullable BluetoothSocket socket) {
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
		if (socket == null) throw new IOException("socket closed.");
		if (!socket.isConnected()) throw new IOException("socket disconnected.");
		return socket.getInputStream();
	}

	@NonNull
	OutputStream getOutputStream() throws IOException {
		if (socket == null) throw new IOException("socket closed.");
		if (!socket.isConnected()) throw new IOException("socket disconnected.");
		return socket.getOutputStream();
	}

	@Override
	public String getName() {
		BluetoothDevice device = getDevice();
		return device == null ? "" : device.getName();
	}

	@Nullable
	@Override
	BluetoothDevice getDevice() {
		return socket == null ? null : socket.getRemoteDevice();
	}
}
