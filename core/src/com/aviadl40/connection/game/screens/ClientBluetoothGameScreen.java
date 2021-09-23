package com.aviadl40.connection.game.screens;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.aviadl40.connection.Connection;
import com.aviadl40.connection.Gui;
import com.aviadl40.connection.game.GameParameters;
import com.aviadl40.connection.game.managers.BluetoothManager;
import com.aviadl40.connection.game.managers.BluetoothManager.BluetoothConnectedDeviceInterface;
import com.aviadl40.connection.game.managers.BluetoothManager.BluetoothPairedDeviceInterface;
import com.aviadl40.connection.game.managers.BluetoothManager.BluetoothState;
import com.aviadl40.connection.game.managers.ScreenManager;
import com.aviadl40.connection.game.screens.HostGameScreen.BTPlayer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class ClientBluetoothGameScreen extends ClientGameScreen<BluetoothConnectedDeviceInterface> implements BluetoothManager.BluetoothListener {
	public static final class ClientBluetoothSetupScreen extends ClientSetupScreen<ClientBluetoothGameScreen> implements BluetoothManager.BluetoothListener {
		private static final String
				DESC_START_DISCOVERY = "Search",
				DESC_DISCOVERING = "Looking for Host...",
				DESC_RESTART_DISCOVERY = "Search again",
				DESC_FOUND = "Found",
				DESC_CONNECTED = "Connected";

		final TextButton startDiscovery = new TextButton("", Gui.skin());

		@Nullable
		private BluetoothConnectedDeviceInterface hostDevice;

		public ClientBluetoothSetupScreen(ScreenManager.UIScreen prev) {
			super(prev);
		}

		@Override
		protected ClientBluetoothGameScreen newGame(@NonNull GameParameters params) {
			if (hostDevice == null)
				throw new IllegalArgumentException("Missing game host.");
			return new ClientBluetoothGameScreen(prev, hostDevice, params);
		}

		@Override
		protected void buildUI() {
			super.buildUI();

			title.setText("Join game");
			title.pack();

			startDiscovery.setText(DESC_START_DISCOVERY);
			startDiscovery.getLabel().setStyle(new Label.LabelStyle(Gui.instance().labelStyles.subTextStyle));
			startDiscovery.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					if (!startDiscovery.isDisabled())
						if (Connection.btManager.getState() != BluetoothState.ON)
							Connection.btManager.requestEnable(true);
						else
							Connection.btManager.enableDiscovery(true);
				}
			});

			tools.add(startDiscovery).growX().row();
		}

		@Override
		public void show() {
			super.show();
			if (Connection.btManager.getState() != BluetoothState.ON)
				Connection.btManager.requestEnable(true);
			Connection.btManager.setBluetoothListener(this);
		}

		@Override
		public boolean back() {
			Connection.btManager.enableDiscovery(false);
			Connection.btManager.requestEnable(false);
			return super.back();
		}

		@Override
		void addPlayer(Player p) {
			super.addPlayer(p);
			// inform host player joined
			byte[] nameBytes = p.name.getBytes(), bytes = new byte[nameBytes.length + 1];
			bytes[0] = HostGameScreen.CODE_PLAYER_JOINED;
			System.arraycopy(nameBytes, 0, bytes, 1, nameBytes.length);
			Connection.btManager.writeTo(hostDevice, bytes);
		}

		@Override
		void removePlayer(int pi) {
			Player p = params.players.get(pi);
			super.removePlayer(pi);
			// inform host player left
			byte[] nameBytes = p.name.getBytes(), bytes = new byte[nameBytes.length + 1];
			bytes[0] = HostGameScreen.CODE_PLAYER_LEFT;
			System.arraycopy(nameBytes, 0, bytes, 1, nameBytes.length);
			Connection.btManager.writeTo(hostDevice, bytes);
		}

		@Override
		public void onStateChanged(BluetoothState state) {
			// if (state != BluetoothState.TURNING_ON)
			//	Connection.btManager.enableDiscovery(state == BluetoothState.ON);
		}

		@Override
		public void onDiscoverableStateChanged(boolean discoverable) {
		}

		@Override
		public void onDiscoveryStateChanged(boolean enabled) {
			startDiscovery.setDisabled(enabled);
			startDiscovery.setText(
					enabled
							? DESC_DISCOVERING
							: hostDevice == null ? DESC_RESTART_DISCOVERY : DESC_START_DISCOVERY
			);
		}

		@Override
		public void onDiscoverDevice(BluetoothPairedDeviceInterface deviceDiscovered) {
			title.setText(DESC_FOUND + "\n" + deviceDiscovered.getName());
			Connection.btManager.enableDiscovery(false);
			deviceDiscovered.connect(Connection.BT_UUID, Connection.btManager); // Attempt to connect to host
		}

		@Override
		public void onConnectedToDevice(BluetoothConnectedDeviceInterface deviceConnectedTo) {
			if (hostDevice == null) {
				hostDevice = deviceConnectedTo;
				title.setText(DESC_CONNECTED + " to\n" + hostDevice.getName());
			}
		}

		@Override
		public void onDisconnectedFromDevice(BluetoothConnectedDeviceInterface deviceDisconnectedFrom) {
			if (deviceDisconnectedFrom == hostDevice) {
				hostDevice = null;
				// TODO: show msg
				params.players.clear();
				updatePlayerTable();
			}
		}

		@Override
		public void onDeviceConnected(BluetoothConnectedDeviceInterface deviceConnected) {
		}

		@Override
		public void onDeviceDisconnected(BluetoothConnectedDeviceInterface deviceDisconnected) {
		}

		@Override
		public void onRead(BluetoothConnectedDeviceInterface from, byte[] bytes) {
			byte opCode = bytes[0];

			if (opCode == HostGameScreen.CODE_LOBBY_WELCOME) {
				LocalPlayer p = new LocalPlayer();
				p.name = Connection.btManager.getName();
				addPlayer(p);
			} else if (opCode == HostGameScreen.CODE_PLAYER_JOINED) {
				byte[] stringBytes = new byte[bytes.length - 1];
				System.arraycopy(bytes, 1, stringBytes, 0, stringBytes.length);
				super.addPlayer(new BTPlayer(new String(stringBytes), null));
			} else if (opCode == HostGameScreen.CODE_PLAYER_LEFT)
				super.removePlayer(bytes[1]);
			else if (opCode == HostGameScreen.CODE_PLAYER_CHANGED_NAME) {
				Player p;
				for (int pi = 0; pi < params.players.size; pi++)
					if ((p = params.players.get(pi)) instanceof BTPlayer && ((BTPlayer) p).deviceInterface == from) {
						byte[] stringBytes = new byte[bytes.length - 1];
						System.arraycopy(bytes, 1, stringBytes, 0, stringBytes.length);
						p.name = new String(stringBytes);
						updatePlayerTable();
						break;
					}
			} else if (opCode == HostGameScreen.CODE_GAME_STARTED)
				startGame();
		}
	}

	private ClientBluetoothGameScreen(ScreenManager.UIScreen prev, @NonNull BluetoothConnectedDeviceInterface hostInterface, @NonNull GameParameters params) {
		super(prev, hostInterface, params);
	}

	@Override
	protected void makeMove(Move move) {
		// Send move request to host
		Connection.btManager.writeTo(hostInterface, new byte[]{HostGameScreen.CODE_MADE_MOVE, getPI(), move.x, move.y, move.i});
	}

	@Override
	public void onStateChanged(BluetoothState state) {
		if (state == BluetoothState.TURNING_OFF || state == BluetoothState.OFF)
			; // TODO: show msg, stop game
	}

	@Override
	public void onDiscoverableStateChanged(boolean discoverable) {

	}

	@Override
	public void onDiscoverDevice(BluetoothPairedDeviceInterface deviceDiscovered) {
	}

	@Override
	public void onConnectedToDevice(BluetoothConnectedDeviceInterface deviceConnectedTo) {
	}

	@Override
	public void onDisconnectedFromDevice(BluetoothConnectedDeviceInterface deviceDisconnectedFrom) {
		// TODO: show msg, stop game
	}

	@Override
	public void onDeviceConnected(BluetoothConnectedDeviceInterface deviceConnected) {
	}

	@Override
	public void onDeviceDisconnected(BluetoothConnectedDeviceInterface deviceDisconnected) {
	}

	@Override
	public void onRead(BluetoothConnectedDeviceInterface from, byte[] bytes) {
		byte opCode = bytes[0];

		if (opCode == HostGameScreen.CODE_GAME_CLOSED)
			ScreenManager.setScreen(prev); //TODO: show msg
		else if (opCode == HostGameScreen.CODE_GAME_RESTARTED) restart();
		else if (opCode == HostGameScreen.CODE_PLAYER_LEFT) super.removePlayer(bytes[1]);
		else if (opCode == HostGameScreen.CODE_MADE_MOVE)
			super.makeMove(new Move(bytes[1], bytes[2], bytes[3]));
	}

	@Override
	public void onDiscoveryStateChanged(boolean enabled) {
	}

	@Override
	public void show() {
		super.show();
		Connection.btManager.setBluetoothListener(this);
	}

	@Override
	protected void onQuit() {
		hostInterface.closeConnection();
		ScreenManager.setScreen(new ClientBluetoothSetupScreen(prev.prev));
	}
}
