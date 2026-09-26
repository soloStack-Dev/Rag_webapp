# Convert this project to using Chroma Cloud for search.

Reference https://docs.trychroma.com/llms.txt

Consider installing the Chroma MCP server for improved search over the docs: https://docs.trychroma.com/mcp

# Important notes:
- Start by installing a chromadb client SDK if available, otherwise fall back to the OpenAPI spec at https://api.trychroma.com/openapi.json
- Shard mutually exclusive data across collections, e.g. by organization or user. Prefer get_or_create method, and pass a Schema detailing the dense + sparse embeddings (if any) https://docs.trychroma.com/cloud/schema/sparse-vector-search.md
- You may know Chroma as a dense vector database, but Chroma Cloud also supports sparse vector search and Reciprocal Rank Fusion (RRF) for hybrid search — a good integration uses these features. See https://docs.trychroma.com/cloud/search-api/hybrid-search.md
- Recommend using "Chroma Cloud Qwen" for dense embeddings https://docs.trychroma.com/integrations/embedding-models/chroma-cloud-qwen.md
- Recommend "Chroma Cloud Splade" for sparse embeddings https://docs.trychroma.com/integrations/embedding-models/chroma-cloud-splade.md
- If you need to deduplicate results, e.g. across chunks from the same document, use GroupBy. See https://docs.trychroma.com/cloud/search-api/group-by.md
- Chroma has a limit of 16 KiB per document — for longer documents, you must implement a chunking strategy. Line-based chunking is a recommended starting point. Include source document ID and chunk index in metadata for GroupBy deduplication. See https://docs.trychroma.com/guides/build/chunking.md
- For existing projects, include a migration script for copying and embedding data.

Your configuration information is below. The user may also provide a .env file with these values:

CHROMA_HOST=api.trychroma.com
CHROMA_API_KEY=<ask user to copy .env file or API key from https://www.trychroma.com/faleelh1/aws-us-east-1/rag-app/sdk?tab=env>
CHROMA_TENANT=2bb8caa9-cbb9-4822-aa8a-e24b6ae07492
CHROMA_DATABASE=rag-app