# License Management Plugin

![Build](https://github.com/DmitryPogrebnoy/license-compatibility-plugin/workflows/Build/badge.svg)

A plugin for IntelliJ IDEA for license management and incompatibility detection.
<!-- Plugin description -->
<!-- Plugin description end -->

## Installation

The plugin is currently in early development, so the first build is coming. Currently, you can open the project in IDEA and run the gradle command `runIde` to test the plugin. A tool window will appear on the right called `Project licenses`, where you can find a list of all of your licenses by module. 

## Features

- [x] Detect the licenses of the project's dependencies and visualize them by module.
- [x] Show inlay hints with license names for dependency add expressions in project build scripts.
- [x] Suggest possible license for the project based on its dependencies.
- [ ] Detect possible incompatibilities between the licenses in the project and warn the developer about them.
- [ ] Provide short and understandable descriptions for existing licenses.
- [ ] Support the detection of the licenses in the files of the project and detect incompatibilities in them also.

## Contacts

If you have any questions or suggestions about the plugin, feel free to contact Yaroslav Golubev in Telegram (@areyde).
