# memInfo — Page UI Specification

## 1. Home Page

Route:

```text
/
```

### Purpose

Introduce memInfo as an enterprise personal RAG system.

### Structure

```text
Navbar
  ↓
Centered Hero
  ↓
memInfo
Enterprise Personal RAG System
Short product description
  ↓
Open Chat / Learn More
  ↓
Simple feature section
  ↓
Footer
```

### Hero Text

```text
memInfo

Enterprise Personal RAG System

Turn your PDF knowledge into a searchable private knowledge base.
Upload documents, retrieve relevant information, and ask questions
through an AI-powered chat interface.
```

### Feature Cards

Use three simple Bootstrap cards:

```text
PDF Knowledge
Upload and process PDF documents.

Vector Search
Store document knowledge as embeddings in Chroma.

Grounded AI
Ask questions and receive answers based on retrieved document context.
```

## 2. About Page

Route:

```text
/about
```

### Purpose

Explain what memInfo does and how the system works.

### Structure

```text
Navbar
  ↓
About Hero
  ↓
What is memInfo?
  ↓
How RAG Works
  ↓
Technology Overview
  ↓
Privacy / Security Principles
  ↓
Footer
```

### What is memInfo?

Explain:

```text
memInfo is a personal enterprise RAG application that converts
uploaded PDF knowledge into searchable vector representations.
Users can then ask questions and receive answers grounded in
the retrieved document content.
```

### RAG Explanation

Display:

```text
PDF
 ↓
Text Extraction
 ↓
Chunking
 ↓
Embeddings
 ↓
Chroma
 ↓
Similarity Retrieval
 ↓
Gemini
 ↓
Grounded Answer
```

### Technology Cards

Show only technologies actually present in the project.

At minimum, based on the current project description:

```text
Spring Boot
HTMX
Bootstrap
MySQL
Chroma
Gemini AI
```

Before displaying additional technology names, inspect `pom.xml` and the existing project configuration.

## 3. Chat Page

Route:

```text
/chat
```

### Main Structure

```text
┌────────────────────────────────────────────────────────────┐
│ Navbar                                                     │
├──────────────┬─────────────────────────────────────────────┤
│ Chat Sidebar │ Chat Header                                │
│              ├─────────────────────────────────────────────┤
│ + New Chat   │                                             │
│              │              Messages                       │
│ History      │                                             │
│              │                                             │
│ Edit Delete  │                                             │
│              │                                             │
│ Source       │                                             │
│ Upload       │                                             │
│              ├─────────────────────────────────────────────┤
│              │ Message input                         Send  │
└──────────────┴─────────────────────────────────────────────┘
```

### New Chat Behavior

```text
Click New Chat
→ create session
→ clear message area
→ set new session as active
→ focus input
```

### Existing Session Behavior

```text
Click history item
→ load messages
→ mark session active
→ show conversation
```

### Edit Behavior

```text
Click Edit
→ inline title input
→ Save
→ HTMX update
→ refresh title
```

### Delete Behavior

```text
Click Delete
→ confirmation
→ delete session/messages
→ refresh history
→ open another session or empty state
```

### Source Upload Behavior

```text
Click Source Upload
→ device PDF picker
→ upload PDF
→ processing status
→ text extraction
→ chunking
→ Gemini embedding
→ Chroma indexing
→ success/error status
```

### Chat Message Behavior

```text
User types question
→ Send
→ show user message
→ show loading indicator
→ retrieve relevant Chroma chunks
→ Gemini generates grounded response
→ show assistant response
→ show sources
→ save conversation
```

## HTMX UI Principle

Prefer targeted partial updates.

Examples:

```text
POST /chat/message
→ replace assistant response area

POST /chat/new
→ replace chat workspace

GET /chat/history
→ replace history list

POST /chat/history/{id}/rename
→ replace one history item

DELETE /chat/history/{id}
→ remove history item

POST /documents/upload
→ replace upload status
```

Use the actual existing controller routes if they already exist. Do not duplicate endpoints unnecessarily.

## Final UI Goal

The finished application should feel like a simple enterprise knowledge assistant:

```text
Home → explains memInfo
About → explains the system
Chat → performs RAG
Sidebar → manages sessions and PDF sources
MySQL → stores application/chat data
Chroma → stores vector knowledge
Gemini → embeds and generates
HTMX → updates UI without a SPA
Bootstrap → responsive visual system
```
