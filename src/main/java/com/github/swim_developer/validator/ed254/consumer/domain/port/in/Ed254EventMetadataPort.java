package com.github.swim_developer.validator.ed254.consumer.domain.port.in;

import com.github.swim_developer.validator.ed254.consumer.domain.model.EventFileMetadata;
import com.github.swim_developer.validator.ed254.consumer.domain.model.FilterOptions;

import java.util.List;

public interface Ed254EventMetadataPort {
    List<EventFileMetadata> getAllEventMetadata();
    FilterOptions getFilterOptions();
}
