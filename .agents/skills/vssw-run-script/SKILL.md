---
name: vssw:run-script
description: >
  Use this skill whenever the user asks to set up the project, install dependencies, start the development server, or build the project.
---

# Run Script Skill

This project uses a standardized `./run` shell script to abstract away underlying tooling (like pip, npm, or mkdocs).

When a user asks you to perform a project-level task, **do not** run underlying commands directly (e.g., do not run `pip install` or `mkdocs serve`). Instead, you must use the `./run` script.

## Available Commands:

1. **Setup / Install Dependencies:**
   If the user asks to set up the project or install dependencies, execute:
   `./run setup`

2. **Start Development Server:**
   If the user asks to start the dev server, serve the project, or preview changes, execute:
   `./run serve`

3. **Build the Project:**
   If the user asks to build the project for production, execute:
   `./run build`

**Rule:** Always ensure the `./run` script is executed from the root directory of the project.
