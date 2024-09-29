laurel
===

[![Maven Central](https://img.shields.io/maven-central/v/com.io7m.laurel/com.io7m.laurel.svg?style=flat-square)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.io7m.laurel%22)
[![Maven Central (snapshot)](https://img.shields.io/nexus/s/com.io7m.laurel/com.io7m.laurel?server=https%3A%2F%2Fs01.oss.sonatype.org&style=flat-square)](https://s01.oss.sonatype.org/content/repositories/snapshots/com/io7m/laurel/)
[![Codecov](https://img.shields.io/codecov/c/github/io7m-com/laurel.svg?style=flat-square)](https://codecov.io/gh/io7m-com/laurel)
![Java Version](https://img.shields.io/badge/21-java?label=java&color=e6c35c)

![com.io7m.laurel](./src/site/resources/laurel.jpg?raw=true)

| JVM | Platform | Status |
|-----|----------|--------|
| OpenJDK (Temurin) Current | Linux | [![Build (OpenJDK (Temurin) Current, Linux)](https://img.shields.io/github/actions/workflow/status/io7m-com/laurel/main.linux.temurin.current.yml)](https://www.github.com/io7m-com/laurel/actions?query=workflow%3Amain.linux.temurin.current)|
| OpenJDK (Temurin) LTS | Linux | [![Build (OpenJDK (Temurin) LTS, Linux)](https://img.shields.io/github/actions/workflow/status/io7m-com/laurel/main.linux.temurin.lts.yml)](https://www.github.com/io7m-com/laurel/actions?query=workflow%3Amain.linux.temurin.lts)|
| OpenJDK (Temurin) Current | Windows | [![Build (OpenJDK (Temurin) Current, Windows)](https://img.shields.io/github/actions/workflow/status/io7m-com/laurel/main.windows.temurin.current.yml)](https://www.github.com/io7m-com/laurel/actions?query=workflow%3Amain.windows.temurin.current)|
| OpenJDK (Temurin) LTS | Windows | [![Build (OpenJDK (Temurin) LTS, Windows)](https://img.shields.io/github/actions/workflow/status/io7m-com/laurel/main.windows.temurin.lts.yml)](https://www.github.com/io7m-com/laurel/actions?query=workflow%3Amain.windows.temurin.lts)|

## laurel

The `laurel` package attempts to provide tools to assist
with _image captioning_ within the context of [machine
learning](https://en.wikipedia.org/wiki/Machine_learning).

In particular, the application is geared towards the management of smaller
datasets (in the range of thousands of images) for use in techniques such as
[LORA](https://en.wikipedia.org/wiki/Fine-tuning_(deep_learning)#Low-rank_adaptation)
training.

![Screenshot](./src/site/resources/screenshot.png?raw=true)

## Features

* A user interface for managing images and captions for those images.
* A caption categorization system for assisting with keeping captions consistent across large datasets.
* The ability to import captions and images into a dataset from a directory hierarchy.
* The ability to export captions and images into a directory for use in training scripts.
* A persistent undo/redo system that can store every change ever made to a
  dataset, including the ability to effectively revert to an earlier version
  at any time.
* Datasets are backed by [SQLite](https://www.sqlite.org) for reliable,
  transactional updates, and a file format that is designed to endure for
  decades to come.
* Command line tools for automating operations such as importing, exporting,
  and interrogating metadata.
* Comprehensive documentation.
* [OSGi](https://www.osgi.org/)-ready.
* [JPMS](https://en.wikipedia.org/wiki/Java_Platform_Module_System)-ready.
* ISC license.

## Usage

See the [documentation](https://www.io7m.com/software/laurel).


