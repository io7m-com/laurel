/*
 * Copyright © 2024 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */


package com.io7m.laurel.io.internal;

import com.io7m.anethum.api.SerializationException;
import com.io7m.laurel.io.LSchemas;
import com.io7m.laurel.model.LImage;
import com.io7m.laurel.model.LImageCaption;
import com.io7m.laurel.model.LImageSet;
import com.io7m.laurel.writer.api.LSerializerType;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * A manifest serializer.
 */

public final class LSerializer implements LSerializerType
{
  private final XMLOutputFactory factory;
  private final OutputStream stream;
  private XMLStreamWriter writer;

  /**
   * A manifest serializer.
   *
   * @param inStream The output stream
   */

  public LSerializer(
    final OutputStream inStream)
  {
    this.stream =
      Objects.requireNonNull(inStream, "stream");
    this.factory =
      XMLOutputFactory.newFactory();
  }

  private static String findNS()
  {
    return LSchemas.schema1().namespace().toString();
  }

  @Override
  public void execute(
    final LImageSet info)
    throws SerializationException
  {
    try {
      final var outputBuffer =
        new ByteArrayOutputStream();
      this.writer =
        this.factory.createXMLStreamWriter(outputBuffer, "UTF-8");

      this.writer.writeStartDocument("UTF-8", "1.0");
      this.writer.writeStartElement("ImageSet");
      this.writer.writeDefaultNamespace(findNS());

      this.writeCaptions(info);
      this.writeImages(info);

      this.writer.writeEndElement();
      this.writer.writeEndDocument();

      final var transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      final var result =
        new StreamResult(this.stream);
      final var source =
        new StreamSource(new ByteArrayInputStream(outputBuffer.toByteArray()));

      this.stream.write("""
<?xml version="1.0" encoding="UTF-8"?>

      """.getBytes(StandardCharsets.UTF_8));

      transformer.transform(source, result);
    } catch (final XMLStreamException | TransformerException | IOException e) {
      throw new SerializationException(e.getMessage(), e);
    }
  }

  private void writeImages(
    final LImageSet info)
    throws XMLStreamException
  {
    this.writer.writeStartElement("Images");
    for (final var image : info.images().values()) {
      this.writeImage(image);
    }
    this.writer.writeEndElement();
  }

  private void writeImage(
    final LImage image)
    throws XMLStreamException
  {
    this.writer.writeStartElement("Image");
    this.writer.writeAttribute("ID", image.imageID().toString());
    this.writer.writeAttribute("FileName", image.fileName());

    final var captions = image.captions();
    if (!captions.isEmpty()) {
      for (final var caption : captions) {
        this.writer.writeStartElement("CaptionReference");
        this.writer.writeAttribute("Caption", caption.id().toString());
        this.writer.writeEndElement();
      }
    }

    this.writer.writeEndElement();
  }

  private void writeCaptions(
    final LImageSet info)
    throws XMLStreamException
  {
    this.writer.writeStartElement("Captions");
    for (final var caption : info.captions().values()) {
      this.writeCaption(caption);
    }
    this.writer.writeEndElement();
  }

  private void writeCaption(
    final LImageCaption caption)
    throws XMLStreamException
  {
    this.writer.writeStartElement("Caption");
    this.writer.writeAttribute("ID", caption.id().toString());
    this.writer.writeAttribute("Text", caption.text());
    this.writer.writeEndElement();
  }

  @Override
  public void close()
  {

  }
}
