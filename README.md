# Sorrel

[![JB Research](https://jb.gg/badges/research-flat-square.svg)](https://research.jetbrains.org/)
![Build](https://github.com/JetBrains-Research/sorrel/workflows/Build/badge.svg)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/JetBrains-Research/sorrel/blob/main/LICENSE)

<!-- Plugin description -->
A plugin for IntelliJ IDEA for managing licenses and detecting license incompatibilities. **Sorrel** allows to work with the
licenses of Java projects right inside IntelliJ IDEA.

## Features

The plugin can:

- Detect and recognize the licenses of project modules.
- Detect and recognize the licenses of project libraries.
- Detect incompatibilities between licenses in the project.
- Suggest a project license that will be compatible with all the project's libraries.
- Visualize all the licensing information inside the IDE and provide convenient features for managing licenses.

<!-- Plugin description end -->

You can learn more in the [demonstration video](http://www.youtube.com/watch?v=doUeAwPjcPE).

## Installation

The plugin is currently in development, so the first version will be coming soon to the Marketplace.

You can install the plugin in IntelliJ IDEA as follows:

1. Download
   the [latest build](https://github.com/JetBrains-Research/sorrel/releases/download/v1.0-eap/Sorrel-1.0-eap.zip) of the
   **Sorrel** plugin.
2. In the IDE, go to `File` - `Settings` - `Plugins` - ⚙️ - `Install from disk`, and select the
   plugin zip.
3. Restart the IDE.

**Sorrel** requires IntelliJ IDEA in the version range from 2020.2 to 2021.3.1.

## Supported Licenses

**Sorrel** currently supports 16 most popular open-source licenses. A trained machine learning model (ML) and the
Sørensen-Dice coefficient (DSC)
are used to detect 12 licenses. The remaining four licenses are detected using only the Sørensen-Dice coefficient.
Supported licenses and detection methods are showed in the table below.

| License                                                  | Detecting method |
| :------------------------------------------------------- | :--------------: |
| GNU Affero General Public License v3.0 only              | ML + DSC         |
| Apache License 2.0                                       | ML + DSC         |
| BSD 2-Clause "Simplified" License                        | ML + DSC         |
| BSD 3-Clause "New" or "Revised" License                  | ML + DSC         |
| Common Development and Distribution License 1.0          | DSC              |
| Eclipse Public License 1.0                               | DSC              |
| GNU General Public License v2.0 only                     | ML + DSC         |
| GNU General Public License v2.0 with classpath exception | DSC              |
| GNU General Public License v3.0 only                     | ML + DSC         |
| ISC License                                              | ML + DSC         |
| GNU Lesser General Public License v2.1 only              | ML + DSC         |
| GNU Lesser General Public License v3.0 only              | ML + DSC         |
| MIT License                                              | ML + DSC         |
| Mozilla Public License 1.1                               | DSC              |
| Mozilla Public License 2.0                               | ML + DSC         |
| Do What The F*** You Want To Public License              | ML + DSC         |

## How to Use

### Tool Window

**Sorrel** provides the user with a graphical interface for the convenience of managing licenses. The main graphical
interface of the plugin is the *Tool Window*. The window contains two tabs and provides the information about all the
licenses in the project. The first tab is called *Project License* and is presented below. It contains the information
about the detected main license of the project (root module), its description (permissions, limitations, and conditions), 
as well as a list of detected potential license violations. From this tab, the user also can export a JSON report with
all the data.

![Project License Window](https://github.com/JetBrains-Research/sorrel/raw/main/docs/pictures/ProjectLicenseWindow.png)

The second tab is called *Package Licenses* and is presented below. This tab contains the information about all the
licenses of libraries used inside project, it supports a search among all the libraries and filtering by modules.

![Package Licenses Window](https://github.com/JetBrains-Research/sorrel/raw/main/docs/pictures/PackageLicensesWindow.png)

### License Editor Notification

In addition to the Tool Window, the plugin provides the *License Editor Notification* panel, presented below. This panel
appears at the top of the editor when the module license is opened in it. The panel allows the user to change the
license in several simple clicks using the drop-down menu, while indicating which licenses are compatible with all the
licenses of the module's libraries. The panel also provides an opportunity to compare the current text of the license
file with the original text of the license to check for possible differences.

![License Editor Notification](https://github.com/JetBrains-Research/sorrel/raw/main/docs/pictures/LicenseEditorNotification.png)

### New Module License File Action

Also, the plugin adds a new item into the IDE's *Create new file...* menu, called *Module License File*. If the project
has no license, the user can use this functionality, and the plugin will detect the licenses compatible with all the
project's libraries, and suggest the most permissive one to the user. This way, even the most inexperienced user can
manage their licenses and not make mistakes. In the future, the user can always change their license using the *License
Editor Notification* panel described above.

![New Module License File Action](https://github.com/JetBrains-Research/sorrel/raw/main/docs/pictures/NewModuleLicenseFileAction.png)

### Inlay License Hints

Finally, the developed plugin provides another convenient way of viewing the licenses of libraries by providing hints
right in the build system script. A hint with the name of the license of a given library appears next to each command
that connects the library, as shown below. These hints allow the user to keep the licenses in mind when adding new
libraries directly in the editor. The hints are implemented for Maven, Groovy Gradle, and Kotlin Gradle scripts.

![Inlay License Hints](https://github.com/JetBrains-Research/sorrel/raw/main/docs/gif/InlayLicenseHints.gif)

## License

This project is licensed under the Apache-2.0 License. The full text of the license can be found in
the [license file](https://github.com/JetBrains-Research/sorrel/blob/main/LICENSE).

## Acknowledgments

**Sorrel** was developed as a part of a research project in the Laboratory of [Machine Learning Methods in Software Engineering](https://research.jetbrains.org/groups/ml_methods/) at JetBrains Research.

## Contacts

If you have any questions or suggestions about the plugin, feel free to contact Yaroslav Golubev in Telegram (@areyde).
