package com.aviadl40.gdxbt.core;

import android.support.annotation.Nullable;

import com.aviadl40.gdxperms.core.PermissionsManager;
import com.badlogic.gdx.utils.Array;

import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;

public interface BluetoothManager<BTPairedDevice extends BluetoothManager.BluetoothPairedDeviceInterface, BTConnectedDevice extends BluetoothManager.BluetoothConnectedDeviceInterface> {
	enum BluetoothState {
		ON,
		OFF,
		TURNING_ON,
		TURNING_OFF,
		;
	}

	final class Packet<S, M> {
		public final S sender;
		public final M message;

		public Packet(S sender, M message) {
			this.sender = sender;
			this.message = message;
		}
	}

	interface BluetoothPairedDeviceInterface<BTManager extends BluetoothManager> {
		Closeable connect(UUID verificationUUID, BluetoothManager btManager);

		String getName();
	}

	interface BluetoothConnectedDeviceInterface {
		void closeConnection();

		String getName();
	}

	interface BluetoothListener {
		void onStateChanged(BluetoothState state);

		void onDiscoverableStateChanged(boolean discoverable);

		void onDiscoveryStateChanged(boolean enabled);

		void onDiscoverDevice(BluetoothPairedDeviceInterface deviceDiscovered);

		void onConnectedToDevice(BluetoothConnectedDeviceInterface deviceConnectedTo);

		void onConnectionFailed(BluetoothPairedDeviceInterface deviceConnectionFailed, IOException e);

		void onDisconnectedFromDevice(BluetoothConnectedDeviceInterface deviceDisconnectedFrom);

		void onDeviceConnected(BluetoothConnectedDeviceInterface deviceConnected);

		void onDeviceDisconnected(BluetoothConnectedDeviceInterface deviceDisconnected);

		void onRead(BluetoothConnectedDeviceInterface from, byte[] bytes);
	}

	boolean bluetoothSupported();

	PermissionsManager getPermManager();

	void requestEnable(PermissionsManager.PermissionRequestListener requestListener);

	void disable();

	boolean isEnabled();

	BluetoothState getState();

	Closeable host(String name, UUID uuid);

	void writeTo(BTConnectedDevice device, byte[] bytes);

	void requestMakeDiscoverable(int duration);

	boolean requestStartDiscovery();

	boolean cancelDiscovery();

	Array<BTPairedDevice> getPairedDevices();

	Array<BTConnectedDevice> getConnectedDevices();

	@Nullable
	BluetoothListener getBluetoothListener();

	void setBluetoothListener(@Nullable BluetoothListener listener);

	String getName();
}
