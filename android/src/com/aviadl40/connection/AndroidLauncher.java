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
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.aviadl40.connection.BTDeviceAdapter.BTConnectedDeviceAdapter;
import com.aviadl40.connection.BTDeviceAdapter.BTPairedDeviceAdapter;
import com.aviadl40.connection.game.managers.BluetoothManager;
import com.aviadl40.connection.game.managers.PermissionsManager;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ByteArray;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.UUID;

public class AndroidLauncher extends AndroidApplication implements PermissionsManager, BluetoothManager<BTPairedDeviceAdapter, BTConnectedDeviceAdapter> {
	// Server
	private static final class BTAcceptClientsTask extends BTSocketTask<BluetoothServerSocket, BTConnectedDeviceAdapter> {
		private final Array<BTConnectedDeviceAdapter> connectedDevices;

		BTAcceptClientsTask(@NonNull BluetoothServerSocket serverSocket, Array<BTConnectedDeviceAdapter> connectedDevices) {
			super(serverSocket);
			this.connectedDevices = connectedDevices;
		}

		@Override
		protected Void doInBackground(Object... params) {
			while (!isCancelled())
				// Loop accepting forever, until accepting is canceled.
				try {
					// Wait for incoming requests
					// NOTE: Cancelling the task closes the socket and closing the socket aborts
					// the blocking done by accept() so we do not need to worry.
					publishProgress(new BTConnectedDeviceAdapter(socket.accept()));
				} catch (IOException e) {
					e.printStackTrace();
					if (!(e instanceof SocketTimeoutException))
						cancel(true);
				}
			return null;
		}

		@Override
		protected void onProgressUpdate(BTConnectedDeviceAdapter... connectedDevices) {
			this.connectedDevices.addAll(connectedDevices);
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			close();
		}
	}

	// Read loop
	private static final class BTReadLoopTask extends AsyncTask<Void, Packet<BTConnectedDeviceAdapter, ByteArray>, Void> {
		private final BluetoothManager<BTPairedDeviceAdapter, BTConnectedDeviceAdapter> btManager;

		BTReadLoopTask(BluetoothManager<BTPairedDeviceAdapter, BTConnectedDeviceAdapter> btManager) {
			this.btManager = btManager;
		}

		@Override
		protected Void doInBackground(Void... voids) {
			System.out.println("starting read task.");
			while (!isCancelled()) {
				byte[] buffer = new byte[1024];
				int count;
				for (BTConnectedDeviceAdapter deviceInterface : btManager.getConnectedDevices()) {
					try {
						System.out.println("reading from " + deviceInterface + "...");
						count = deviceInterface.getInputStream().read(buffer);
						System.out.println("read [" + count + "] bytes");
						if (count > 0) {
							ByteArray byteArray = new ByteArray(buffer);
							byteArray.shrink();
							//noinspection unchecked
							publishProgress(new Packet<>(deviceInterface, byteArray));
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			return null;
		}

		@SafeVarargs
		@Override
		protected final void onProgressUpdate(Packet<BTConnectedDeviceAdapter, ByteArray>... values) {
			System.out.println("on progress update:");
			BluetoothListener listener = btManager.getBluetoothListener();
			if (listener != null)
				for (Packet<BTConnectedDeviceAdapter, ByteArray> packet : values)
					btManager.getBluetoothListener().onRead(packet.sender, packet.message.toArray());
		}
	}

	private static final int REQ_MAKE_DISCOVERABLE = 8574;

	private final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
	private final Array<BTPairedDeviceAdapter> pairedDevices = new Array<>();
	private final Array<BTConnectedDeviceAdapter> connectedDevices = new Array<>();
	private BroadcastReceiver btBroadcastHandle;
	@Nullable
	private BluetoothManager.BluetoothListener btListener = null;

	@Nullable
	private BTAcceptClientsTask btAcceptTask = null;
	@Nullable
	private BTReadLoopTask btReadLoopTask = null;

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
						final String action = intent.getAction();
						int prev;

						if (action != null) {
							switch (action) {
								case BluetoothAdapter.ACTION_STATE_CHANGED:
									prev = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_OFF);
									int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, prev);
									if (state != prev) btListener.onStateChanged(getState(state));
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
							}

							final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
							if (device != null) {
								switch (action) {
									case BluetoothDevice.ACTION_FOUND:
										boolean paired = false;
										for (BTPairedDeviceAdapter pd : pairedDevices)
											if (pd.getDevice().equals(device)) {
												paired = true;
												break;
											}
										if (!paired) {
											final BTPairedDeviceAdapter btDeviceInterface = new BTPairedDeviceAdapter(device);
											pairedDevices.add(btDeviceInterface);
											btListener.onDiscoverDevice(btDeviceInterface);
										}
										break;
									case BluetoothDevice.ACTION_ACL_CONNECTED:
										// Connected device interfaces are created on-connection inside
										// the accept Task, where we have the socket to pass
										break;
									case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
										prev = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_NONE);
										int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, prev);
										break;
									case BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED:
									case BluetoothDevice.ACTION_ACL_DISCONNECTED:
										// Register device disconnections, and close connected sockets.
										for (int i = pairedDevices.size - 1; i >= 0; i--)
											if (pairedDevices.get(i).getDevice().equals(device)) {
												pairedDevices.removeIndex(i);
												return;
											}
										for (int i = connectedDevices.size - 1; i >= 0; i--)
											if (connectedDevices.get(i).getDevice().equals(device)) {
												connectedDevices.get(i).closeConnection();
												return;
											}
										break;
								}
							}
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
			btIntentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
			btIntentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
			btIntentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
			btIntentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
			registerReceiver(btBroadcastHandle, btIntentFilter);
		}

		(btReadLoopTask = new BTReadLoopTask(this)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

		initialize(new Connection(this, this), config);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQ_MAKE_DISCOVERABLE) if (resultCode == RESULT_CANCELED)
			if (btListener != null)
				btListener.onDiscoverableStateChanged(false);

		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(btBroadcastHandle);
		if (btAcceptTask != null) btAcceptTask.cancel(true);
		if (btReadLoopTask != null) btReadLoopTask.cancel(true);
		btAdapter.cancelDiscovery();
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
		Permission[] permissions = {Permission.BLUETOOTH, Permission.BLUETOOTH_ADMIN, Permission.LOCATION_COARSE, Permission.LOCATION_FINE};
		if (enable && !hasPermissions(permissions)) {
			requestPermissions(permissions);
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
		if (btAcceptTask != null) btAcceptTask.cancel(true);
		btAdapter.cancelDiscovery();
		try {
			BluetoothServerSocket serverSocket = btAdapter.listenUsingRfcommWithServiceRecord(name, uuid);
			btAcceptTask = new BTAcceptClientsTask(serverSocket, connectedDevices);
			btAcceptTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			// NOTE: we do not close the server socket, as the accept task just got it and needs it open.
			// The accept task therefore is now the one in charge of closing the socket after it's done.
			return btAcceptTask;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void writeTo(BTConnectedDeviceAdapter device, byte[] bytes) {
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
		if (enabled) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				// Android 10 and above require location services to be enabled
				// in order to perform bluetooth discovery
				LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
				if (locationManager != null && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
					startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
					return;
				}
			}

			btAdapter.startDiscovery();
		}
	}

	@Override
	public Array<BTPairedDeviceAdapter> getPairedDevices() {
		return pairedDevices;
	}

	@Override
	public Array<BTConnectedDeviceAdapter> getConnectedDevices() {
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
