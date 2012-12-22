package com.mathieuclement.android.kjunior.remote;

import android.app.Activity;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;
import com.mathieuclement.android.kjunior.remote.dialog.AlertDialogFragment;
import com.mathieuclement.android.kjunior.remote.dialog.ChooseDialogFragment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class RemoteControlActivity extends Activity implements ChooseDialogFragment.ChooseDialogListener {

    // TODO I suppose the user has already paired his phone with the Bluetooth device
    // It would be better that we query paired devices, and if not found we scan the devices, ask for pairing and so on...

    private static final String TAG = "KJuniorRemote";

    // See file:///home/mathieu/dev/android-sdk-linux/docs/guide/topics/connectivity/bluetooth.html
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter bAdapter;
    private List<BluetoothDevice> robotBluetoothDevices;
    private BluetoothDevice activeBluetoothDevice;
    private BluetoothSocket virtualPortSocket;
    private InputStream receiveStream = null; // Bluetooth (virtual) serial reception channel
    private OutputStream sendStream = null; // Bluetooth (virtual) serial emission channel

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Set initial status
        setStatus("Hello, tell me what to do.");

        // Start data reception thread
        SerialPortMessageReceptionThread receiverThread = new SerialPortMessageReceptionThread(handler);
        receiverThread.start();

        // "Connect to KJunior" button listener
        Button connectButton = (Button) findViewById(R.id.connectButton);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showConnectDialog();
            }
        });


        bAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bAdapter == null) {
            // This device does not support bluetooth
            Log.e(TAG, "Device does not support Bluetooth!");

            // Show dialog
            DialogFragment newFragment = AlertDialogFragment.newInstance("Fatal error",
                    "Bluetooth is a critical requirement but is not available on your device.");
            newFragment.show(getFragmentManager(), "bluetooth_missing_dialog");
        } else {
            // Check bluetooth is turned on
            if (!bAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                // Ask Android OS (and hence the user) to enable it
                // This is an asynchronous process, the result will be a method call to onActivityResult()
                // with the passed code (REQUEST_ENABLED_BT)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    private void showConnectDialog() {
        // Find Bluetooth devices
        Set<BluetoothDevice> pairedDevices = bAdapter.getBondedDevices();

        // K-Junior devices
        robotBluetoothDevices = new LinkedList<BluetoothDevice>();

        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                //mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                //Log.i(TAG, "Bluetooth device { name : " + device.getName() + ", address : " + device.getAddress());

                // Add device to list of robots if it's a K-Junior
                if (device.getName().startsWith("K-Junior")) {
                    robotBluetoothDevices.add(device);
                }
            }
        }

        if (robotBluetoothDevices.isEmpty()) {
            AlertDialogFragment.newInstance("Device not found", "No K-Junior robot could be found. Please note that" +
                    "the device should already be paired on phone for this to work.");

            return;
        }

        // Sort robotBluetoothDevices by name
        Collections.sort(robotBluetoothDevices, new Comparator<BluetoothDevice>() {
            @Override
            public int compare(BluetoothDevice lhs, BluetoothDevice rhs) {
                return lhs.getName().compareTo(rhs.getName());
            }
        });

        // Make an array with the names of K-Junior devices
        String[] robotsNames = new String[robotBluetoothDevices.size()];
        for (int i = 0; i < robotBluetoothDevices.size(); i++) {
            robotsNames[i] = robotBluetoothDevices.get(i).getName();
        }

        // Display a bluetooth device chooser
        ChooseDialogFragment chooseDialogFragment = ChooseDialogFragment.newInstance("Choose device", robotsNames);
        chooseDialogFragment.show(getFragmentManager(), "choose_kjunior_bluetooth_device_dialog");

        // When item is selected or dialog is cancelled, an event will fire the onChooseDialog...() methods
    }

    private void connectToKJunior(int deviceIndex) {
        setStatus("Connecting to KJunior device...");

        // Make Bluetooth connection with the Bluetooth device at index deviceIndex of robotDevices list
        activeBluetoothDevice = robotBluetoothDevices.get(deviceIndex);

        // Create a socket to operate as a virtual serial port
        // See http://nononux.free.fr/index.php?page=elec-brico-bluetooth-android-microcontroleur
        try {
            virtualPortSocket = activeBluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));

            receiveStream = virtualPortSocket.getInputStream();
            sendStream = virtualPortSocket.getOutputStream();

            // Connect to device (on virtual serial port socket)
            new Thread() {
                @Override
                public void run() {
                    try {
                        virtualPortSocket.connect();

                        handler.setStatus("Connected to KJunior device.");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                final Button connectButton = (Button) findViewById(R.id.connectButton);
                                connectButton.setText("Disconnect from KJunior");
                                connectButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        // Disconnect
                                        new Thread() {
                                            @Override
                                            public void run() {
                                                try {
                                                    virtualPortSocket.close();
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }.start();
                                        setStatus("Disconnected. Tell me what to do now.");

                                        connectButton.setText("Connect to KJunior");
                                        connectButton.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                showConnectDialog();
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Error while connecting to socket", e);
                        handler.setStatus("Error! Could not connect to virtual serial port!");
                    }
                }
            }.start();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Error while creating socket", e);
            handler.setStatus("Error! Could not create virtual serial port!");
        }
    }

    private void setStatus(String str) {
        TextView statusTextView = (TextView) findViewById(R.id.status_textView);
        statusTextView.setText(str);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                setStatus("Bluetooth is enabled.");
            }
        }
    }

    @Override
    public void onChooseDialogItemClicked(DialogFragment dialog, int which) {
        connectToKJunior(which);
    }

    @Override
    public void onChooseDialogCancelled(DialogFragment dialog) {
    }

    // A toggle button from the view was "toggled"
    public void onToggleClicked(View view) {
        // Is the toggle on?
        boolean on = ((ToggleButton) view).isChecked();

        if (view.equals(findViewById(R.id.buzzer_toggleButton))) {
            if (on) {
                // Buzz!
                sendSerialPortData("H,1\n");
            } else {
                // Stop buzzing
                sendSerialPortData("H,0\n");
            }
        }
    }

    public void sendSerialPortData(String data) {
        try {
            // Write message to sending buffer
            sendStream.write(data.getBytes());

            // Make sure they are sent now
            sendStream.flush();

        } catch (IOException e) {
            e.printStackTrace();
            setStatus("Could not transmit last bunch of data.");
        }
    }

    private class MyHandler extends Handler {
        private MyHandler() {
        }

        public void handleMessage(Message msg) {
            String data = msg.getData().getString("receivedData");
            Log.i("KJuniorData", data);
        }

        public void setStatus(final String status) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    RemoteControlActivity.this.setStatus(status);
                }
            });
        }
    }

    final MyHandler handler = new MyHandler();

    private class SerialPortMessageReceptionThread extends Thread {
        Handler handler;

        SerialPortMessageReceptionThread(Handler h) {
            handler = h;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    // On teste si des données sont disponibles
                    if (receiveStream != null && virtualPortSocket.isConnected() && receiveStream.available() > 0) {
                        byte buffer[] = new byte[100];

                        // On lit les données, k représente le nombre de bytes lu
                        int k = receiveStream.read(buffer, 0, 100);

                        if (k > 0) {
                            // On convertit les données en String
                            byte rawdata[] = new byte[k];
                            System.arraycopy(buffer, 0, rawdata, 0, k);

                            String data = new String(rawdata);

                            // On envoie les données dans le thread de l'UI pour les affichées
                            Message msg = handler.obtainMessage();
                            Bundle b = new Bundle();
                            b.putString("receivedData", data);
                            msg.setData(b);
                            handler.sendMessage(msg);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
