package com.aviadl40.connection.game.screens;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.aviadl40.connection.Connection;
import com.aviadl40.connection.Gui;
import com.aviadl40.connection.Settings;
import com.aviadl40.connection.game.GameParameters;
import com.aviadl40.connection.game.managers.ScreenManager;
import com.aviadl40.connection.game.screens.HostGameScreen.BTPlayer;
import com.aviadl40.gdxbt.core.BluetoothManager;
import com.aviadl40.gdxbt.core.BluetoothManager.BluetoothConnectedDeviceInterface;
import com.aviadl40.gdxbt.core.BluetoothManager.BluetoothPairedDeviceInterface;
import com.aviadl40.gdxbt.core.BluetoothManager.BluetoothState;
import com.aviadl40.gdxperms.core.PermissionsManager.PermissionRequestListener;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import java.io.IOException;

public final class ClientBluetoothGameScreen extends ClientGameScreen<BluetoothConnectedDeviceInterface> implements BluetoothManager.BluetoothListener {
	public static final class ClientBluetoothSetupScreen extends ClientSetupScreen<ClientBluetoothGameScreen> implements BluetoothManager.BluetoothListener {
		private static final String
				TITLE = "Join Game",
				DESC_START_DISCOVERY = "Search",
				DESC_DISCOVERING = "Searching for Devices...",
				DESC_RESTART_DISCOVERY = "Search again",
				DESC_FOUND = "Found",
				DESC_CONNECTING = "Connecting to",
				DESC_CONNECTED = "Connected",

		ACTION_CONNECT = "Connect",

		ERR_CONNECTION_FAILED = "Could not connect.";

		final TextButton startDiscovery = new TextButton("", Gui.skin());
		final List<BluetoothPairedDeviceInterface> foundDevicesList = new List<BluetoothPairedDeviceInterface>(Gui.skin()) {
			@Override
			public String toString(BluetoothPairedDeviceInterface device) {
				String name = device.getName();
				return name == null ? "" : name;
			}
		};
		final TextButton connectButton = new TextButton("", Gui.skin());
		ScrollPane foundDevicesScroller = new ScrollPane(foundDevicesList);
		final Table foundDevicesTable = new Table() {
			@Override
			public void act(float delta) {
				foundDevicesList.setHeight(foundDevicesList.getPrefHeight());
				foundDevicesScroller.setHeight(foundDevicesList.getHeight());
				getCell(foundDevicesScroller).height(foundDevicesScroller.getHeight()).maxHeight(Gui.buttonSize() * 3);
				foundDevicesList.invalidateHierarchy();
			}
		};

		@Nullable
		private BluetoothConnectedDeviceInterface hostInterface;

		ClientBluetoothSetupScreen(ScreenManager.UIScreen prev) {
			super(prev);
		}

		@Override
		protected ClientBluetoothGameScreen newGame(@NonNull GameParameters params) {
			if (hostInterface == null)
				throw new IllegalArgumentException("Missing game host.");
			return new ClientBluetoothGameScreen(this, hostInterface, params);
		}

		@Override
		protected void buildUI() {
			super.buildUI();

			title.setText(TITLE);
			title.pack();

			Label.LabelStyle labelStyle = Gui.instance().labelStyles.subTextStyle;
			startDiscovery.getLabel().setStyle(new Label.LabelStyle(labelStyle));
			startDiscovery.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					if (!startDiscovery.isDisabled()) {
						leaveLobby();
						startDiscovery();
					}
				}
			});
			connectButton.getLabel().setStyle(new Label.LabelStyle(labelStyle));
			connectButton.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					if (foundDevicesList.getSelected() == null) return;

					// Attempt to connect to host
					BluetoothPairedDeviceInterface futureHost = foundDevicesList.getSelected();
					connectButton.setText(DESC_CONNECTING + " " + futureHost.getName() + "...");
					disableDeviceSearch(true);

					futureHost.connect(Connection.BT_UUID, Connection.btManager);
				}
			});
			connectButton.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					connectButton.setVisible(hostInterface == null && foundDevicesList.getSelected() != null);
				}
			});
			connectButton.fire(new ChangeListener.ChangeEvent());

			foundDevicesList.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					connectButton.setText(ACTION_CONNECT);
					connectButton.fire(new ChangeEvent());
				}
			});

			foundDevicesTable.add(startDiscovery).spaceBottom(Gui.sparsity()).growX().row();
			foundDevicesTable.add(foundDevicesScroller).growX().growY().maxHeight(Gui.buttonSize() * 2).row();
			foundDevicesTable.add(connectButton).spaceTop(Gui.sparsity()).row();

			tools.add(foundDevicesTable).fillX().row();

			leaveLobby();
			Connection.btManager.requestEnable(new PermissionRequestListener() {
				@Override
				public void OnDenied() {
					back();
				}
			});
		}

		@Override
		public void show() {
			super.show();
			Connection.btManager.setBluetoothListener(this);
		}

		@Override
		public void dispose() {
			Connection.btManager.cancelDiscovery();
			Connection.btManager.disable();
			super.dispose();
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
			if (!Connection.btManager.isEnabled())
				// TODO: add dedicated bluetooth state button/indicator,
				//  and hide search button when bt is off.
				Connection.btManager.requestEnable(null);
			else
				Connection.btManager.cancelDiscovery();
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
			startDiscovery.setText(DESC_START_DISCOVERY);
			disableDeviceSearch(false);
		}

		private void disableDeviceSearch(boolean disable) {
			if (disable)
				Connection.btManager.cancelDiscovery();

			foundDevicesTable.setTouchable(disable ? Touchable.disabled : Touchable.enabled);
			foundDevicesTable.getColor().a = disable ? .5f : 1f;
		}

		@Override
		public void onStateChanged(BluetoothState state) {
			if (state == BluetoothState.ON)
				startDiscovery();
		}

		@Override
		public void onDiscoverableStateChanged(boolean discoverable) {
		}

		@Override
		public void onDiscoveryStateChanged(boolean discoveryEnabled) {
			if (discoveryEnabled) {
				foundDevicesList.clearItems();
				foundDevicesList.fire(new ChangeListener.ChangeEvent());
			}

			startDiscovery.setDisabled(discoveryEnabled);
			startDiscovery.setText(
					discoveryEnabled
							? DESC_DISCOVERING
							: hostInterface == null ? DESC_RESTART_DISCOVERY : DESC_START_DISCOVERY
			);
			connectButton.fire(new ChangeListener.ChangeEvent());
		}

		@Override
		public void onDiscoverDevice(BluetoothPairedDeviceInterface deviceDiscovered) {
			if (deviceDiscovered.getName() == null) return;

			foundDevicesList.getItems().add(deviceDiscovered);
			foundDevicesList.invalidateHierarchy();
		}

		@Override
		public void onConnectedToDevice(BluetoothConnectedDeviceInterface deviceConnectedTo) {
			if (hostInterface == null) {
				hostInterface = deviceConnectedTo;

				connectButton.setText(DESC_CONNECTED + " to\n" + hostInterface.getName());
				connectButton.fire(new ChangeListener.ChangeEvent());
			}
		}

		@Override
		public void onConnectionFailed(BluetoothPairedDeviceInterface deviceConnectionFailed, IOException e) {
			connectButton.setText(ERR_CONNECTION_FAILED + (Settings.moreInfo ? "\n" + e.getLocalizedMessage() : ""));
			disableDeviceSearch(false);
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
	public void onConnectionFailed(BluetoothPairedDeviceInterface deviceConnectionFailed, IOException e) {
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
