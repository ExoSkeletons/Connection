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
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ByteArray;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AndroidLauncher extends AndroidApplication implements PermissionsManager, BluetoothManager<BTPairedDeviceAdapter, BTConnectedDeviceAdapter> {
	// Server
	private final static class BTAcceptClientsTask extends BTSocketTask<BluetoothServerSocket, BTConnectedDeviceAdapter, Void> {
		private final BluetoothManager<?, BTConnectedDeviceAdapter> btManager;
		private final Lock accessLock;

		BTAcceptClientsTask(@NonNull BluetoothServerSocket serverSocket, BluetoothManager<?, BTConnectedDeviceAdapter> btManager, Lock accessLock) {
			super(serverSocket);
			this.btManager = btManager;
			this.accessLock = accessLock;
		}

		@Override
		protected Void doInBackground(BluetoothServerSocket serverSocket) {
			while (!isCancelled())
				// Loop accepting forever, until accepting is canceled.
				try {
					// Wait for incoming requests
					// NOTE: Cancelling the task closes the socket and closing the socket aborts
					// the blocking done by accept() so we do not need to worry.
					publishProgress(new BTConnectedDeviceAdapter(serverSocket.accept()));
				} catch (IOException e) {
					if (!(e instanceof SocketTimeoutException))
						cancel(true);
				}
			return null;
		}

		@Override
		protected void onProgressUpdate(final BTConnectedDeviceAdapter... progress) {
			accessLock.lock();
			btManager.getConnectedDevices().addAll(progress);
			accessLock.unlock();
			final BluetoothListener btListener = btManager.getBluetoothListener();
			if (btListener != null)
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						for (BTConnectedDeviceAdapter connectedDevice : progress)
							btListener.onDeviceConnected(connectedDevice);
					}
				});
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			close();
		}
	}

	// Read loop
	private static final class BTReadLoopTask extends AsyncTask<Void, Packet<BTConnectedDeviceAdapter, ByteArray>, Void> {
		private final BluetoothManager<BTPairedDeviceAdapter, BTConnectedDeviceAdapter> btManager;
		private final ReentrantLock accessLock;

		BTReadLoopTask(BluetoothManager<BTPairedDeviceAdapter, BTConnectedDeviceAdapter> btManager, ReentrantLock accessLock) {
			this.btManager = btManager;
			this.accessLock = accessLock;
		}

		@Override
		protected Void doInBackground(Void... voids) {
			final Array<BTConnectedDeviceAdapter> connectedDevices = new Array<>();
			BTConnectedDeviceAdapter deviceInterface;
			InputStream is;
			while (!isCancelled()) {
				connectedDevices.clear();
				accessLock.lock();
				connectedDevices.addAll(btManager.getConnectedDevices());
				accessLock.unlock();
				for (int i = connectedDevices.size - 1; i >= 0 && !isCancelled(); i--) {
					try {
						accessLock.lock();
						deviceInterface = connectedDevices.get(i);
						is = deviceInterface.getInputStream();
						if (is.available() > 0) { // check first if there's stuff to read. can't call read() itself since its a blocking call and will hold up reading the next device
							System.out.println("reading from " + deviceInterface.getName() + "...");
							byte[] buffer = new byte[1024];
							int count = is.read(buffer);
							System.out.println("read " + count + " bytes [" + Arrays.toString(buffer) + "]");
							accessLock.unlock();
							if (count > 0) {
								// Read byte array length, then read [size] bytes from buffer into new byte array
								for (int bytesStart = 0, bytesLength; bytesStart < count; bytesStart += bytesLength) {
									bytesLength = buffer[bytesStart];
									bytesStart++;
									ByteArray byteArray = new ByteArray(true, buffer, bytesStart, bytesLength);
									System.out.println("publishing packet " + byteArray);
									//noinspection unchecked
									publishProgress(new Packet<>(deviceInterface, byteArray));
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						if (accessLock.isLocked()) accessLock.unlock();
					}
				}
			}
			return null;
		}

		@SafeVarargs
		@Override
		protected final void onProgressUpdate(final Packet<BTConnectedDeviceAdapter, ByteArray>... progress) {
			final BluetoothListener btListener = btManager.getBluetoothListener();
			if (btListener != null)
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						for (Packet<BTConnectedDeviceAdapter, ByteArray> packet : progress)
							btListener.onRead(packet.sender, packet.message.toArray());
					}
				});
		}

		@Override
		protected void onCancelled(Void aVoid) {
			System.err.println("read task canceled");
		}
	}

	private static final int REQ_MAKE_DISCOVERABLE = 8574;

	private final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
	private final Array<BTPairedDeviceAdapter> foundDevices = new Array<>();
	private final Array<BTConnectedDeviceAdapter> connectedDevices = new Array<>();
	private final ReentrantLock
			connectedDevicesAccessLock = new ReentrantLock(true),
			foundDevicesAccessLock = new ReentrantLock(true);
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
					if (intent != null) {
						final String action = intent.getAction();
						int prev;

						if (action != null) {
							switch (action) {
								case BluetoothAdapter.ACTION_STATE_CHANGED:
									prev = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_OFF);
									final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, prev);
									if (state != prev) if (btListener != null)
										Gdx.app.postRunnable(new Runnable() {
											@Override
											public void run() {
												btListener.onStateChanged(getState(state));
											}
										});
									break;
								case BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:
									if (btListener != null) {
										prev = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE, -1);
										final int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, prev);
										if (mode != prev)
											Gdx.app.postRunnable(new Runnable() {
												@Override
												public void run() {
													btListener.onDiscoverableStateChanged(mode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
												}
											});
									}
									break;
								case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
									foundDevicesAccessLock.lock();
									connectedDevicesAccessLock.lock();
									for (int i = getPairedDevices().size - 1; i >= 0; i--) {
										boolean connected = false;
										for (BTConnectedDeviceAdapter connectedDevice : getConnectedDevices())
											if (connectedDevice.getDevice().equals(getPairedDevices().get(i).getDevice())) {
												connected = true;
												break;
											}
										if (!connected) getPairedDevices().removeIndex(i);
									}
									connectedDevicesAccessLock.unlock();
									foundDevicesAccessLock.unlock();
									if (btListener != null)
										Gdx.app.postRunnable(new Runnable() {
											@Override
											public void run() {
												btListener.onDiscoveryStateChanged(true);
											}
										});
									break;
								case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
									if (btListener != null)
										Gdx.app.postRunnable(new Runnable() {
											@Override
											public void run() {
												btListener.onDiscoveryStateChanged(false);
											}
										});
									break;
							}

							final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
							if (device != null) {
								switch (action) {
									case BluetoothDevice.ACTION_FOUND:
										// Register found devices
										boolean found = false;
										for (BTPairedDeviceAdapter pd : foundDevices)
											if (pd.getDevice().equals(device)) {
												found = true;
												break;
											}
										if (!found) {
											final BTPairedDeviceAdapter pairedDevice = new BTPairedDeviceAdapter(device);
											foundDevicesAccessLock.lock();
											foundDevices.add(pairedDevice);
											foundDevicesAccessLock.unlock();
											if (btListener != null)
												Gdx.app.postRunnable(new Runnable() {
													@Override
													public void run() {
														btListener.onDiscoverDevice(pairedDevice);
													}
												});
										}
										break;
									case BluetoothDevice.ACTION_ACL_CONNECTED:
										// Connected device interfaces are created on-connection inside
										// the accept Task, where we have the socket to pass
										break;
									case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
										prev = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_NONE);
										int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, prev);
										System.out.println("bt bond state change: " + prev + "->" + state);
										break;
									case BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED:
									case BluetoothDevice.ACTION_ACL_DISCONNECTED:
										// Register device disconnections, and close connected sockets.
										BTConnectedDeviceAdapter d;
										for (int i = connectedDevices.size - 1; i >= 0; i--) {
											d = connectedDevices.get(i);
											if (d.getDevice().equals(device)) {
												connectedDevicesAccessLock.lock();
												connectedDevices.removeIndex(i);
												connectedDevicesAccessLock.unlock();
												if (btListener != null) {
													final BTConnectedDeviceAdapter disconnected = d;
													Gdx.app.postRunnable(new Runnable() {
														@Override
														public void run() {
															btListener.onDeviceDisconnected(disconnected);
															btListener.onDisconnectedFromDevice(disconnected);
															// Closing the connection has to be called last, since
															// we can't access device after closing.
															disconnected.closeConnection();
														}
													});
												} else
													// If there's no listener to fire, we can just close the connection from here.
													d.closeConnection();
												return;
											}
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

		(btReadLoopTask = new BTReadLoopTask(this, connectedDevicesAccessLock)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

		initialize(new Connection(this, this), config);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQ_MAKE_DISCOVERABLE) if (resultCode == RESULT_CANCELED)
			if (btListener != null)
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						btListener.onDiscoverableStateChanged(false);
					}
				});

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
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						btListener.onStateChanged(BluetoothState.OFF);
					}
				});
			return;
		}
		if (btAdapter.isEnabled() == enable) return;
		if (enable) btAdapter.enable();
		else btAdapter.disable();
	}

	@Override
	public boolean isEnabled() {
		return btAdapter.isEnabled();
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
			btAcceptTask = new BTAcceptClientsTask(serverSocket, this, foundDevicesAccessLock);
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
			OutputStream os = device.getOutputStream();
			// Write bytes length, then write bytes
			byte[] bytes_ = new byte[bytes.length + 1];
			bytes_[0] = (byte) bytes.length;
			System.arraycopy(bytes, 0, bytes_, 1, bytes.length);
			// Write all bytes in one call to avoid errs
			os.write(bytes_);
		} catch (IOException e) {
			System.err.println("err writing" + Arrays.toString(bytes) + " to " + device);
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
	public synchronized Array<BTPairedDeviceAdapter> getPairedDevices() {
		return foundDevices;
	}

	@Override
	public synchronized Array<BTConnectedDeviceAdapter> getConnectedDevices() {
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

	@Override
	public String getName() {
		return btAdapter.getName();
	}
}
