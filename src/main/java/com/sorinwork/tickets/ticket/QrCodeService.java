package com.sorinwork.tickets.ticket;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class QrCodeService {

  /** Renders {@code text} as a square PNG QR code of {@code size} pixels. */
  public byte[] png(String text, int size) {
    try {
      BitMatrix matrix =
          new QRCodeWriter()
              .encode(
                  text,
                  BarcodeFormat.QR_CODE,
                  size,
                  size,
                  Map.of(
                      EncodeHintType.ERROR_CORRECTION,
                      ErrorCorrectionLevel.M,
                      EncodeHintType.MARGIN,
                      2));
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      MatrixToImageWriter.writeToStream(matrix, "PNG", out);
      return out.toByteArray();
    } catch (WriterException e) {
      throw new IllegalArgumentException("Cannot encode QR code", e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
