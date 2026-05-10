package com.petplace.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QLocalAuth is a Querydsl query type for LocalAuth
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLocalAuth extends EntityPathBase<LocalAuth> {

    private static final long serialVersionUID = 1041438465L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QLocalAuth localAuth = new QLocalAuth("localAuth");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath loginId = createString("loginId");

    public final StringPath password = createString("password");

    public final QUser user;

    public QLocalAuth(String variable) {
        this(LocalAuth.class, forVariable(variable), INITS);
    }

    public QLocalAuth(Path<? extends LocalAuth> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QLocalAuth(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QLocalAuth(PathMetadata metadata, PathInits inits) {
        this(LocalAuth.class, metadata, inits);
    }

    public QLocalAuth(Class<? extends LocalAuth> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user")) : null;
    }

}

