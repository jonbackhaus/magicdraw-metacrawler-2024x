# MagicDraw Metacrawler

A MagicDraw 2024x plugin that adds interactive metachain crawling to the containment browser. Right-click any element to explore its metamodel properties and navigate to target elements — no dialogs, no searching, just right-click and go.

## What It Does

When you right-click an element in MagicDraw's containment browser, Metacrawler adds a context menu that shows:

- All metamodel properties of the selected element (attributes and references discovered via MOF reflection)
- The target elements for each property, labeled with their representation text and type
- One-click navigation: selecting a target element opens it in the containment tree

This lets you interactively walk through metachains without needing to know the metamodel structure ahead of time.

## Requirements

- MagicDraw 2024x (tested with 2024xR3)
- Java 11
- Maven

## Building

```bash
# Build the plugin JAR
mvn clean package

# Build the full distribution bundle
bash build_dist.sh
```

The distribution script builds the JAR, generates `plugin.xml` and a resource manager descriptor, and packages everything into `dist/metacrawler-plugin-v1.0.0.zip`.

> **Note:** All MagicDraw dependencies use `system` scope and resolve from a local installation. The default path is `/Applications/MagicDraw 2024xR3`. To use a different installation, update `md.path` in `pom.xml`.

## Installation

Unzip the distribution bundle into your MagicDraw installation directory:

```bash
unzip dist/metacrawler-plugin-v1.0.0.zip -d "/Applications/MagicDraw 2024xR3"
```

Restart MagicDraw. The plugin loads automatically on startup.

## Usage

1. Open a project in MagicDraw
2. Right-click any element in the containment browser
3. Expand the **Metacrawler** submenu
4. Browse properties and their target elements
5. Click a target element to navigate to it in the containment tree

Metamodel properties are cached after first discovery, so subsequent right-clicks on elements of the same type are fast.

## Architecture

Four classes in `com.jonbackhaus.metacrawler`:

| Class | Role |
| ----- | ---- |
| `MetacrawlerPlugin` | Plugin entry point; registers the menu configurator on `init()` |
| `MetacrawlerMenuConfigurator` | Hooks into MagicDraw's browser context menu via `BrowserContextAMConfigurator` |
| `MetacrawlerService` | Core logic: discovers metamodel properties via MOF reflection, caches them, and builds menu items |
| `MetacrawlerAction` | Opens a target element in the containment tree when clicked |

## License

MIT
