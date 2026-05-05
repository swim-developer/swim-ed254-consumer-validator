package com.github.swim_developer.validator.ed254.consumer.domain.port.in;

import com.github.swim_developer.validator.core.domain.model.TopicDetails;
import com.github.swim_developer.validator.core.domain.model.TopicSummary;

import java.util.List;
import java.util.Optional;

public interface Ed254TopicPort {
    List<TopicSummary> getAllTopics();
    Optional<TopicDetails> getTopicDetails(String topicId);
}
