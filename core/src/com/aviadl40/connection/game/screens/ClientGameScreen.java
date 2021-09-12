package com.aviadl40.connection.game.screens;

import android.support.annotation.NonNull;

import com.aviadl40.connection.game.GameParameters;
import com.aviadl40.connection.game.managers.ScreenManager;

abstract class ClientGameScreen<H> extends GameScreen {
	abstract static class ClientSetupScreen<CG extends ClientGameScreen> extends SetupScreen<CG> {
		ClientSetupScreen(ScreenManager.UIScreen prev) {
			super(prev);
		}
	}

	@NonNull
	final H hostInterface;

	ClientGameScreen(ScreenManager.UIScreen prev, @NonNull H hostInterface, @NonNull GameParameters params) {
		super(prev, params);
		this.hostInterface = hostInterface;
	}
}
