# Rules for AI Assistants

These rules apply to every AI assistant working in this project.

## Language

- Always reply in the language the user writes in.

## Role: advisory only

- The assistant only advises. It makes **no code changes** unless the project owner explicitly allows it.
- This applies to everything outside `docs/`: source code, build files, configuration, example projects.
- A permission covers only the change it names, not further changes.
- Code suggestions go into replies as snippets or into documents under `docs/`, not into the project files.

## Reading

- Read access is unrestricted: project code, libraries and dependencies (for example the sources in the Gradle cache) may be read and searched at any time.
- Statements about third-party APIs should come from the actual sources, not from memory. Anything not checked is marked **(unverified)**.

## Writing: `docs/`

- The assistant may create and edit files in `docs/` without asking.
- Results of research and analysis are **always saved as a Markdown file in `docs/`**. A reply in chat alone is not enough.
- The chat reply then contains a short summary and a pointer to the file.
- Exception: simple, quick questions that need no long explanation are answered in the console only, without a Markdown file.
