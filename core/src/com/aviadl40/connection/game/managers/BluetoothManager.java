package com.aviadl40.connection.game.managers;

import android.support.annotation.Nullable;

import com.badlogic.gdx.utils.Array;

import java.io.Closeable;
import java.util.UUID;

public interface BluetoothManager<BTP extends BluetoothManager.BluetoothPairedDeviceInterface, BTC extends Closeable> {
	enum BluetoothState {
		ON,
		OFF,
		TURNING_ON,
		TURNING_OFF,
		;
	}

	interface BluetoothPairedDeviceInterface {
		void connect(UUID verificationUUID, BluetoothListener listener);

		String getName();
	}

	interface BluetoothConnectedDeviceInterface extends Closeable {
		void disconnect();

		String getName();
	}

	interface BluetoothListener {
		void onStateChanged(BluetoothState state);

		void onDiscoverableStateChanged(boolean discoverable);

		void onDiscoveryStateChanged(boolean enabled);

		void onDiscoverDevice(BluetoothPairedDeviceInterface device);

		void onConnectedToDevice(BluetoothConnectedDeviceInterface device);

		void onDisconnectedFromDevice(BluetoothConnectedDeviceInterface device);

		void onDeviceConnected(BluetoothConnectedDeviceInterface device);

		void onDeviceDisconnected(BluetoothConnectedDeviceInterface disconnected);

		void onRead(BluetoothConnectedDeviceInterface from, byte[] bytes);
	}

	boolean bluetoothSupported();

	void requestEnable(boolean enable);

	BluetoothState getState();

	Closeable host(String name, UUID uuid);

	void writeTo(BTC device, byte[] bytes);

	void requestMakeDiscoverable(int duration);

	void enableDiscovery(boolean enabled);

	Array<BTP> getPairedDevices();

	Array<BTC> getConnectedDevices();

	@Nullable
	BluetoothListener getBluetoothListener();

	void setBluetoothListener(@Nullable BluetoothListener listener);
}
