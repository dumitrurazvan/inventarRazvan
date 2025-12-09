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
     * The layout mimics the qr_scan_item.xml view, with the QR code on the left
     * and text information on the right.
     *
     * @return A byte array containing the ESC/POS commands.
     */
    public static byte[] getESCCommand(String nrInventar, String building, String room, String date) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            String shortDate = (date != null && date.length() > 10) ? date.substring(0, 10) : date;

            // Initialize printer - resets any previous settings.
            stream.write(new byte[]{0x1B, 0x40});

            // --- Begin Page Mode Block ---
            // Enter Page Mode: ESC L
            stream.write(new byte[]{0x1B, 0x4C});

            // Set the print area for page mode: ESC W xL xH yL yH dxL dxH dyL dyH
            // Based on 203dpi (8 dots/mm): 50mm width = 400 dots, 32mm height = 256 dots.
            // Area starts at 0,0 and has a width of 400 and height of 256.
            stream.write(new byte[]{0x1B, 0x57, 0, 0, 0, 0, (byte) 144, 1, (byte) 0, 1});

            // --- 1. Place and Print QR Code ---
            byte[] qrData = nrInventar.getBytes(StandardCharsets.UTF_8);

            // Set position for QR code (X=24 dots, Y=65 dots)
            setAbsolutePosition(stream, 24, 65);

            // Generate QR Code (using standard ESC/POS commands)
            stream.write(new byte[]{0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00}); // Model: QR Code Model 2
            stream.write(new byte[]{0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, 0x06}); // Module Size: 6 dots
            stream.write(new byte[]{0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x31}); // Error Correction: Level M

            int dataLength = qrData.length + 3;
            stream.write(new byte[]{0x1D, 0x28, 0x6B, (byte) (dataLength % 256), (byte) (dataLength / 256), 0x31, 0x50, 0x30});
            stream.write(qrData); // Store data
            stream.write(new byte[]{0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30}); // Print from storage

            // --- 2. Place and Print Text Lines ---

            // Print nrInventar (larger, bolded)
            setAbsolutePosition(stream, 190, 65);
            stream.write(new byte[]{0x1B, 0x45, 0x01}); // Bold on
            stream.write(new byte[]{0x1D, 0x21, 0x01}); // Double height
            stream.write(nrInventar.getBytes("GBK"));
            stream.write(new byte[]{0x1B, 0x45, 0x00}); // Bold off
            stream.write(new byte[]{0x1D, 0x21, 0x00}); // Normal size

            // NOTE: The 'building' parameter actually contains the asset's title/description,
            // and the 'room' parameter contains the location string. The labels on the
            // printed output are based on the original project's format.

            // Y-position is adjusted to prevent overlap with the double-height inventory number.
            // Print Title (passed in 'building' parameter) with "Cladire:" prefix
            setAbsolutePosition(stream, 190, 115); // Was 105, caused overlap
            stream.write(("Cladire: " + building).getBytes("GBK"));

            // Print Location (passed in 'room' parameter) with "Camera:" prefix
            setAbsolutePosition(stream, 190, 140); // Was 135
            stream.write(("Camera: " + room).getBytes("GBK"));

            // Print Date
            setAbsolutePosition(stream, 190, 165);
            stream.write(("Data: " + shortDate).getBytes("GBK"));

            // Print the content of the page and return to standard mode: FF
            stream.write(0x0C);

            // --- End Page Mode Block ---

            return stream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0]; // Return empty array on error
        }
    }
}
