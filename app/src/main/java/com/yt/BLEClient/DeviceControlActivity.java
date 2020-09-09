/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yt.BLEClient;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Thread.sleep;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private Switch CntDcnt;
    private TextView mConnectionState , testText;
    private TextView TirePerssure , BatteryVoltage  , TirePuncture , Temperature ;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic ;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            final String action = intent.getAction();
            Runnable updater = null;

            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                    mConnected = true;
                updateConnectionState(R.string.connected);
                CntDcnt.setText("Connect");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                CntDcnt.setText("Disconnect");

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                final Handler handler = new Handler();
                Timer timer = new Timer();
                TimerTask doTask = new TimerTask() {
                    @Override
                    public void run() {
                        handler.post(new Runnable() {
                            @SuppressWarnings("unchecked")
                            public void run() {
                                try {
                                    // Show all the supported services and characteristics on the user interface.
                                displayGattServices(mBluetoothLeService.getSupportedGattServices());
                                if (mNotifyCharacteristic != null) {
                                    final int charaProp = mNotifyCharacteristic.getProperties();
                                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                                        mBluetoothLeService.readCharacteristic(mNotifyCharacteristic);
                                    }
                                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                                        mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, true);
                                    }
                                }

                            }
                                catch (Exception e) {
                                    // TODO Auto-generated catch block
                                }
                                             }
                        });
                    }
                };
                timer.schedule(doTask, 0, 500);

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
               String uuid = mNotifyCharacteristic.getUuid().toString();
                if(uuid.equals("00002a2b-0000-1000-8000-00805f9b34fb"))
                {
                    displayData(intent.getStringExtra(BluetoothLeService.Tire_Pressure));
                    displayData2(intent.getStringExtra(BluetoothLeService.Battery_Voltage));
                    displayData3(intent.getStringExtra(BluetoothLeService.Tire_Puncture));
                    displayData4(intent.getStringExtra(BluetoothLeService.Temperature));

                }
            }
        }

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashbord);
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        // Sets up UI references.
        CntDcnt = (Switch) findViewById(R.id.switch1);
        TirePerssure = (TextView) findViewById(R.id.value2);
        BatteryVoltage = (TextView) findViewById(R.id.value3);
        TirePuncture = (TextView) findViewById(R.id.value4);
        Temperature = (TextView) findViewById(R.id.value1);
        final Intent gattServiceIntent = new Intent(DeviceControlActivity.this, BluetoothLeService.class);

        CntDcnt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                {
                    bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
                }
                else
                {
                    mBluetoothLeService.disconnect();
                    unbindService(mServiceConnection);
                    mBluetoothLeService.close();
                    mBluetoothLeService=null;
                }


            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);

        }


    }
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;

    }


    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            TirePerssure.setText(data);
        }
    }
    private void displayData2(String data) {
        if (data != null) {
            BatteryVoltage.setText(data);
        }
    }
    private void displayData3(String data) {
        if (data != null) {
            TirePuncture.setText(data);
        }
    } private void displayData4(String data) {
        if (data != null) {
            Temperature.setText(data);
        }
    }


    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.

        private void displayGattServices(List<BluetoothGattService> gattServices) {

            if (gattServices == null)
                return;
            String uuid = null;
            String serviceString = "unknown service";
            String charaString = "unknown characteristic";

            for (BluetoothGattService gattService : gattServices) {

                uuid = gattService.getUuid().toString();
                if (uuid.equals("00001805-0000-1000-8000-00805f9b34fb")){
                   if (serviceString != null) {
                    List<BluetoothGattCharacteristic> gattCharacteristics =
                            gattService.getCharacteristics();
                    for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                        HashMap<String, String> currentCharaData = new HashMap<String, String>();
                        uuid = gattCharacteristic.getUuid().toString();
                        if(uuid.equals("00002a2b-0000-1000-8000-00805f9b34fb"))
                        {
                            mNotifyCharacteristic = gattCharacteristic;
                        }

                       } }
                } }

        }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
