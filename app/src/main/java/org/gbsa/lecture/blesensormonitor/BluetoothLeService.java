package org.gbsa.lecture.blesensormonitor;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BluetoothLeService extends Service {
    private static final String TAG = "CSH_BLE";

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);
    public final static UUID UUID_BATTERY_LEVEL_MEASUREMENT =
            UUID.fromString(SampleGattAttributes.BATTERY_LEVEL_MEASUREMENT);

   private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
       @Override
       public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
           super.onPhyUpdate(gatt, txPhy, rxPhy, status);
       }

       @Override
       public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
           super.onPhyRead(gatt, txPhy, rxPhy, status);
       }

       @Override
       public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
           String intentAction;
           if (newState == BluetoothProfile.STATE_CONNECTED) {
               intentAction = ACTION_GATT_CONNECTED;
               mConnectionState = STATE_CONNECTED;
               broadcastUpdate(intentAction);
               Log.i(TAG, "Connected to GATT server.");
               // Attempts to discover services after successful connection.
               Log.i(TAG, "Attempting to start service discovery:" +
                       mBluetoothGatt.discoverServices());
           } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
               intentAction = ACTION_GATT_DISCONNECTED;
               mConnectionState = STATE_DISCONNECTED;
               Log.i(TAG, "Disconnected from GATT server.");
               broadcastUpdate(intentAction);
           }
       }

       @Override
       public void onServicesDiscovered(BluetoothGatt gatt, int status) {
           if (status == BluetoothGatt.GATT_SUCCESS) {
               broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
           } else {
               Log.w(TAG, "onServicesDiscovered received: " + status);
           }
       }

       @Override
       public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
           if (status == BluetoothGatt.GATT_SUCCESS) {
               broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
           }
       }

       @Override
       public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
           super.onCharacteristicWrite(gatt, characteristic, status);
       }

       @Override
       public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
           broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
       }

       @Override
       public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
           super.onDescriptorRead(gatt, descriptor, status);
       }

       @Override
       public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
           super.onDescriptorWrite(gatt, descriptor, status);
       }

       @Override
       public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
           super.onReliableWriteCompleted(gatt, status);
       }

       @Override
       public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
           super.onReadRemoteRssi(gatt, rssi, status);
       }

       @Override
       public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
           super.onMtuChanged(gatt, mtu, status);
       }

       @Override
       public void onServiceChanged(@NonNull BluetoothGatt gatt) {
           super.onServiceChanged(gatt);
       }
   };

   private void broadcastUpdate(final String action) {
       final Intent intent = new Intent(action);
       sendBroadcast(intent);
   }

   private void broadcastUpdate(final String action,
                                final BluetoothGattCharacteristic characteristic) {
       final Intent intent = new Intent(action);

       if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
           int flag = characteristic.getProperties();
           int format = -1;
           if((flag & 0x01) != 0) {
               format = BluetoothGattCharacteristic.FORMAT_UINT16;
               Log.d(TAG, "Heart rate format UINT16.");
           } else {
               format = BluetoothGattCharacteristic.FORMAT_UINT8;
               Log.d(TAG, "Heart rate format UINT8.");
           }
           final int heartRate = characteristic.getIntValue(format, 1);
           Log.d(TAG, String.format("Received heart rate: %d", heartRate));
           intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
       } else if (UUID_BATTERY_LEVEL_MEASUREMENT.equals(characteristic.getUuid())) {
           int level =  characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
           intent.putExtra(EXTRA_DATA, String.valueOf(level));
       } else {
           final byte[] data = characteristic.getValue();
           if (data != null && data.length > 0) {
               final StringBuilder stringBuilder = new StringBuilder(data.length);
               for (byte byteChar : data) {
                   stringBuilder.append(String.format("%02X ", byteChar));
               }
               intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
           }
       }
       sendBroadcast(intent);
   }

    public List<BluetoothGattService> getSupportedGattServices() {
       if (mBluetoothGatt == null) return null;

       return mBluetoothGatt.getServices();
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
       close();
       return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain BluetoothAdapter.");
            return false;
        }

        return true;
    }

    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");

            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // This is specific to Heart Rate Measurement.
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }
}
