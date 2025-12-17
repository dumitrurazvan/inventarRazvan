package ro.cnpr.inventar.print;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ESCPrinter {

    // Helper to set the absolute print position (in dots) in page mode.
    private static void setAbsolutePosition(ByteArrayOutputStream stream, int x, int y) throws IOException {
        // Set horizontal position: ESC $ nL nH
        stream.write(new byte[]{0x1B, 0x24, (byte) (x % 256), (byte) (x / 256)});
        // Set vertical position: GS $ nL nH
        stream.write(new byte[]{0x1D, 0x24, (byte) (y % 256), (byte) (y / 256)});
    }

    /**
     * Generates ESC/POS commands to print a label for a 50x32mm media size.
     * NOTE: Printer initialization (ESC @) is handled by the calling manager class.
     */
    public static byte[] getESCCommand(String nrInventar, String itemName, String location, String gestionar, String date) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            // Enter Page Mode
            stream.write(new byte[]{0x1B, 0x4C});

            // Set print area for 50x32mm label (400x256 dots at 203dpi)
            stream.write(new byte[]{0x1B, 0x57, 0, 0, 0, 0, (byte) 144, 1, (byte) 0, 1});

            // --- 1. QR Code ---
            byte[] qrData = nrInventar.getBytes(StandardCharsets.UTF_8);
            setAbsolutePosition(stream, 20, 56); // Adjusted for QR size
            stream.write(new byte[]{0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00}); // Model: QR Code Model 2
            stream.write(new byte[]{0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, 0x07}); // Module Size: 7 dots
            stream.write(new byte[]{0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x31}); // Error Correction: Level M
            int dataLength = qrData.length + 3;
            stream.write(new byte[]{0x1D, 0x28, 0x6B, (byte) (dataLength % 256), (byte) (dataLength / 256), 0x31, 0x50, 0x30});
            stream.write(qrData);
            stream.write(new byte[]{0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30}); // Print QR

            // --- 2. Text Lines ---

            // Print nrInventar (larger, bolded)
            setAbsolutePosition(stream, 190, 65);
            stream.write(new byte[]{0x1B, 0x45, 0x01}); // Bold on
            stream.write(new byte[]{0x1D, 0x21, 0x01}); // Double height
            stream.write(nrInventar.getBytes("GBK"));
            stream.write(new byte[]{0x1B, 0x45, 0x00}); // Bold off
            stream.write(new byte[]{0x1D, 0x21, 0x00}); // Normal size

            // --- Print item name (asset description), wrapped to 2 lines ---

            String itemNameToPrint = itemName != null ? itemName : "";

            if (itemNameToPrint.length() > 16) {
                itemNameToPrint = itemNameToPrint.substring(0, 16);
            }

            String line1 = itemNameToPrint;
            String line2 = "";

            if (itemNameToPrint.length() > 16) {
                int splitPos = itemNameToPrint.lastIndexOf(' ', 16);
                if (splitPos > 0) {
                    line2 = itemNameToPrint.substring(splitPos).trim();
                    line1 = itemNameToPrint.substring(0, splitPos);
                } else {
                    // No space found, hard wrap
                    line2 = itemNameToPrint.substring(16);
                    line1 = itemNameToPrint.substring(0, 16);
                }
            }

            int currentY = 115; // Start Y-pos below the inventory number
            setAbsolutePosition(stream, 190, currentY);
            stream.write(line1.getBytes("GBK"));

            if (!line2.isEmpty()) {
                currentY += 35; // Move to next line
                setAbsolutePosition(stream, 190, currentY);
                stream.write(line2.getBytes("GBK"));
            }

            // --- Print Room Number and Building Name from 'location' parameter ---
            currentY += 35; // Move to next line for room number
            setAbsolutePosition(stream, 190, currentY);
            stream.write(location.getBytes("GBK"));

            // --- Print Gestionar ---
            currentY += 35; // Move to next line for room number
            setAbsolutePosition(stream, 190, currentY);
            String gestionarToPrint = gestionar != null ? gestionar : "";
            if (gestionarToPrint.length() > 16) {
                gestionarToPrint = gestionarToPrint.substring(0, 16);
            }
            stream.write(gestionarToPrint.getBytes("GBK"));

            // Print the page and exit page mode
            stream.write(0x0C); // FF (Form Feed)

            return stream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
}