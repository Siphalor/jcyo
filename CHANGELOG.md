# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed

- Remove incorrect `PathSensitivity` annotation on output directory

## [0.6.2] - 2026-03-15

### Changed

- Mark directory properties of the Gradle task with `PathSensitivity.RELATIVE`
- Some internal Gradle build improvements

## [0.6.1] - 2026-02-22

### Changed

- The `JcyoTask` is now skipped, when the input directory does not exist.
