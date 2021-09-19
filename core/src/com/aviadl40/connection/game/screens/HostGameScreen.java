package com.aviadl40.connection.game.screens;

import android.support.annotation.NonNull;

import com.aviadl40.connection.Connection;
import com.aviadl40.connection.Gui;
import com.aviadl40.connection.Settings;
import com.aviadl40.connection.Utils;
import com.aviadl40.connection.game.GameParameters;
import com.aviadl40.connection.game.managers.BluetoothManager;
import com.aviadl40.connection.game.managers.BluetoothManager.BluetoothConnectedDeviceInterface;
import com.aviadl40.connection.game.managers.BluetoothManager.BluetoothPairedDeviceInterface;
import com.aviadl40.connection.game.managers.BluetoothManager.BluetoothState;
import com.aviadl40.connection.game.managers.ScreenManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

public final class HostGameScreen extends GameScreen implements BluetoothManager.BluetoothListener {
	public static final class HostSetupScreen extends SetupScreen<HostGameScreen> implements BluetoothManager.BluetoothListener {
		private static final String
				WARN = "are you sure this is what you want?",
				TOO_MANY_PLAYERS = "too many players",
				VICTORY_IMPOSSIBLE = "victory is impossible";
		private final TextButton toggleBTHost = new TextButton("", Gui.skin());
		byte errorLevel = 0;

		public HostSetupScreen(ScreenManager.UIScreen prev) {
			super(prev);
		}

		@Override
		protected final HostGameScreen newGame(@NonNull GameParameters params) {
			return new HostGameScreen(this, params);
		}

		@Override
		protected void buildUI() {
			super.buildUI();

			// Title
			title.setText("Set up game");
			title.pack();
			title.setAlignment(Align.center);

			// Players
			final Table addPlayers = new Table(Gui.skin());
			final TextButton addHuman = new TextButton("Add Player", Gui.skin());
			addHuman.getLabel().setStyle(new Label.LabelStyle(Gui.instance().labelStyles.subTextStyle));
			ClickListener hcl = new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					Human h = new LocalPlayer();
					byte num = 1;
					for (Player p : params.players)
						if (p.getClass() == h.getClass())
							num++;
					h.name = "Player " + num;
					addPlayer(h);
				}
			};
			addHuman.addListener(hcl);
			for (byte n = 0; n < 1; n++)
				hcl.clicked(null, 0, 0);
			final TextButton addBot = new TextButton("Add Bot", Gui.skin());
			addBot.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					Bot b = new Bot();
					byte num = 1;
					for (Player p : params.players)
						if (p.getClass() == b.getClass())
							num++;
					b.name = "Bot " + num;
					addPlayer(b);
				}
			});
			addBot.getLabel().setStyle(addHuman.getLabel().getStyle());
			addPlayers.add(addHuman).fill().growX().minWidth(Gui.buttonSizeSmall()).padRight(Gui.sparsity() / 2);
			addPlayers.add(addBot).fill().growX().minWidth(Gui.buttonSizeSmall()).padLeft(Gui.sparsity() / 2);

			// Host
			Table hostTable = new Table();
			hostTable.add(new Label("Host game", Gui.skin())).row();
			if (Connection.btManager.bluetoothSupported()) {
				Table btHostTable = new Table();
				final Label hostLabel = new Label("Bluetooth", Gui.instance().labelStyles.subTextStyle);
				hostLabel.setAlignment(Align.center);
				toggleBTHost.getLabel().setStyle(new Label.LabelStyle(hostLabel.getStyle()));
				toggleBTHost.addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						if (!toggleBTHost.isDisabled())
							Connection.btManager.requestEnable(toggleBTHost.isChecked());
					}
				});

				btHostTable.add(hostLabel).growX().padRight(Gui.sparsity());
				btHostTable.add(toggleBTHost).growX().row();
				hostTable.add(btHostTable).growX().row();
			} else {
				Label ns = new Label("Bluetooth is not supported on this device.", Gui.instance().labelStyles.subTextStyle);
				ns.setWrap(true);
				ns.setAlignment(Align.center);
				ns.getColor().a = .75f;
				hostTable.add(ns).growX();
			}

			// Size
			final Table sizeTools = new Table(Gui.skin());
			final TextButton sizeUp = new TextButton("+", Gui.skin()), sizeDown = new TextButton("-", Gui.skin());
			final Label sizeLabel = new Label("", Gui.skin()) {
				@Override
				public void act(float delta) {
					super.act(delta);
					setText("Board size: " + params.size);
					pack();
					sizeUp.setVisible(params.size + 1 <= GameParameters.MAX_SIZE);
					sizeDown.setVisible(params.size - 1 >= (Settings.DEV_MODE ? 1 : 3));
				}
			};
			sizeUp.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					params.size++;
					if (Connection.btManager.getState() != BluetoothState.OFF) {
						byte[] bytes = {CODE_BOARD_CHANGED_SIZE, params.size};
						for (Player p : params.players)
							if (p instanceof BTPlayer)
								Connection.btManager.writeTo(((BTPlayer) p).deviceInterface, bytes);
					}
				}
			});
			sizeDown.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					params.size--;
					if (Connection.btManager.getState() != BluetoothState.OFF) {
						byte[] bytes = {CODE_BOARD_CHANGED_SIZE, params.size};
						for (Player p : params.players)
							if (p instanceof BTPlayer)
								Connection.btManager.writeTo(((BTPlayer) p).deviceInterface, bytes);
					}
				}
			});
			sizeTools.add(sizeLabel).padRight(Gui.sparsity());
			sizeTools.add(sizeDown).minWidth(Gui.buttonSizeSmall());
			sizeTools.add(sizeUp).minWidth(Gui.buttonSizeSmall());

			// Start
			final TextButton startGame = new TextButton("Start", Gui.skin());
			startGame.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					if (errorLevel < 3 || Settings.moreInfo)
						startGame();
				}
			});
			startGame.setSize(Gdx.graphics.getWidth() - Gui.sparsity() * 2, Gui.buttonSize());

			// Message
			Label messageLabel = new Label("", Gui.instance().labelStyles.subTextStyle) {
				@Override
				public void act(float delta) {
					super.act(delta);

					final String error;
					if (params.players.size < 1) {
						error = "Game must have at least 1 Player";
						errorLevel = 3;
					} else if (params.size == 1) {
						error = params.players.size > 1 ? "Ok really" : "A winner is you";
						errorLevel = 0;
					} else if (params.size == 2) {
						if (params.players.size <= 8) {
							error = "Only the first Player can win this game.";
							errorLevel = 2;
						} else {
							error = Utils.capitaliseFirst(TOO_MANY_PLAYERS) + ", " + Utils.capitaliseFirst(VICTORY_IMPOSSIBLE);
							errorLevel = 3;
						}
					} else if (Math.pow(params.size, 2) < params.players.size) {
						int num = (int) (Math.pow(params.size, 3) - params.players.size * (params.size - 1));
						error = Utils.capitaliseFirst(TOO_MANY_PLAYERS) + ", " +
								(num > 0
										? "Only the first " + (num == 1 ? "Player" : (num + " Players")) + " can win this game."
										: Utils.capitaliseFirst(VICTORY_IMPOSSIBLE)
								) + "."
						;
						errorLevel = (byte) (num > 0 ? 2 : 3);
					} else {
						error = "";
						errorLevel = 0;
					}

					setText(error + (errorLevel == 2 ? " " + Utils.capitaliseFirst(WARN) : ""));
					setColor(errorLevel == 3 ? Color.RED : errorLevel == 2 ? Color.ORANGE : Color.WHITE);
				}
			};
			messageLabel.setAlignment(Align.center);
			messageLabel.setWrap(true);
			messageLabel.setTouchable(Touchable.disabled);


			tools.add(addPlayers).fill().spaceBottom(Gui.sparsity() * 2).row();
			if (Settings.BT_READY || Settings.DEV_MODE)
				tools.add(hostTable).fill().growX().spaceBottom(Gui.sparsity() * 2).row();
			tools.add(sizeTools).spaceBottom(Gui.sparsity()).row();
			tools.add(messageLabel).grow().spaceBottom(Gui.sparsity() * 2).row();
			tools.add(startGame).fill();
		}

		@Override
		void addPlayer(Player player) {
			for (Player p : params.players)
				if (p instanceof BTPlayer) {
					byte[] nameBytes = player.name.getBytes(), bytes = new byte[nameBytes.length + 1];
					bytes[0] = CODE_PLAYER_JOINED;
					System.arraycopy(nameBytes, 0, bytes, 1, nameBytes.length);
					Connection.btManager.writeTo(((BTPlayer) p).deviceInterface, bytes);
				}
			super.addPlayer(player);
		}

		@Override
		protected boolean canRemovePlayer(Player p) {
			return true;
		}

		@Override
		void removePlayer(int pi) {
			byte[] bytes = {CODE_PLAYER_LEFT, (byte) pi};
			for (Player p : params.players)
				if (p instanceof BTPlayer)
					Connection.btManager.writeTo(((BTPlayer) p).deviceInterface, bytes);
			super.removePlayer(pi);
		}

		@Override
		public void show() {
			super.show();
			Connection.btManager.setBluetoothListener(this);
		}

		@Override
		public boolean back() {
			Connection.btManager.requestEnable(false);
			return super.back();
		}

		@Override
		public void onStateChanged(BluetoothState state) {
			if (state == BluetoothState.ON)
				Connection.btManager.requestMakeDiscoverable(300);

			CharSequence text = toggleBTHost.getText();
			switch (state) {
				case ON:
					text = "turn off";
					toggleBTHost.setChecked(true);
					break;
				case OFF:
					text = "turn on ";
					toggleBTHost.setChecked(false);
					break;
				case TURNING_ON:
				case TURNING_OFF:
					text = "  . . . . .   ";
					break;
			}
			toggleBTHost.getLabel().setColor(state == BluetoothState.ON ? Color.GOLD : Color.WHITE);
			toggleBTHost.setText(text.toString());
			toggleBTHost.setDisabled(state == BluetoothState.TURNING_ON || state == BluetoothState.TURNING_OFF);
		}

		@Override
		public void onDiscoverableStateChanged(boolean discoverable) {
			if (discoverable)
				Connection.btManager.host("Test?..", Connection.BT_UUID);
			else
				Connection.btManager.requestEnable(false);
		}

		@Override
		public void onDiscoverDevice(BluetoothPairedDeviceInterface deviceDiscovered) {
		}

		@Override
		public void onConnectedToDevice(BluetoothConnectedDeviceInterface deviceConnectedTo) {
		}

		@Override
		public void onDisconnectedFromDevice(BluetoothConnectedDeviceInterface deviceDisconnectedFrom) {
		}

		@Override
		public void onDeviceConnected(BluetoothConnectedDeviceInterface deviceConnected) {
		}

		@Override
		public void onDeviceDisconnected(BluetoothConnectedDeviceInterface deviceDisconnected) {
		}

		@Override
		public void onRead(BluetoothConnectedDeviceInterface from, byte[] bytes) {
		}

		@Override
		public void onDiscoveryStateChanged(boolean enabled) {
		}
	}

	static final class BTPlayer extends Human {
		final BluetoothConnectedDeviceInterface deviceInterface;

		BTPlayer(@NonNull String name, BluetoothConnectedDeviceInterface deviceInterface) {
			super(name);
			this.deviceInterface = deviceInterface;
		}
	}

	static final byte
			CODE_GAME_CLOSED = 10,
			CODE_GAME_STARTED = 11,
			CODE_GAME_RESTARTED = 12;
	static final byte
			CODE_PLAYER_JOINED = 20,
			CODE_PLAYER_LEFT = 21,
			CODE_PLAYER_CHANGED_NAME = 22,
			CODE_PLAYER_CHANGED_COLOR = 23,
			CODE_BOARD_CHANGED_SIZE = 24;
	static final byte
			CODE_MADE_MOVE = 99;

	private HostGameScreen(ScreenManager.UIScreen prev, @NonNull GameParameters params) {
		super(prev, params);
	}

	@Override
	protected void makeMove(Move move) {
		applyMove(move, params.players, getPI(), board, pieces);
		// Send move to players (except current)
		Player p;
		byte[] bytes = {CODE_MADE_MOVE, move.x, move.y, move.i};
		for (int pi = 0; pi < params.players.size; pi++)
			if (pi != getPI() && (p = params.players.get(pi)) instanceof BTPlayer)
				Connection.btManager.writeTo(((BTPlayer) p).deviceInterface, bytes);
		nextPlayer();
	}

	@Override
	void restart() {
		// Send restart to players
		byte[] bytes = {CODE_GAME_RESTARTED};
		for (Player p : params.players)
			if (p instanceof BTPlayer)
				Connection.btManager.writeTo(((BTPlayer) p).deviceInterface, bytes);
		super.restart();
	}

	@Override
	protected void removePlayer(byte pi) {
		// Send player leave to players
		byte[] bytes = {CODE_PLAYER_LEFT, pi};
		for (Player p : params.players)
			if (p instanceof BTPlayer)
				Connection.btManager.writeTo(((BTPlayer) p).deviceInterface, bytes);
		super.removePlayer(pi);
	}

	@Override
	protected void buildUI() {
		super.buildUI();

		final TextButton restart = new TextButton("Restart", Gui.skin());
		restart.setPosition(Gdx.graphics.getWidth() - Gui.sparsity() - restart.getWidth(), Gui.sparsity());
		restart.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				restart();
			}
		});
		ui.addActor(restart);
	}

	@Override
	protected void onQuit() {
		// Send quit to players
		byte[] bytes = {CODE_GAME_CLOSED};
		for (Player p : params.players)
			if (p instanceof BTPlayer)
				Connection.btManager.writeTo(((BTPlayer) p).deviceInterface, bytes); // Keep connection, for setup menu
	}

	@Override
	public void onStateChanged(BluetoothState state) {
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
	}

	@Override
	public void onDeviceConnected(BluetoothConnectedDeviceInterface deviceConnected) {
	}

	@Override
	public void onDeviceDisconnected(BluetoothConnectedDeviceInterface deviceDisconnected) {
		Player p;
		for (byte pi = (byte) (params.players.size - 1); pi >= 0; pi--)
			if ((p = params.players.get(pi)) instanceof BTPlayer && ((BTPlayer) p).deviceInterface == deviceDisconnected) {
				removePlayer(pi);
				break;
			}
	}

	@Override
	public void onRead(BluetoothConnectedDeviceInterface from, byte[] bytes) {
		byte opCode = bytes[0];

		if (opCode == CODE_MADE_MOVE) {
			for (byte pi = 0; pi < params.players.size; pi++)
				if (params.players.get(pi) instanceof BTPlayer && pi == getPI())
					makeMove(new Move(bytes[1], bytes[2], bytes[3]));
		} else if (opCode == CODE_PLAYER_LEFT)
			from.disconnect();
	}

	@Override
	public void onDiscoveryStateChanged(boolean enabled) {
	}

	@Override
	public void show() {
		super.show();
		Connection.btManager.setBluetoothListener(this);
	}
}
