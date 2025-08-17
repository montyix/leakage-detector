package com.pipelinedetector;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Service for managing Bluetooth connections
 */
public class BluetoothService {
    private static final String TAG = "BluetoothService";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard SerialPortService ID
    
    private final BluetoothAdapter bluetoothAdapter;
    private final Handler handler;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private int state;
    
    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // not connected
    public static final int STATE_CONNECTING = 1; // connecting
    public static final int STATE_CONNECTED = 2;  // connected
    
    // Message types sent to the Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    
    /**
     * Constructor
     * 
     * @param handler Handler to send messages back to the UI Activity
     */
    public BluetoothService(Handler handler) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        state = STATE_NONE;
        this.handler = handler;
    }
    
    /**
     * Set the current state of the chat connection
     * 
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + this.state + " -> " + state);
        this.state = state;
        
        // Give the new state to the Handler so the UI Activity can update
        handler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }
    
    /**
     * Return the current connection state
     */
    public synchronized int getState() {
        return state;
    }
    
    /**
     * Start the service
     */
    public synchronized void start() {
        Log.d(TAG, "start");
        
        // Cancel any thread attempting to make a connection
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        
        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        
        setState(STATE_NONE);
    }
    
    /**
     * Start the ConnectThread to initiate a connection to a remote device
     * 
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);
        
        // Cancel any thread attempting to make a connection
        if (state == STATE_CONNECTING) {
            if (connectThread != null) {
                connectThread.cancel();
                connectThread = null;
            }
        }
        
        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        
        // Start the thread to connect with the given device
        connectThread = new ConnectThread(device);
        connectThread.start();
        setState(STATE_CONNECTING);
    }
    
    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * 
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        Log.d(TAG, "connected");
        
        // Cancel the thread that completed the connection
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        
        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        
        // Start the thread to manage the connection and perform transmissions
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();
        
        // Send the name of the connected device back to the UI Activity
        handler.obtainMessage(MESSAGE_DEVICE_NAME, 0, -1, device.getName()).sendToTarget();
        
        setState(STATE_CONNECTED);
    }
    
    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Log.d(TAG, "stop");
        
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        
        setState(STATE_NONE);
    }
    
    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * 
     * @param out The bytes to write
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (state != STATE_CONNECTED) return;
            r = connectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }
    
    /**
     * Indicate that the connection attempt failed and notify the UI Activity
     */
    private void connectionFailed() {
        setState(STATE_NONE);
        
        // Send a failure message back to the Activity
        handler.obtainMessage(MESSAGE_TOAST, 0, -1, "Unable to connect device").sendToTarget();
    }
    
    /**
     * Indicate that the connection was lost and notify the UI Activity
     */
    private void connectionLost() {
        setState(STATE_NONE);
        
        // Send a failure message back to the Activity
        handler.obtainMessage(MESSAGE_TOAST, 0, -1, "Device connection was lost").sendToTarget();
    }
    
    /**
     * Thread for trying to establish a Bluetooth connection
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        
        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            
            // Get a BluetoothSocket for a connection with the given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket creation failed", e);
            }
            mmSocket = tmp;
        }
        
        public void run() {
            Log.i(TAG, "BEGIN connectThread");
            setName("ConnectThread");
            
            // Always cancel discovery because it will slow down a connection
            bluetoothAdapter.cancelDiscovery();
            
            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }
            
            // Reset the ConnectThread because we're done
            synchronized (BluetoothService.this) {
                connectThread = null;
            }
            
            // Start the connected thread
            connected(mmSocket, mmDevice);
        }
        
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
    
    /**
     * Thread for handling Bluetooth connections
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        
        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            
            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }
            
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        
        public void run() {
            Log.i(TAG, "BEGIN connectedThread");
            byte[] buffer = new byte[1024];
            int bytes;
            
            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    
                    // Send the obtained bytes to the UI Activity
                    handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }
        
        /**
         * Write to the connected OutStream
         * 
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                
                // Share the sent message back to the UI Activity
                handler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }
        
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
