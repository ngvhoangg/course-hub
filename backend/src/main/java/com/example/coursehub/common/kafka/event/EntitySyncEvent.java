package com.example.coursehub.common.kafka.event;

import com.example.coursehub.ai.embedding.EntityType;

import java.util.Map;

public record EntitySyncEvent (
    Long entityId,
    EntityType entityType,
    EntityAction action,
    Map<String, Object> metadata,
    boolean reEmbed,
    Long occurredAt
) {}
