package com.petplace.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRecentView is a Querydsl query type for RecentView
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRecentView extends EntityPathBase<RecentView> {

    private static final long serialVersionUID = 1293886994L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRecentView recentView = new QRecentView("recentView");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QRestaurant restaurant;

    public final QUser user;

    public final DateTimePath<java.time.LocalDateTime> viewedAt = createDateTime("viewedAt", java.time.LocalDateTime.class);

    public QRecentView(String variable) {
        this(RecentView.class, forVariable(variable), INITS);
    }

    public QRecentView(Path<? extends RecentView> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRecentView(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRecentView(PathMetadata metadata, PathInits inits) {
        this(RecentView.class, metadata, inits);
    }

    public QRecentView(Class<? extends RecentView> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.restaurant = inits.isInitialized("restaurant") ? new QRestaurant(forProperty("restaurant"), inits.get("restaurant")) : null;
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user")) : null;
    }

}

