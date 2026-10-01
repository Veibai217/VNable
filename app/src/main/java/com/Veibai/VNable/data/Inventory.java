package com.Veibai.VNable.data;

import java.util.LinkedHashMap;
import java.util.Map;

public class Inventory {

    private final LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();

    public void add(String name, int n) {
        if (n <= 0) return;
        counts.put(name, counts.getOrDefault(name, 0) + n);
    }

    public void min(String name, int n) {
        if (n <= 0) return;
        int left = Math.max(0, counts.getOrDefault(name, 0) - n);
        if (left == 0) counts.remove(name);
        else counts.put(name, left);
    }

    public int count(String name) {
        return counts.getOrDefault(name, 0);
    }

    public int distinctCount() {
        return counts.size();
    }

    public Map<String, Integer> snapshot() {
        return new LinkedHashMap<>(counts);
    }

    public void restore(Map<String, Integer> data) {
        counts.clear();
        if (data != null) {
            for (Map.Entry<String, Integer> e : data.entrySet()) {
                if (e.getValue() != null && e.getValue() > 0) counts.put(e.getKey(), e.getValue());
            }
        }
    }
}
