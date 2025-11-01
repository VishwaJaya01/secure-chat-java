package com.securechat.web.figma;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

public class TokensConverter {

    public String toCss(Map<String, Object> tokens) {
        Map<String, String> flat = new LinkedHashMap<>();
        flatten("", tokens, flat);
        StringJoiner joiner = new StringJoiner(" ", ":root {", " }");
        flat.forEach((key, value) -> joiner.add("--" + key + ":" + value + ";"));
        return joiner.toString();
    }

    private void flatten(String prefix, Object node, Map<String, String> sink) {
        if (node instanceof Map<?, ?> map) {
            map.forEach((k, v) -> {
                String next = prefix.isEmpty() ? sanitize(k.toString()) : prefix + "-" + sanitize(k.toString());
                flatten(next, v, sink);
            });
        } else if (node != null) {
            sink.put(prefix, node.toString());
        }
    }

    private String sanitize(String token) {
        return token.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }
}
