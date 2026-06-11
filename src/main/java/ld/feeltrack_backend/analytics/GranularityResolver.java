package ld.feeltrack_backend.analytics;

public class GranularityResolver {

    public static Granularity resolve(int days) {

        if (days <= 30) return Granularity.DAY;
        if (days <= 120) return Granularity.WEEK;

        return Granularity.MONTH;
    }

}
