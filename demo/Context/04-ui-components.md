# memInfo — UI Components

## Design Direction

Create a clean enterprise personal knowledge/RAG interface.

Website name:

**memInfo**

Product descriptor:

**Enterprise Personal RAG System**

Use Bootstrap as the primary UI system and existing CSS for customization.

Use HTMX for server-driven interactions instead of introducing a separate frontend framework.

## Global Layout

```text
┌──────────────────────────────────────────────────────────────┐
│ memInfo        Home   About   Chat                           │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│                       Page Content                           │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│ Footer                                                       │
└──────────────────────────────────────────────────────────────┘
```

## Navigation

Navbar links:

```text
Home
About
Chat
```

The active page should be visually identifiable.

Use responsive Bootstrap navigation.

## Home Hero

Center the main content vertically and horizontally.

Display:

```text
memInfo

Enterprise Personal RAG System

Upload your private PDF knowledge and ask questions
using retrieval-augmented generation.
```

Primary action:

```text
Open Chat
```

Secondary action:

```text
Learn More
```

Keep the home page simple and professional.

## Chat Layout

Desktop:

```text
┌──────────────┬───────────────────────────────────────────────┐
│              │                                               │
│ CHAT HISTORY │              CURRENT CHAT                     │
│              │                                               │
│ + New Chat   │                                               │
│              │                                               │
│ Session 1    │          Assistant messages                   │
│ Session 2    │          User messages                        │
│ Session 3    │                                               │
│              │                                               │
│              │                                               │
│ Source Upload│                                               │
│              │                                               │
├──────────────┴───────────────────────────────────────────────┤
│                         Message input                        │
└───────────────────────────────────────────────────────────────┘
```

## Sidebar

Sidebar sections:

### New Chat

Button:

```text
+ New Chat
```

Behavior:

- create a new chat session
- clear/open the current conversation
- update active session
- focus the message input

### Chat History

Display previous sessions.

Each item should contain:

```text
Session title
Edit
Delete
```

Edit:

- allow renaming
- save through HTMX
- update only the sidebar item

Delete:

- ask for confirmation
- delete the session and messages
- refresh history

### Source Upload

Button:

```text
Source Upload
```

Clicking it opens the device PDF picker.

After selection:

```text
Uploading...
Processing...
Embedding...
Indexing...
Indexed successfully
```

Use an HTMX-compatible upload flow.

## Chat Area

Components:

- chat header
- message list
- user message bubble
- assistant message bubble
- loading indicator
- source/citation section
- input field
- send button

Assistant response should appear without requiring a full page reload.

## Loading State

When Gemini/retrieval is processing:

```text
Searching your knowledge...
Generating answer...
```

Use a subtle Bootstrap spinner.

## Empty Chat State

When no messages exist:

```text
What would you like to know?

Ask a question about your uploaded documents.
```

Optional example questions can be shown.

## Source Panel

For answers with source metadata:

```text
Sources
────────────────────
📄 document.pdf
   Page 12

📄 policy.pdf
   Page 4
```

## Responsive Behavior

Desktop:

- persistent left sidebar

Tablet:

- narrower sidebar

Mobile:

- collapsible/offcanvas sidebar
- chat takes full width

Use Bootstrap responsive utilities instead of creating unnecessary custom breakpoint systems.

## Visual Style

Use:

- clean enterprise interface
- rounded Bootstrap cards
- subtle borders
- restrained shadows
- readable typography
- consistent spacing
- accessible button states
- clear loading/error/success states

Avoid:

- excessive gradients
- excessive animations
- unnecessary dashboards
- complicated agent visualizations
- excessive neon effects
