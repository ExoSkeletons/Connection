package com.aviadl40.connection.game.managers;

import android.support.annotation.NonNull;

import java.io.IOException;

public interface PermissionsManager {
	enum Permission {
		BLUETOOTH,
		BLUETOOTH_ADMIN,
		LOCATION_COARSE,
		LOCATION_FINE,
		INTERNET,
		;
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

	void requestPermissions(@NonNull Permission... permissions);
}