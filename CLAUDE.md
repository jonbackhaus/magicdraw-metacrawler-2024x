# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MagicDraw Metacrawler is a Java plugin for MagicDraw 2024x that adds a context menu for metachain crawling in the containment browser. When users right-click an element, the plugin shows all metamodel properties and their target elements, allowing navigation through the model.

## Build Commands

```bash
# Build the plugin JAR
mvn clean package

# Build distribution bundle (JAR + plugin.xml + resource descriptor ZIP)
bash build_dist.sh
```

Output:

- JAR: `target/magicdraw-metacrawler-1.0.0.jar`
- Distribution: `dist/metacrawler-plugin-v1.0.0.zip`

## Architecture

Four classes in `src/main/java/com/jonbackhaus/metacrawler/`:

1. **MetacrawlerPlugin** - Plugin entry point; registers the menu configurator on `init()`
2. **MetacrawlerMenuConfigurator** - Implements `BrowserContextAMConfigurator`; hooks into MagicDraw's browser context menu
3. **MetacrawlerService** - Core logic: discovers metamodel properties via MOF reflection, caches them in a `ConcurrentHashMap`, and populates menu items
4. **MetacrawlerAction** - Extends `MDAction`; opens the selected element in the containment tree

Data flow: User right-clicks → `MetacrawlerMenuConfigurator.configure()` → `MetacrawlerService.populatePropertyMenu()` → Creates property submenus with target element actions

## Critical Implementation Detail

MagicDraw's JMI collections are "live" and can change in background threads. Always use `.toArray()` to snapshot collections before iteration to avoid `ConcurrentModificationException`. See `MetacrawlerService.java:119-123`.

## Dependencies

All MagicDraw dependencies use system scope and resolve from the local MagicDraw installation. The default path is `/Applications/MagicDraw 2024xR3`. To use a different installation, update `md.path` in `pom.xml`.

## Requirements

- Java 11
- Maven
- MagicDraw 2024x installation (for compile-time dependencies)
