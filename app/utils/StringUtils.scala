package utils

import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.{Deflater, DeflaterOutputStream, Inflater, InflaterOutputStream}

object StringUtils {
  def deflateBase64(str: String, charset: String = "UTF-8"): String = {
    val deflate = new Deflater(Deflater.DEFAULT_COMPRESSION, true)
    val output = new ByteArrayOutputStream()
    val deflater = new DeflaterOutputStream(output, deflate, true)
    deflater.write(str.getBytes(charset))
    deflater.flush()
    deflater.close()
    deflate.end()

    Base64.getUrlEncoder.encodeToString(output.toByteArray)
  }

  def inflateBase64(str: String, charset: String = "UTF-8"): String = {
    val inflate = new Inflater(true)
    val output = new ByteArrayOutputStream()
    val inflater = new InflaterOutputStream(output, inflate)
    inflater.write(Base64.getUrlDecoder.decode(str))
    inflater.flush()
    inflater.close()
    inflate.end()

    output.toString(charset)
  }
}
