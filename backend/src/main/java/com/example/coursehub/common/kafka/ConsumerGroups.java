package com.example.coursehub.common.kafka;

public final class ConsumerGroups {
    private ConsumerGroups() {}

    public static final String EMAIL = "course-hub-email-group";
    public static final String PAYMENT = "course-hub-payment-group";
    public static final String EMBEDDING_SYNC_GROUP = "course-hub-embedding-sync-group";
}
