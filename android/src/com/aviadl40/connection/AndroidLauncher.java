package com.aviadl40.connection;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.aviadl40.gdxbt.android.AndroidBluetoothManager;
import com.aviadl40.gdxperms.android.AndroidPermissionsManager;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

public class AndroidLauncher extends AndroidApplication {
	private final AndroidPermissionsManager permManager = new AndroidPermissionsManager(this);
	private final AndroidBluetoothManager btManager = new AndroidBluetoothManager(this, permManager);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		config.useAccelerometer = false;
		config.useCompass = false;
		config.useGyroscope = false;

		btManager.init();

		initialize(new Connection(permManager, btManager), config);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		btManager.onActivityResult(requestCode, resultCode, data);

		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		permManager.onRequestPermissionsResult(requestCode, permissions, grantResults);

		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	@Override
	protected void onDestroy() {
		btManager.destroy();

		super.onDestroy();
	}
}
