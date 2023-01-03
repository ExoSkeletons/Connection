package com.aviadl40.gdxperms.core;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;

@SuppressWarnings({"WeakerAccess", "unused"})
public interface PermissionsManager {
	enum Permission {
		BLUETOOTH,
		BLUETOOTH_SCAN,
		BLUETOOTH_ADVERTISE,
		BLUETOOTH_CONNECT,

		LOCATION_COARSE,
		LOCATION_FINE,

		INTERNET,

		;
	}

	class PermissionRequestListener {
		public void OnDenied() {
		}

		public void OnGranted() {
		}
	}

	class MissingPermissionsException extends IOException {
		public final PermissionsManager permManager;
		public final Permission[] missing;

		public MissingPermissionsException(PermissionsManager permManager, Permission... missing) {
			this.permManager = permManager;
			this.missing = missing;
		}
	}

	boolean hasPermissions(@NonNull Permission... permissions);

	void requestPermissions(@NonNull Permission permission, @Nullable PermissionRequestListener requestListener);
}