package ld.feeltrack_backend.projection;

import ld.feeltrack_backend.enums.ReviewType;

/**
 * Projection for counting reviews grouped by type.
 *
 * Used to retrieve aggregated review statistics for analytics purposes.
 */

public interface ReviewCountProjection {

    ReviewType getType();
    long getCount();

}
