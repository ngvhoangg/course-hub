package com.example.coursehub.ai.embedding;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EntityEmbeddingRepository extends JpaRepository<EntityEmbedding, Long> {
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO entity_embeddings (entity_type, entity_id, content, embedding, metadata)
        VALUES (:type, :id, :content, CAST(:vector AS vector), CAST(:metadata AS jsonb))
        ON CONFLICT (entity_type, entity_id) DO UPDATE 
        SET embedding = EXCLUDED.embedding, content = EXCLUDED.content, metadata = EXCLUDED.metadata, updated_at = NOW()
        """, nativeQuery = true)
    void upsert(@Param("type") String type, @Param("id") Long id,
                @Param("content") String content, @Param("vector") String vector,
                @Param("metadata") String metadata);
}
