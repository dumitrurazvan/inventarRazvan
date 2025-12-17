package ro.cnpr.inventar.print;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Thin wrapper used by Activities.
 *
 * IMPORTANT:
 *  - This runs synchronously and may block the UI thread for ~2-4s
 *    when connecting the first time. For the internal MVP this is OK.
 *    If it feels too slow in practice, we can later move this to a
 *    background thread and change the Activity logic slightly.
 */
public class PrinterHelper {

    private static final String TAG = "PrinterHelper";

    /**
     * Change this to match the Bluetooth name of your printer
     * (as seen in Android's Bluetooth settings).
     */
    private static final String DEFAULT_PRINTER_NAME = "HM-A300-0F44";

    /**
     * Prints a label via Bluetooth.
     *
     * @param context        for Toasts.
     * @param nrInventar     QR content and big text.
     * @param title          asset title (caracteristici / denumire).
     * @param locationCamera e.g. "DACIA - 203".
     * @param gestionar      the asset owner.
     * @return true if sending data to the printer succeeded, false otherwise.
     */
    public static boolean printLabel(Context context,
                                     String nrInventar,
                                     String title,
                                     String locationCamera,
                                     String gestionar) {
        HprtPrinterManager manager = HprtPrinterManager.getInstance();

        try {
            if (!manager.isConnected()) {
                Log.d(TAG, "Printer not connected, attempting connection to "
                        + DEFAULT_PRINTER_NAME + "...");
                manager.connect(DEFAULT_PRINTER_NAME);
            }

            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(new Date());

            manager.printLabel(nrInventar, title, locationCamera, gestionar, date);
            Log.d(TAG, "printLabel OK for nrInventar=" + nrInventar);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error while printing label", e);
            if (context != null) {
                Toast.makeText(
                        context,
                        "Eroare la tipărire: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
            return false;
        }
    }
}
