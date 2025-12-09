package ro.cnpr.inventar.print;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Builds ESC/POS commands for the HPRT HM-A300E (or similar).
 *
 * Layout for 50x32mm label:
 * - Left half: QR code with nrInventar.
 * - Right half (top -> bottom):
 *   1) Big bold nrInventar
 *   2) Title (caracteristici / denumire)
 *   3) Locatie - camera
 *   4) Data: yyyy-MM-dd
 */
public class ESCPrinter {

    private static void setAbsolutePosition(ByteArrayOutputStream stream, int x, int y) throws IOException {
        stream.write(new byte[]{0x1B, 0x24, (byte) (x % 256), (byte) (x / 256)});
        stream.write(new byte[]{0x1D, 0x24, (byte) (y % 256), (byte) (y / 256)});
    }

    /**
     * Builds ESC/POS bytes for one label.
     *
     * @param nrInventar      QR content + large text.
     * @param title           asset title (caracteristici / denumire).
     * @param locationCamera  e.g. "DACIA - 203".
     * @param dateText        e.g. "2025-01-15" (already formatted). May be null.
     */
    public static byte[] buildLabelCommand(String nrInventar,
                                           String title,
                                           String locationCamera,
                                           String dateText) {
        try {
            if (nrInventar == null) {
                nrInventar = "";
            }
            if (title == null) {
                title = "";
            }
            if (locationCamera == null) {
                locationCamera = "";
            }
            if (dateText == null) {
                dateText = "";
            }

            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            stream.write(new byte[]{0x1B, 0x40});
            stream.write(new byte[]{0x1B, 0x4C});
            stream.write(new byte[]{
                    0x1B, 0x57,
                    0x00, 0x00,
                    0x00, 0x00,
                    (byte) 144, 0x01,
                    0x00, 0x01
            });


            byte[] qrData = nrInventar.getBytes(StandardCharsets.UTF_8);

            setAbsolutePosition(stream, 24, 65);

            stream.write(new byte[]{0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00});
            stream.write(new byte[]{0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, 0x06});
            stream.write(new byte[]{0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x31});

            int dataLength = qrData.length + 3;
            stream.write(new byte[]{
                    0x1D, 0x28, 0x6B,
                    (byte) (dataLength % 256),
                    (byte) (dataLength / 256),
                    0x31, 0x50, 0x30
            });
            stream.write(qrData);
            stream.write(new byte[]{0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30});

            setAbsolutePosition(stream, 190, 60);
            stream.write(new byte[]{0x1B, 0x45, 0x01});
            stream.write(new byte[]{0x1D, 0x21, 0x01});
            stream.write(nrInventar.getBytes("GBK"));
            stream.write(new byte[]{0x1B, 0x45, 0x00});
            stream.write(new byte[]{0x1D, 0x21, 0x00});

            setAbsolutePosition(stream, 190, 100);
            stream.write(title.getBytes("GBK"));

            setAbsolutePosition(stream, 190, 135);
            stream.write(locationCamera.getBytes("GBK"));

            if (!dateText.isEmpty()) {
                setAbsolutePosition(stream, 190, 170);
                String dateLine = "Data: " + dateText;
                stream.write(dateLine.getBytes("GBK"));
            }

            stream.write(0x0C);

            return stream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
}
