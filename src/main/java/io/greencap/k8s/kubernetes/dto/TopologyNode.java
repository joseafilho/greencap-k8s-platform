package io.greencap.k8s.kubernetes.dto;

public record TopologyNode(
        String id,
        String label,
        String type,
        String status,
        String manifestUrl
) {}
