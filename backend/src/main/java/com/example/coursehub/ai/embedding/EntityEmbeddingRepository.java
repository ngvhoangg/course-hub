package com.example.coursehub.ai.embedding;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EntityEmbeddingRepository extends JpaRepository<EntityEmbedding, Long> {
    Optional<EntityEmbedding> findByEntityTypeAndEntityId(EntityType type, Long entityId);

    void deleteByEntityTypeAndEntityId(EntityType entityType, Long entityId);

    @Modifying
    @Transactional
    @Query(value = """
        UPDATE entity_embeddings 
        SET metadata = COALESCE(metadata, '{}'::jsonb) || CAST(:metadata AS jsonb), 
            updated_at = NOW() 
        WHERE entity_type = :type AND entity_id = :id
        """, nativeQuery = true)
    void patchMetadata(@Param("type") String type, @Param("id") Long id, @Param("metadata") String metadata);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO entity_embeddings (entity_type, entity_id, content, embedding, metadata, updated_at)
        VALUES (:type, :id, :content, CAST(:vector AS vector), CAST(:metadata AS jsonb), NOW())
        ON CONFLICT (entity_type, entity_id) 
        DO UPDATE SET 
            embedding = EXCLUDED.embedding, 
            content = EXCLUDED.content, 
            metadata = COALESCE(entity_embeddings.metadata, '{}'::jsonb) || EXCLUDED.metadata,
            updated_at = NOW()
        """, nativeQuery = true)
    void upsert(@Param("type") String type, @Param("id") Long id,
                    @Param("content") String content, @Param("vector") String vector,
                    @Param("metadata") String metadata);
}
