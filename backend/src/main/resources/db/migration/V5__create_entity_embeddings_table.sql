-- Create entity_embeddings table.
-- If the pgvector extension is available, use the vector type and HNSW index.
-- Otherwise (e.g. in local tests without pgvector), fall back to a simple schema
-- without the vector type so Flyway can still migrate successfully.

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_available_extensions WHERE name = 'vector') THEN
        -- pgvector is installed: enable the extension and use the vector type
        CREATE EXTENSION IF NOT EXISTS vector;

        CREATE TABLE IF NOT EXISTS entity_embeddings (
            id BIGSERIAL PRIMARY KEY,
            entity_type VARCHAR(50) NOT NULL, -- 'COURSE', 'LESSON',...
            entity_id BIGINT NOT NULL,
            content TEXT NOT NULL,            -- Content used for embedding
            embedding vector(384) NOT NULL,
            metadata JSONB,                   -- Store extra fields for fast filtering
            created_at TIMESTAMP DEFAULT NOW(),
            updated_at TIMESTAMP DEFAULT NOW(),

            UNIQUE(entity_type, entity_id)
        );

        CREATE INDEX IF NOT EXISTS idx_entity_embeddings_v
            ON entity_embeddings USING hnsw (embedding vector_cosine_ops);

        CREATE INDEX IF NOT EXISTS idx_entity_embeddings_meta
            ON entity_embeddings USING GIN (metadata);
    ELSE
        -- pgvector is not installed: create a compatible table without vector type
        CREATE TABLE IF NOT EXISTS entity_embeddings (
            id BIGSERIAL PRIMARY KEY,
            entity_type VARCHAR(50) NOT NULL, -- 'COURSE', 'LESSON',...
            entity_id BIGINT NOT NULL,
            content TEXT NOT NULL,            -- Content used for embedding
            embedding BYTEA,                  -- Fallback storage type when vector is unavailable
            metadata JSONB,                   -- Store extra fields for fast filtering
            created_at TIMESTAMP DEFAULT NOW(),
            updated_at TIMESTAMP DEFAULT NOW(),

            UNIQUE(entity_type, entity_id)
        );

        -- GIN index for metadata to filter by category/price without JOIN
        CREATE INDEX IF NOT EXISTS idx_entity_embeddings_meta
            ON entity_embeddings USING GIN (metadata);
    END IF;
END $$;