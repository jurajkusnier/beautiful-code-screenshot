## Project Overview
An IntelliJ plugin that generates beautiful, shareable images of source code selections. Inspired by tools like Carbon, it renders selected code inside a styled window (macOS-like) with rounded corners, shadow, and window controls.

## Goals
- Export selected code as a visually appealing image
- Support transparent background
- Use IDE syntax highlighting
- Mimic macOS window styling (red/yellow/green buttons)
- Work inside IntelliJ IDEA and Android Studio

## Core Features
- Render **selected editor content** to an image
- Respect **IDE color scheme**
- Add **window chrome** (top bar + control buttons)
- Rounded corners + drop shadow
- Copy image to clipboard or save to file

## Tech Stack
- Kotlin
- IntelliJ Platform SDK
- Gradle IntelliJ Plugin

## Future Ideas
- Theme presets (Dracula, Nord, etc.)
- Adjustable padding and font size
- Export as SVG
- Copy to clipboard
- Quick share (Twitter / LinkedIn)

## Development Notes
- Only render **selected code**
- Ensure **no selection highlight** in output
- Maintain **pixel-perfect rendering**
- Optimize for fast generation