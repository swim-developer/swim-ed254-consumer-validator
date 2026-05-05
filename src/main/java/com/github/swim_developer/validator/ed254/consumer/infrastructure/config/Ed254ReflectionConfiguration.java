package com.github.swim_developer.validator.ed254.consumer.infrastructure.config;

import com.github.swim_developer.validator.ed254.consumer.domain.model.EventFileMetadata;
import com.github.swim_developer.validator.ed254.consumer.domain.model.FilterOptions;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(targets = {FilterOptions.class, EventFileMetadata.class})
@TemplateData(target = FilterOptions.class)
@TemplateData(target = EventFileMetadata.class)
public class Ed254ReflectionConfiguration {
}
