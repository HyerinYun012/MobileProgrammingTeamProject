package com.petplace.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRestaurant is a Querydsl query type for Restaurant
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRestaurant extends EntityPathBase<Restaurant> {

    private static final long serialVersionUID = -828694097L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRestaurant restaurant = new QRestaurant("restaurant");

    public final StringPath address = createString("address");

    public final BooleanPath allowLarge = createBoolean("allowLarge");

    public final BooleanPath allowMedium = createBoolean("allowMedium");

    public final BooleanPath allowSmall = createBoolean("allowSmall");

    public final StringPath businessNo = createString("businessNo");

    public final EnumPath<Restaurant.Category> category = createEnum("category", Restaurant.Category.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final BooleanPath hasArtificialGrass = createBoolean("hasArtificialGrass");

    public final BooleanPath hasFence = createBoolean("hasFence");

    public final BooleanPath hasIndoor = createBoolean("hasIndoor");

    public final BooleanPath hasNaturalGrass = createBoolean("hasNaturalGrass");

    public final BooleanPath hasOutdoor = createBoolean("hasOutdoor");

    public final BooleanPath hasParking = createBoolean("hasParking");

    public final BooleanPath hasRestroom = createBoolean("hasRestroom");

    public final BooleanPath hasSnack = createBoolean("hasSnack");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final ListPath<RestaurantImage, QRestaurantImage> images = this.<RestaurantImage, QRestaurantImage>createList("images", RestaurantImage.class, QRestaurantImage.class, PathInits.DIRECT2);

    public final BooleanPath isVerified = createBoolean("isVerified");

    public final NumberPath<java.math.BigDecimal> latitude = createNumber("latitude", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> longitude = createNumber("longitude", java.math.BigDecimal.class);

    public final StringPath name = createString("name");

    public final QUser owner;

    public final StringPath phone = createString("phone");

    public final EnumPath<Restaurant.Region> region = createEnum("region", Restaurant.Region.class);

    public final ListPath<Review, QReview> reviews = this.<Review, QReview>createList("reviews", Review.class, QReview.class, PathInits.DIRECT2);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QRestaurant(String variable) {
        this(Restaurant.class, forVariable(variable), INITS);
    }

    public QRestaurant(Path<? extends Restaurant> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRestaurant(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRestaurant(PathMetadata metadata, PathInits inits) {
        this(Restaurant.class, metadata, inits);
    }

    public QRestaurant(Class<? extends Restaurant> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.owner = inits.isInitialized("owner") ? new QUser(forProperty("owner")) : null;
    }

}

