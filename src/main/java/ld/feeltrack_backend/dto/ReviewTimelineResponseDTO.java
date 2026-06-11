package ld.feeltrack_backend.dto;

import java.util.List;

import ld.feeltrack_backend.analytics.Granularity;

public record ReviewTimelineResponseDTO(
    Granularity granularity,
    List<ReviewTimelineItemDTO> data
) {}
