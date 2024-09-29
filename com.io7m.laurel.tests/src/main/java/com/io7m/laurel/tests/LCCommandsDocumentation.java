/*
 * Copyright © 2023 Mark Raynsford <code@io7m.com> https://www.io7m.com
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


package com.io7m.laurel.tests;

import com.io7m.laurel.cmdline.internal.LCExport;
import com.io7m.laurel.cmdline.internal.LCImport;
import com.io7m.quarrel.core.QCommandOrGroupType;
import com.io7m.quarrel.core.QCommandParserConfiguration;
import com.io7m.quarrel.core.QCommandParsers;
import com.io7m.quarrel.core.QCommandType;
import com.io7m.quarrel.core.QValueConverterDirectory;
import com.io7m.quarrel.ext.xstructural.QCommandXS;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public final class LCCommandsDocumentation
{
  private LCCommandsDocumentation()
  {

  }

  public static void main(
    final String[] args)
  {
    final var parsers =
      new QCommandParsers();
    final var xs =
      new QCommandXS("xs", true);

    final var parserConfig =
      new QCommandParserConfiguration(
        QValueConverterDirectory.core(),
        QCommandParsers.emptyResources()
      );

    final var commands =
      List.of(
        new LCExport(),
        new LCImport()
      );

    final SortedMap<String, QCommandOrGroupType> commandsByName =
      new TreeMap<>();

    for (final var command : commands) {
      commandsByName.put(command.metadata().name(), command);
    }

    for (final var command : commands) {
      writeMain(command, parsers, parserConfig, commandsByName, xs);
      writeParameters(command, parsers, parserConfig, commandsByName, xs);
    }
  }

  private static void writeMain(
    final QCommandType command,
    final QCommandParsers parsers,
    final QCommandParserConfiguration parserConfig,
    final SortedMap<String, QCommandOrGroupType> commandsByName,
    final QCommandXS xs)
  {
    try {
      final var name =
        command.metadata().name();
      final var parser =
        parsers.create(parserConfig);

      final var textWriter =
        new StringWriter();
      final var writer =
        new PrintWriter(textWriter);

      final var context =
        parser.execute(
          commandsByName,
          writer,
          xs,
          List.of(
            "--type",
            "main",
            "--parameters-include",
            "scmd-%s-parameters.xml",
            name
          )
        );

      context.execute();
      writer.println();
      writer.flush();

      final var path =
        Paths.get("/shared-tmp/scmd-%s.xml".formatted(name));

      Files.writeString(path, textWriter.toString(), StandardCharsets.UTF_8);
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  private static void writeParameters(
    final QCommandType command,
    final QCommandParsers parsers,
    final QCommandParserConfiguration parserConfig,
    final SortedMap<String, QCommandOrGroupType> commandsByName,
    final QCommandXS xs)
  {
    try {
      final var name =
        command.metadata().name();
      final var parser =
        parsers.create(parserConfig);

      final var textWriter =
        new StringWriter();
      final var writer =
        new PrintWriter(textWriter);

      final var context =
        parser.execute(
          commandsByName,
          writer,
          xs,
          List.of(
            "--type",
            "parameters",
            "--parameters-include",
            "scmd-%s-parameters.xml",
            name
          )
        );

      context.execute();
      writer.println();
      writer.flush();

      final var path =
        Paths.get("/shared-tmp/scmd-%s-parameters.xml".formatted(name));

      Files.writeString(path, textWriter.toString(), StandardCharsets.UTF_8);
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }
}
