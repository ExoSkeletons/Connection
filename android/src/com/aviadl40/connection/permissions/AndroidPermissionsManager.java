package com.aviadl40.connection.permissions;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;

import com.aviadl40.connection.AndroidLauncher;
import com.aviadl40.connection.game.managers.PermissionsManager;

public final class AndroidPermissionsManager implements PermissionsManager {
	private final AndroidLauncher mAndroid;

	public AndroidPermissionsManager(AndroidLauncher mAndroid) {
		this.mAndroid = mAndroid;
	}

	private String getPermName(Permission perm) {
		switch (perm) {
			case BLUETOOTH:
				return Manifest.permission.BLUETOOTH;
			case BLUETOOTH_ADMIN:
				return Manifest.permission.BLUETOOTH_ADMIN;
			case LOCATION_COARSE:
				return Manifest.permission.ACCESS_COARSE_LOCATION;
			case LOCATION_FINE:
				return Manifest.permission.ACCESS_FINE_LOCATION;
			case INTERNET:
				return Manifest.permission.INTERNET;
			default:
				return null;
		}
	}

	@Override
	public boolean hasPermissions(@NonNull Permission... permissions) {
		if (permissions.length == 0)
			return true;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
			return true;
		String name;
		for (Permission perm : permissions) {
			name = getPermName(perm);
			if (name != null && mAndroid.checkSelfPermission(name) != PackageManager.PERMISSION_GRANTED)
				return false;
		}
		return true;
	}

	@Override
	public void requestPermissions(@NonNull Permission... permissions) {
		if (permissions.length == 0)
			return;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
			return;
		final String[] names = new String[permissions.length];
		for (int i = 0; i < permissions.length; i++)
			names[i] = getPermName(permissions[i]);
		mAndroid.requestPermissions(names, 42);
	}
}
