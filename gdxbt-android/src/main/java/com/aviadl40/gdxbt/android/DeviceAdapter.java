package com.aviadl40.gdxbt.android;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.Nullable;

abstract class DeviceAdapter {
	@Nullable
	abstract BluetoothDevice getDevice();

	/**
	 * @return true if obj is a device adapter & both the devices attached to the adapters are equal
	 * @see DeviceAdapter#equals(Object)
	 */
	boolean deviceEquals(@Nullable DeviceAdapter deviceAdapter) {
		return deviceAdapter != null && getDevice() != null && getDevice().equals(deviceAdapter.getDevice());
	}
}
