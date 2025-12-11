package ro.cnpr.inventar.print;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

/**
 * Very small Bluetooth manager for HPRT HM-A300E-like printers.
 *
 * Usage:
 *   HprtPrinterManager mgr = HprtPrinterManager.getInstance();
 *   mgr.connect(printerName);      // once
 *   mgr.printLabel(...);           // multiple times
 *   mgr.disconnect();              // optional on app exit
 */
public class HprtPrinterManager {

    private static final String TAG = "HprtPrinterManager";
    private static HprtPrinterManager instance;

    private BluetoothSocket socket;
    private OutputStream outputStream;
    private BluetoothDevice printerDevice;

    private HprtPrinterManager() {
    }

    public static synchronized HprtPrinterManager getInstance() {
        if (instance == null) {
            instance = new HprtPrinterManager();
        }
        return instance;
    }

    private BluetoothDevice findPrinter(String printerName) throws SecurityException {
        if (printerDevice != null) {
            return printerDevice;
        }

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            Log.e(TAG, "Bluetooth adapter is null (device has no BT?).");
            return null;
        }
        if (!adapter.isEnabled()) {
            Log.e(TAG, "Bluetooth is disabled.");
            return null;
        }

        Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
        if (pairedDevices != null && !pairedDevices.isEmpty()) {
            for (BluetoothDevice device : pairedDevices) {
                if (device != null && device.getName() != null &&
                        device.getName().equals(printerName)) {
                    printerDevice = device;
                    Log.d(TAG, "Printer found: " + device.getName() + " [" + device.getAddress() + "]");
                    return printerDevice;
                }
            }
        }

        Log.e(TAG, "Printer with name '" + printerName + "' not found among paired devices.");
        return null;
    }

    public synchronized void connect(String printerName) throws IOException {
        if (isConnected()) {
            Log.d(TAG, "Already connected to printer.");
            return;
        }

        try {
            BluetoothDevice device = findPrinter(printerName);
            if (device == null) {
                throw new IOException("Printer '" + printerName + "' not found or not paired.");
            }

            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

            Log.d(TAG, "Creating RFCOMM socket...");
            socket = device.createRfcommSocketToServiceRecord(uuid);

            Log.d(TAG, "Connecting to printer...");
            socket.connect();
            outputStream = socket.getOutputStream();
            Log.d(TAG, "Printer connected successfully.");
        } catch (SecurityException se) {
            throw new IOException("Bluetooth permission error (BLUETOOTH_CONNECT / SCAN).", se);
        } catch (IOException ioe) {
            disconnect();
            throw ioe;
        }
    }

    public synchronized boolean isConnected() {
        return socket != null && socket.isConnected() && outputStream != null;
    }

    public synchronized void disconnect() {
        Log.d(TAG, "Disconnecting from printer...");
        try {
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing output stream", e);
        }

        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing Bluetooth socket", e);
        }

        outputStream = null;
        socket = null;
        printerDevice = null;
    }

    /**
     * Sends one label to the printer. Assumes we are already connected.
     */
    public synchronized void printLabel(String nrInventar,
                                        String title,
                                        String locationCamera,
                                        String dateText) throws IOException {
        if (!isConnected()) {
            throw new IOException("Printer not connected.");
        }

        // Explicitly reset the printer to a known state before each print job.
        outputStream.write(new byte[]{0x1B, 0x40}); // ESC @ - Initialize Printer

        byte[] data = ESCPrinter.getESCCommand(nrInventar, title, locationCamera, dateText);
        if (data.length == 0) {
            throw new IOException("ESC/POS command buffer is empty (build failed).");
        }

        Log.d(TAG, "Sending " + data.length + " bytes to printer...");
        outputStream.write(data);
        outputStream.flush();
        Log.d(TAG, "Label data sent.");
    }
}
