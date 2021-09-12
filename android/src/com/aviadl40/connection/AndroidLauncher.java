package com.aviadl40.connection;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.aviadl40.connection.game.managers.BluetoothManager;
import com.aviadl40.connection.game.managers.PermissionsManager;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ByteArray;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.UUID;

public class AndroidLauncher extends AndroidApplication implements PermissionsManager, BluetoothManager<BTDeviceAdapter.BTPairedDeviceAdapter, BTDeviceAdapter.BTConnectedDeviceAdapter> {
	// Server
	private static final class BTAcceptClientsTask extends AsyncTask<Void, Void, Void> implements Closeable {
		private final Array<BTDeviceAdapter.BTConnectedDeviceAdapter> connectedDevices;
		private BluetoothServerSocket serverSocket = null;

		BTAcceptClientsTask(BluetoothAdapter btAdapter, Array<BTDeviceAdapter.BTConnectedDeviceAdapter> connectedDevices, String name, UUID uuid) {
			this.connectedDevices = connectedDevices;
			try {
				btAdapter.cancelDiscovery();
				serverSocket = btAdapter.listenUsingRfcommWithServiceRecord(name, uuid);
			} catch (IOException e) {
				e.printStackTrace();
				onCancelled();
			}
		}

		@Override
		protected Void doInBackground(Void[] params) {
			while (!isCancelled())
				try {
					// Wait for incoming requests
					connectedDevices.add(new BTDeviceAdapter.BTConnectedDeviceAdapter(serverSocket.accept()));
				} catch (IOException e) {
					e.printStackTrace();
					if (!(e instanceof SocketTimeoutException))
						cancel(true);
				}
			return null;
		}

		@Override
		public void close() {
			cancel(true);
		}

		@Override
		protected void onCancelled() {
			try {
				if (serverSocket != null)
					serverSocket.close();
			} catch (IOException ignored) {
			}
		}
	}

	// Read loop
	private static final class BTReadLoopTask extends AsyncTask<Void, Utils.Packet<BTDeviceAdapter.BTConnectedDeviceAdapter, ByteArray>, Void> {
		private final BluetoothManager<BTDeviceAdapter.BTPairedDeviceAdapter, BTDeviceAdapter.BTConnectedDeviceAdapter> btManager;

		BTReadLoopTask(BluetoothManager<BTDeviceAdapter.BTPairedDeviceAdapter, BTDeviceAdapter.BTConnectedDeviceAdapter> btManager) {
			this.btManager = btManager;
		}

		@Override
		protected Void doInBackground(Void... voids) {
			System.out.println("starting read task.");
			while (!isCancelled()) {
				byte[] buffer = new byte[1024];
				int count;
				for (BTDeviceAdapter.BTConnectedDeviceAdapter deviceInterface : btManager.getConnectedDevices()) {
					try {
						System.out.println("reading...");
						count = deviceInterface.getInputStream().read(buffer);
						System.out.println("read [" + count + "] bytes");
						ByteArray byteArray = new ByteArray(buffer);
						byteArray.shrink();
						if (count > 0)
							publishProgress(new Utils.Packet<>(deviceInterface, byteArray));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			return null;
		}

		@SafeVarargs
		@Override
		protected final void onProgressUpdate(Utils.Packet<BTDeviceAdapter.BTConnectedDeviceAdapter, ByteArray>... values) {
			System.out.println("on progress update:");
			BluetoothListener listener = btManager.getBluetoothListener();
			if (listener != null)
				for (Utils.Packet<BTDeviceAdapter.BTConnectedDeviceAdapter, ByteArray> packet : values)
					btManager.getBluetoothListener().onRead(packet.senderInterface, packet.message.toArray());
		}
	}

	private static final int REQ_MAKE_DISCOVERABLE = 8574;

	private final Game game = new Connection(this, this);
	private final Array<BTDeviceAdapter.BTPairedDeviceAdapter> pairedDevices = new Array<>();
	private final Array<BTDeviceAdapter.BTConnectedDeviceAdapter> connectedDevices = new Array<>();
	private BroadcastReceiver btBroadcastHandle;
	@Nullable
	private BluetoothManager.BluetoothListener btListener = null;
	private BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
	@Nullable
	private AsyncTask<?, ?, ?> btTask = null;
	@Nullable
	private BTReadLoopTask readLoopTask = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		config.useAccelerometer = false;
		config.useCompass = false;
		config.useGyroscope = false;

		if (bluetoothSupported()) {
			btBroadcastHandle = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					if (btListener != null && intent != null) {
						String action = intent.getAction();
						int prev;
						if (action != null)
							switch (action) {
								case BluetoothAdapter.ACTION_STATE_CHANGED:
									prev = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_OFF);
									int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, prev);
									if (state != prev)
										btListener.onStateChanged(getState(state));
									break;
								case BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:
									prev = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE, BluetoothAdapter.SCAN_MODE_NONE);
									int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, prev);
									if (mode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE && prev != mode)
										btListener.onDiscoverableStateChanged(true);
									else if (mode != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE && prev == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
										btListener.onDiscoverableStateChanged(false);
									break;
								case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
									btListener.onDiscoveryStateChanged(true);
									break;
								case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
									btListener.onDiscoveryStateChanged(false);
									break;

								case BluetoothDevice.ACTION_FOUND:
									final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
									if (device != null) {
										final BTDeviceAdapter.BTPairedDeviceAdapter btDeviceInterface = new BTDeviceAdapter.BTPairedDeviceAdapter(device);
										pairedDevices.add(btDeviceInterface);
										btListener.onDiscoverDevice(btDeviceInterface);
									}
									break;
							}
					}

				}
			};
			final IntentFilter btIntentFilter = new IntentFilter();
			btIntentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
			btIntentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
			btIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
			btIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
			btIntentFilter.addAction(BluetoothDevice.ACTION_FOUND);
			registerReceiver(btBroadcastHandle, btIntentFilter);
		}

		(readLoopTask = new BTReadLoopTask(this)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

		initialize(game, config);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQ_MAKE_DISCOVERABLE)
			if (resultCode == RESULT_CANCELED)
				if (btListener != null)
					btListener.onDiscoverableStateChanged(false);
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(btBroadcastHandle);
		if (btTask != null)
			btTask.cancel(true);
		if (readLoopTask != null)
			readLoopTask.cancel(true);
		btAdapter.cancelDiscovery();
	}

	private String getPermName(Permission perm) {
		switch (perm) {
			case BLUETOOTH:
				return Manifest.permission.BLUETOOTH;
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
			if (name != null && checkSelfPermission(name) != PackageManager.PERMISSION_GRANTED)
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
		requestPermissions(names, 42);
	}

	@Override
	public boolean bluetoothSupported() {
		return btAdapter != null;
	}

	@Override
	public void requestEnable(boolean enable) {
		if (!bluetoothSupported()) return;
		if (enable && !hasPermissions(Permission.BLUETOOTH, Permission.LOCATION_COARSE, Permission.LOCATION_FINE)) {
			requestPermissions(Permission.LOCATION_FINE, Permission.LOCATION_COARSE, Permission.BLUETOOTH);
			requestEnable(false);
			if (btListener != null)
				btListener.onStateChanged(BluetoothState.OFF);
			return;
		}
		if (btAdapter.isEnabled() == enable) return;
		if (enable) btAdapter.enable();
		else btAdapter.disable();
	}

	private BluetoothState getState(int stateID) {
		if (bluetoothSupported())
			switch (stateID) {
				case BluetoothAdapter.STATE_ON:
					return BluetoothState.ON;
				case BluetoothAdapter.STATE_OFF:
					return BluetoothState.OFF;
				case BluetoothAdapter.STATE_TURNING_ON:
					return BluetoothState.TURNING_ON;
				case BluetoothAdapter.STATE_TURNING_OFF:
					return BluetoothState.TURNING_OFF;
			}
		return BluetoothState.OFF;
	}

	@Override
	public BluetoothState getState() {
		return getState(btAdapter.getState());
	}

	@Override
	public Closeable host(String name, UUID uuid) {
		if (btTask != null)
			btTask.cancel(true);
		btTask = new BTAcceptClientsTask(btAdapter, connectedDevices, name, uuid);
		return (Closeable) btTask;
	}

	@Override
	public void writeTo(BTDeviceAdapter.BTConnectedDeviceAdapter device, byte[] bytes) {
		if (!bluetoothSupported()) return;
		try {
			device.getOutputStream().write(bytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void requestMakeDiscoverable(int duration) {
		if (!bluetoothSupported()) return;
		btAdapter.cancelDiscovery();
		startActivityForResult(
				new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
						.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, duration),
				REQ_MAKE_DISCOVERABLE
		);
	}

	@Override
	public void enableDiscovery(boolean enabled) {
		if (!bluetoothSupported()) return;
		btAdapter.cancelDiscovery();
		if (enabled)
			btAdapter.startDiscovery();
	}

	@Override
	public Array<BTDeviceAdapter.BTPairedDeviceAdapter> getPairedDevices() {
		return pairedDevices;
	}

	@Override
	public Array<BTDeviceAdapter.BTConnectedDeviceAdapter> getConnectedDevices() {
		return connectedDevices;
	}

	@Override
	@Nullable
	public BluetoothListener getBluetoothListener() {
		return btListener;
	}

	@Override
	public void setBluetoothListener(@Nullable BluetoothManager.BluetoothListener listener) {
		if (!bluetoothSupported()) return;
		if (listener == btListener) return;
		btListener = listener;
		if (listener != null)
			listener.onStateChanged(getState());
	}
}
