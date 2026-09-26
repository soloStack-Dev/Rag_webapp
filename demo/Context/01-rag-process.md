# memInfo — RAG Process Specification

## Purpose

Build a simple enterprise personal RAG system named **memInfo**.

The application stores knowledge extracted from uploaded PDF documents in a Chroma vector database. When a user asks a question, the system retrieves relevant document chunks and uses Gemini AI to generate a grounded answer.

## Core RAG Flow

```text
PDF Upload
   ↓
PDF Parsing / Text Extraction
   ↓
Text Cleaning
   ↓
Document Chunking
   ↓
Gemini Embedding Model
   ↓
Vector + Metadata
   ↓
Chroma Vector Database
```

User query flow:

```text
User Question
   ↓
Query Understanding
   ↓
Query Embedding
   ↓
Similarity Search in Chroma
   ↓
Top Relevant Chunks
   ↓
Context Assembly
   ↓
Gemini Chat Model
   ↓
Grounded Answer
   ↓
Chat UI
```

## PDF Ingestion

When the user clicks **Source Upload**:

1. Open the device file picker.
2. Accept PDF files.
3. Validate the file type and reasonable upload size.
4. Parse the PDF text on the server.
5. Clean unnecessary whitespace and extraction artifacts.
6. Preserve useful document metadata such as:
   - original filename
   - page number when available
   - upload timestamp
   - document identifier
7. Split the extracted text into overlapping chunks.
8. Generate embeddings for each chunk with the configured Gemini embedding model.
9. Store embeddings, chunk text, and metadata in Chroma.
10. Store document/upload metadata in MySQL when required by the existing schema.
11. Return upload/indexing status to the UI.

## Query / Retrieval

When the user submits a question:

1. Receive the question through the existing Spring Boot endpoint.
2. Normalize the query without changing its meaning.
3. Generate a query embedding using the same embedding model used for indexed documents.
4. Search Chroma using vector similarity.
5. Retrieve only the most relevant chunks.
6. Optionally apply metadata filtering when a source/document is selected.
7. Build a compact context block from the retrieved chunks.
8. Send the user question + retrieved context to Gemini Chat.
9. Instruct Gemini to answer only from the supplied context.
10. If the context does not contain enough information, return a clear "I couldn't find this information in the uploaded sources" response instead of inventing facts.
11. Display the answer in the current chat session.

## Chunking Rules

Start with practical configurable values rather than hard-coding them:

- chunk size: configurable
- chunk overlap: configurable
- top-K retrieval: configurable

Keep chunk boundaries as meaningful as possible. Prefer paragraph/section boundaries before arbitrary character cuts.

## Retrieval Quality

The implementation should:

- use the same embedding space for documents and queries
- retrieve top-K relevant chunks
- avoid sending unnecessary chunks to Gemini
- preserve source metadata
- optionally expose source filename/page information in the answer UI
- avoid duplicate chunks where possible
- handle empty/no-result searches gracefully

## Grounding Rule

The Gemini response must be grounded in retrieved context.

System behavior:

```text
If the retrieved context supports the answer:
    answer clearly and concisely.

If the retrieved context does not support the answer:
    state that the uploaded knowledge base does not contain enough information.
    Do not fabricate an answer.
```

## Chat Sessions

A chat session contains:

- session ID
- title
- messages
- created timestamp
- updated timestamp

Behavior:

```text
New Chat
   ↓
Create new session
   ↓
Open empty chat panel
   ↓
User messages belong to this session
```

Previous sessions must remain available in the sidebar.

Each previous session provides:

- Edit/Rename
- Delete

Deleting a chat must remove the conversation from MySQL and refresh the history list.

Renaming a chat must update the stored session title and sidebar label.

## Important Separation

Do not mix the following responsibilities:

```text
MySQL
→ users/session/chat metadata/application records

Chroma
→ document chunks + embeddings + retrieval metadata

Gemini
→ embeddings + language generation
```

Use the actual existing project architecture and `pom.xml` dependencies as the source of truth before adding new libraries.
