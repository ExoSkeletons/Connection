package com.aviadl40.connection.game;

import android.support.annotation.NonNull;

import com.aviadl40.connection.game.screens.HostGameScreen;
import com.aviadl40.gdxbt.core.BluetoothManager;

public final class BTPlayer extends Player.Human {
	public final BluetoothManager.BluetoothConnectedDeviceInterface deviceInterface;

	public BTPlayer(@NonNull String name, BluetoothManager.BluetoothConnectedDeviceInterface deviceInterface) {
		super(name);
		this.deviceInterface = deviceInterface;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof BTPlayer && (((BTPlayer) o).deviceInterface.equals(deviceInterface));
	}
}
