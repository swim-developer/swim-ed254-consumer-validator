package com.github.swim_developer.validator.ed254.consumer.domain.model;


import java.util.Set;

public class EventFileMetadata {

    private final String filename;
    private final String messageType;
    private final String aerodromeDesignator;
    private final int sequenceEntriesCount;
    private final Set<String> callsigns;

    public EventFileMetadata(String filename, String messageType, String aerodromeDesignator,
                             int sequenceEntriesCount, Set<String> callsigns) {
        this.filename = filename;
        this.messageType = messageType;
        this.aerodromeDesignator = aerodromeDesignator;
        this.sequenceEntriesCount = sequenceEntriesCount;
        this.callsigns = callsigns;
    }

    public String getFilename() { return filename; }
    public String getMessageType() { return messageType; }
    public String getAerodromeDesignator() { return aerodromeDesignator; }
    public int getSequenceEntriesCount() { return sequenceEntriesCount; }
    public Set<String> getCallsigns() { return callsigns; }

}
