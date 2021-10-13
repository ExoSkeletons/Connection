package com.aviadl40.connection;

import android.content.Intent;
import android.os.Bundle;

import com.aviadl40.connection.bluetooth.AndroidBluetoothManager;
import com.aviadl40.connection.permissions.AndroidPermissionsManager;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

public class AndroidLauncher extends AndroidApplication {
	private AndroidBluetoothManager btManager = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		config.useAccelerometer = false;
		config.useCompass = false;
		config.useGyroscope = false;

		AndroidPermissionsManager permManager = new AndroidPermissionsManager(this);
		btManager = new AndroidBluetoothManager(this, permManager).init();

		initialize(new Connection(permManager, btManager), config);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (btManager != null) btManager.onActivityResult(requestCode, resultCode, data);

		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onDestroy() {
		if (btManager != null) btManager.destroy();

		super.onDestroy();
	}
}
