# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-09-25

### Added
- Foreground `dataSync` service compatible with Android 14/15 (`onTimeout` lifecycle support).
- Dual-layer captive portal detection:
  - RFC 8908 via system `LinkProperties`
  - HTTP `generate_204` probes across multiple selectable endpoints (Google Android, Gstatic, Cloudflare).
- Network bound in-app browser using `bindProcessToNetwork` for isolated portal authentication.
- Deduplicated notification system (60s suppression window).
- Modern Jetpack Compose Material 3 user interface with live status cards and pulse animations.
- Persistent event and network history logging backed by Room database.
- WorkManager and BootReceiver fallback mechanism for Doze mode and device restart.
- Local JVM and Robolectric unit tests for core detection and notification logic.
