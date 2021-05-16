package utils

import org.apache.xml.security.utils
import org.w3c.dom.{Document, Node}

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, OutputStream}
import java.net.{URLDecoder, URLEncoder}
import java.util.Base64
import java.util.zip.{Deflater, DeflaterOutputStream, Inflater, InflaterInputStream, InflaterOutputStream}

object XMLUtils {
  def docToString(d: Document) = {
    val bs = new ByteArrayOutputStream()
    utils.XMLUtils.outputDOM(d, bs)
    bs.toString()
  }

  def printDoc(d: Document) = {
    println(docToString(d))
  }

  def decodeURLParamToXML(document: String, charset: String) = {
    // Not needed because Play already decodes that for us
    // val urlDec = URLDecoder.decode(document, charset)
    val b64dec = Base64.getDecoder.decode(document)

    val inflater = new Inflater(true)
    val output = new ByteArrayOutputStream()
    val inflaterOs = new InflaterOutputStream(output, inflater)
    inflaterOs.write(b64dec)

    inflaterOs.close()
    output.flush()
    inflater.end()

    scala.xml.XML.load(new ByteArrayInputStream(output.toByteArray))
  }
}
