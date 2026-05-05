package com.github.swim_developer.validator.ed254.consumer.domain.model;


import java.util.List;

public class FilterOptions {

    private final List<String> messageTypes;
    private final List<String> aerodromes;
    private final List<String> callsigns;

    public FilterOptions(List<String> messageTypes, List<String> aerodromes, List<String> callsigns) {
        this.messageTypes = messageTypes;
        this.aerodromes = aerodromes;
        this.callsigns = callsigns;
    }

    public List<String> getMessageTypes() { return messageTypes; }
    public List<String> getAerodromes() { return aerodromes; }
    public List<String> getCallsigns() { return callsigns; }
}
