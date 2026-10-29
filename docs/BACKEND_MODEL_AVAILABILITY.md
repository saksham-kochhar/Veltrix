# OpenRouter live model catalog (backend)

The Android app loads `/v1/models` and **only lists models that are marked live**. Each entry should reflect OpenRouter’s current catalog and uptime, not a static PRD list.

## Required JSON fields

For each object in `models[]`:

| Field | Type | Purpose |
|-------|------|---------|
| `id` | string | OpenRouter model id (e.g. `dots-studio/dots-3-note-preview:free`) |
| `display_name` | string | Human-readable name |
| `allowed` | boolean | User’s plan includes this model |
| **`available`** | boolean | **Model is currently listed on OpenRouter** (wire id or route alias) |
| **`allowed`** | boolean | User’s plan includes this model (show locked in UI when false) |

The app shows **all** catalog models: locked when `allowed` is false, greyed when `available` is false. Chat auto-selects only models that are both allowed and available.

## Chat errors

Return HTTP 503 with:

```json
{
  "detail": {
    "code": "model_unavailable",
    "detail": "The selected model (…) is unavailable right now. Choose another model in the app."
  }
}
```

The app hides that model for 30 minutes and re-fetches `/v1/models` when the user opens the model picker.

## System prompt

The app sends per-request instructions that name the **real model and provider**. The backend should **not** prepend a global “You are Cherry AI” system message that overrides model identity.
