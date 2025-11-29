package com.sorinwork.tickets.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class QrCodeServiceTest {

  @Test
  void rendersAReadablePng() throws Exception {
    byte[] png = new QrCodeService().png("K7M2Q9X4P3TZ", 200);

    BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
    assertThat(image.getWidth()).isEqualTo(200);
    var bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
    assertThat(new QRCodeReader().decode(bitmap).getText()).isEqualTo("K7M2Q9X4P3TZ");
  }
}
