# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Herd Manager** is a JetBrains IntelliJ/PhpStorm plugin (Kotlin) that integrates Laravel Herd site management directly into the IDE. It auto-detects Herd installation, manages `herd.yml` project config files, and provides link/unlink/secure operations via the Herd CLI.

- **Plugin ID**: `com.jeffersongoncalves.herdmanager`
- **Target IDE**: PhpStorm 2024.3+ (build 243–263.*)
- **Language**: Kotlin (JVM 17)
- **Build System**: Gradle 8.13 with IntelliJ Platform Gradle Plugin 2.6.0
- **Dependencies**: SnakeYAML 2.3 (YAML parsing), JUnit 4.13.2 (tests)

## Build & Development Commands

```bash
# Build the plugin (output in build/distributions/)
./gradlew buildPlugin

# Run PhpStorm sandbox with plugin loaded
./gradlew runIde

# Run tests
./gradlew test

# Verify plugin compatibility
./gradlew verifyPlugin

# Clean build artifacts
./gradlew clean
```

## Architecture

Three application/project-level services connected via IntelliJ message bus:

### Services (core layer)
- **HerdDetectorService** (app-level): Platform-aware detection of Herd installation (Windows/macOS/Linux paths), reads installed PHP versions from `~/.config/herd/config/php.json`, TLD from valet config, and checks site link status via symlinks in `Sites/` directory.
- **HerdCliService** (app-level): Async execution of `herd link|unlink|secure|unsecure|open` commands via `CapturingProcessHandler` with 30s timeout. Shows balloon notifications for results.
- **HerdConfigService** (project-level): Manages `herd.yml` lifecycle — load/save/watch. Subscribes to VFS changes for reactive auto-reload. Publishes `CONFIG_CHANGED` topic so all UI components stay in sync.

### Flow
1. **Project open** → `HerdProjectOpenListener` checks Herd installed + config exists + site linked → shows notification with quick actions if needed
2. **UI interaction** → Tool window panel (right sidebar) or status bar widget → user edits config → saves `herd.yml` → links site via CLI
3. **Reactive updates** → Any `herd.yml` change (internal or external) triggers VFS listener → `CONFIG_CHANGED` → all UI refreshes

### Key packages under `com.jeffersongoncalves.herdmanager`
| Package | Purpose |
|---------|---------|
| `model` | `HerdConfig` data class — YAML parse/serialize, default generation |
| `service` | Detection, CLI execution, config state management |
| `ui` | Tool window panel (config form) + status bar widget |
| `actions` | IDE actions: Configure, Link, Open in Browser (Tools menu) |
| `listeners` | Post-startup activity for auto-detection |
| `icons` | SVG icon constants (herd, linked, unlinked) |

### Plugin registration
All services, extensions, and actions are declared in `src/main/resources/META-INF/plugin.xml`. i18n strings are in `src/main/resources/messages/HerdManagerBundle.properties`.

## Testing

Tests are in `src/test/kotlin/` using JUnit 4. Currently covers the model layer (`HerdConfigTest`) — YAML roundtrip, parsing edge cases, default config generation.

## Key Config Properties (gradle.properties)

| Property | Value | Purpose |
|----------|-------|---------|
| `platformVersion` | 2024.3 | PhpStorm version for dev/test |
| `pluginSinceBuild` | 243 | Minimum IDE build |
| `pluginUntilBuild` | 263.* | Maximum IDE build |
