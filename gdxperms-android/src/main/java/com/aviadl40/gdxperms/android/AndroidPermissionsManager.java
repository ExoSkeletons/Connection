package com.aviadl40.gdxperms.android;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.aviadl40.gdxperms.core.PermissionsManager;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.utils.Array;

import java.util.HashMap;

public final class AndroidPermissionsManager implements PermissionsManager {
	private static final int REQ_PERMS = 7656;

	private final AndroidApplication mAndroid;
	private final HashMap<Permission, Array<String>> permRequestStringsMap = new HashMap<>();
	private PermissionRequestListener mPermRequestListener = null;

	public AndroidPermissionsManager(AndroidApplication mAndroid) {
		this.mAndroid = mAndroid;

		// Setup perm strings map for perm requesting
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			Array<String> baseBTPermStrings = Array.with(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				baseBTPermStrings.addAll(Manifest.permission.BLUETOOTH_CONNECT);

				permRequestStringsMap.put(Permission.BLUETOOTH_ADVERTISE, Array.with(
						Manifest.permission.BLUETOOTH_SCAN,
						Manifest.permission.BLUETOOTH_ADVERTISE
				));
				permRequestStringsMap.put(Permission.BLUETOOTH_SCAN, Array.with(
						Manifest.permission.BLUETOOTH_SCAN,
						Manifest.permission.BLUETOOTH_CONNECT
				));
				permRequestStringsMap.put(Permission.BLUETOOTH_CONNECT, Array.with(Manifest.permission.BLUETOOTH_CONNECT));
			} else {
				baseBTPermStrings.addAll(
						Manifest.permission.ACCESS_FINE_LOCATION,
						Manifest.permission.ACCESS_COARSE_LOCATION
				);

				permRequestStringsMap.put(Permission.BLUETOOTH_ADVERTISE, baseBTPermStrings);
				permRequestStringsMap.put(Permission.BLUETOOTH_SCAN, baseBTPermStrings);
				permRequestStringsMap.put(Permission.BLUETOOTH_CONNECT, baseBTPermStrings);
			}
			permRequestStringsMap.put(Permission.BLUETOOTH, baseBTPermStrings);

			permRequestStringsMap.put(Permission.LOCATION_FINE, Array.with(Manifest.permission.ACCESS_FINE_LOCATION));
			permRequestStringsMap.put(Permission.LOCATION_COARSE, Array.with(Manifest.permission.ACCESS_COARSE_LOCATION));
		}

	}

	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == REQ_PERMS && mPermRequestListener != null) {
			boolean grantedAll = true;
			for (int i = 0; i < permissions.length && grantedAll; i++)
				grantedAll = grantResults[i] == PackageManager.PERMISSION_GRANTED;
			if (grantedAll) mPermRequestListener.OnGranted();
			else mPermRequestListener.OnDenied();
		}
	}

	@Override
	public boolean hasPermissions(@NonNull Permission... permissions) {
		if (permissions.length == 0)
			return true;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
			return true;
		Array<String> permStrings;
		for (Permission perm : permissions) {
			permStrings = permRequestStringsMap.get(perm);
			if (permStrings != null)
				for (String permString : permStrings)
					if (permString != null)
						if (mAndroid.checkSelfPermission(permString) != PackageManager.PERMISSION_GRANTED)
							return false;
		}
		return true;
	}

	@Override
	public void requestPermissions(@NonNull Permission permission, @Nullable PermissionRequestListener requestListener) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;

		mPermRequestListener = requestListener;

		Array<String> permStrings = permRequestStringsMap.get(permission);
		if (permStrings != null)
			mAndroid.requestPermissions(permStrings.toArray(String.class), REQ_PERMS);
	}
}