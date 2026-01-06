package com.ddo.torneios.request;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
public class JuizVirtualRequest {
    private List<Content> contents;

    @Data
    @AllArgsConstructor
    public static class Content {
        private List<Part> parts;
    }

    @Data
    @AllArgsConstructor
    public static class Part {
        private String text;
    }
}
