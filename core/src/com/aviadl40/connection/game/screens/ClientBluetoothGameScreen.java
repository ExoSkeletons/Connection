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
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class ClientBluetoothGameScreen extends ClientGameScreen<BluetoothConnectedDeviceInterface> implements BluetoothManager.BluetoothListener {
	public static final class ClientBluetoothSetupScreen extends ClientSetupScreen<ClientBluetoothGameScreen> implements BluetoothManager.BluetoothListener {
		private static final String
				TITLE = "Join Game",
				DESC_START_DISCOVERY = "Search",
				DESC_DISCOVERING = "Looking for Host...",
				DESC_RESTART_DISCOVERY = "Search again",
				DESC_FOUND = "Found",
				DESC_CONNECTED = "Connected";

		final TextButton startDiscovery = new TextButton("", Gui.skin());

		@Nullable
		private BluetoothConnectedDeviceInterface hostInterface;

		public ClientBluetoothSetupScreen(ScreenManager.UIScreen prev) {
			super(prev);
		}

		@Override
		protected ClientBluetoothGameScreen newGame(@NonNull GameParameters params) {
			if (hostInterface == null)
				throw new IllegalArgumentException("Missing game host.");
			return new ClientBluetoothGameScreen(prev, hostInterface, params);
		}

		@Override
		protected void buildUI() {
			super.buildUI();

			startDiscovery.getLabel().setStyle(new Label.LabelStyle(Gui.instance().labelStyles.subTextStyle));
			startDiscovery.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					if (!startDiscovery.isDisabled()) {
						leaveLobby();
						startDiscovery();
					}
				}
			});

			leaveLobby();

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
			if (hostInterface != null) {
				byte[] nameBytes = p.name.getBytes(), colorBytes = p.color.toString().getBytes(), bytes = new byte[nameBytes.length + colorBytes.length + 1];
				bytes[0] = HostGameScreen.CODE_PLAYER_JOINED;
				System.arraycopy(colorBytes, 0, bytes, 1, colorBytes.length);
				System.arraycopy(nameBytes, 0, bytes, 1 + colorBytes.length, nameBytes.length);
				Connection.btManager.writeTo(hostInterface, bytes);
			}
		}

		@Override
		protected boolean canRemovePlayer(Player p) {
			return p instanceof LocalPlayer;
		}

		@Override
		void removePlayer(int pi) {
			super.removePlayer(pi);
			// inform host player left
			if (hostInterface != null) {
				byte[] bytes = new byte[2];
				bytes[0] = HostGameScreen.CODE_PLAYER_LEFT;
				bytes[1] = (byte) pi;
				Connection.btManager.writeTo(hostInterface, bytes);
			}
		}

		@Override
		void changePlayerName(byte pi, String newName) {
			super.changePlayerName(pi, newName);
			// inform host player name changed
			if (hostInterface != null) {
				byte[] nameBytes = params.players.get(pi).name.getBytes(), bytes = new byte[nameBytes.length + 2];
				bytes[0] = HostGameScreen.CODE_PLAYER_CHANGED_NAME;
				bytes[1] = pi;
				System.arraycopy(nameBytes, 0, bytes, 2, nameBytes.length);
				Connection.btManager.writeTo(hostInterface, bytes);
			}
		}

		void startDiscovery() {
			Connection.btManager.enableDiscovery(true);
		}

		void leaveLobby() {
			for (int i = params.players.size - 1; i >= 0; i--)
				if (params.players.get(i) instanceof LocalPlayer)
					removePlayer(i); // Will inform host
			params.players.clear();
			updatePlayerList();
			params.size = 3;
			playerTools.setVisible(false);
			sizeTools.setVisible(false);
			title.setText(TITLE);
			title.pack();
			startDiscovery.setText(DESC_START_DISCOVERY);
		}

		@Override
		public void onStateChanged(BluetoothState state) {
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
							: hostInterface == null ? DESC_RESTART_DISCOVERY : DESC_START_DISCOVERY
			);
		}

		@Override
		public void onDiscoverDevice(BluetoothPairedDeviceInterface deviceDiscovered) {
			if (deviceDiscovered.getName() == null) return;
			title.setText(DESC_FOUND + "\n" + deviceDiscovered.getName());
			Connection.btManager.enableDiscovery(false);
			deviceDiscovered.connect(Connection.BT_UUID, Connection.btManager); // Attempt to connect to host
		}

		@Override
		public void onConnectedToDevice(BluetoothConnectedDeviceInterface deviceConnectedTo) {
			if (hostInterface == null) {
				hostInterface = deviceConnectedTo;
				title.setText(DESC_CONNECTED + " to\n" + hostInterface.getName());
			}
		}

		@Override
		public void onDisconnectedFromDevice(BluetoothConnectedDeviceInterface deviceDisconnectedFrom) {
			if (deviceDisconnectedFrom == hostInterface) leaveLobby();
		}

		@Override
		public void onDeviceConnected(BluetoothConnectedDeviceInterface deviceConnected) {
		}

		@Override
		public void onDeviceDisconnected(BluetoothConnectedDeviceInterface deviceDisconnected) {
		}

		@Override
		public void onRead(BluetoothConnectedDeviceInterface from, byte[] bytes) {
			if (from == hostInterface) {
				byte opCode = bytes[0];

				if (opCode == HostGameScreen.CODE_LOBBY_WELCOME) {
					addPlayer();
					playerTools.setVisible(true);
					sizeTools.setVisible(true);
				} else if (opCode == HostGameScreen.CODE_PLAYER_JOINED) {
					byte[] colorBytes = new byte[8], nameBytes = new byte[bytes.length - 1 - colorBytes.length];
					System.arraycopy(bytes, 1, colorBytes, 0, colorBytes.length);
					System.arraycopy(bytes, 1 + colorBytes.length, nameBytes, 0, nameBytes.length);
					Player p = new BTPlayer(new String(nameBytes), null);
					p.color = Color.valueOf(new String(colorBytes));
					super.addPlayer(p);
				} else if (opCode == HostGameScreen.CODE_PLAYER_LEFT)
					super.removePlayer(bytes[1]);
				else if (opCode == HostGameScreen.CODE_PLAYER_CHANGED_NAME) {
					byte[] nameBytes = new byte[bytes.length - 2];
					System.arraycopy(bytes, 2, nameBytes, 0, nameBytes.length);
					super.changePlayerName(bytes[1], new String(nameBytes));
				} else if (opCode == HostGameScreen.CODE_PLAYER_CHANGED_COLOR) {
					byte pi = bytes[1];
					if (pi >= 0 && pi < params.players.size) {
						Player p = params.players.get(pi);
						if (p instanceof BTPlayer && ((BTPlayer) p).deviceInterface == from) {
							byte[] colorBytes = new byte[8];
							System.arraycopy(bytes, 2, colorBytes, 0, colorBytes.length);
							super.changePlayerColor(pi, Color.valueOf(new String(colorBytes)));
						}
					}
				} else if (opCode == HostGameScreen.CODE_BOARD_CHANGED_SIZE)
					changeBoardSize(bytes[1]);
				else if (opCode == HostGameScreen.CODE_GAME_STARTED)
					startGame();
			}
		}
	}

	private ClientBluetoothGameScreen(ScreenManager.UIScreen prev, @NonNull BluetoothConnectedDeviceInterface hostInterface, @NonNull GameParameters params) {
		super(prev, hostInterface, params);
	}

	@Override
	void selectMove(Move move) {
		super.selectMove(move); // Selection doesn't need host confirmation
		Connection.btManager.writeTo(hostInterface, new byte[]{HostGameScreen.CODE_SELECTED_MOVE, getPI(), move.x, move.y, move.i});
	}

	@Override
	protected void makeMove(Move move) {
		// Send move request to host
		Connection.btManager.writeTo(hostInterface, new byte[]{HostGameScreen.CODE_MADE_MOVE, getPI(), move.x, move.y, move.i});
	}

	@Override
	public void onStateChanged(BluetoothState state) {
		if (state == BluetoothState.TURNING_OFF || state == BluetoothState.OFF) back();
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
		if (deviceDisconnectedFrom == hostInterface) back();
	}

	@Override
	public void onDeviceConnected(BluetoothConnectedDeviceInterface deviceConnected) {
	}

	@Override
	public void onDeviceDisconnected(BluetoothConnectedDeviceInterface deviceDisconnected) {
	}

	@Override
	public void onRead(BluetoothConnectedDeviceInterface from, byte[] bytes) {
		if (from == hostInterface) {
			byte opCode = bytes[0];

			if (opCode == HostGameScreen.CODE_GAME_CLOSED) ScreenManager.setScreen(prev);
			else if (opCode == HostGameScreen.CODE_GAME_RESTARTED) restart();
			else if (opCode == HostGameScreen.CODE_PLAYER_LEFT) super.removePlayer(bytes[1]);
			else if (opCode == HostGameScreen.CODE_SELECTED_MOVE)
				super.selectMove(new Move(bytes[1], bytes[2], bytes[3]));
			else if (opCode == HostGameScreen.CODE_MADE_MOVE)
				super.makeMove(new Move(bytes[1], bytes[2], bytes[3]));
		}
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
		((ClientBluetoothSetupScreen) prev).leaveLobby();
	}
}
