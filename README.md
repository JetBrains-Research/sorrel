# License Management Plugin

![Build](https://github.com/DmitryPogrebnoy/license-compatibility-plugin/workflows/Build/badge.svg)

<!-- Plugin description -->
A plugin for IntelliJ IDEA for license management and incompatibility detection.

## Features

- [x] Detect the licenses of the project's dependencies and visualize them by a module.
- [x] Show inlay hints with license names for dependency add expressions in project build scripts.
- [x] Suggest possible license for the project/module based on its dependencies.
- [x] Detect possible incompatibilities between the licenses in the project and warn the developer about them.
- [x] Provide short and understandable descriptions for existing licenses.

<!-- Plugin description end -->

## Installation

The plugin is currently in development, so the first version is coming on the Marketplace.

You can install the plugin in IDEA as follows:

1. Run the gradle command `build`. jar-archive of the plugin will be in the `build/libs` directory.
2. To install the plugin you need to go to `File` - `Settings` - `Plugins` - ⚙️ - `Install from disk` - Pick plugin jar.

If you don't want to install the plugin in IDEA, you can open this project in IDEA and run the gradle command `runIde`
to test the plugin. A plugin tool window will appear on the right called `Project licenses`.

## Contacts

If you have any questions or suggestions about the plugin, feel free to contact Yaroslav Golubev in Telegram (@areyde).
