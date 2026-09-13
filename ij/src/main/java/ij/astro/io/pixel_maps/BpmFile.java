package ij.astro.io.pixel_maps;

import ij.astro.util.PixelPatcher;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public record BpmFile(BpmHeader header, Map<PixelPatcher.PatchType.Type, Collection<PixelPatcher.Pixel>> patches) {
    public BpmFile() {
        this(new BpmHeader());
    }

    public BpmFile(BpmHeader header) {
        this(header, new EnumMap<>(PixelPatcher.PatchType.Type.class));
    }

    public BpmFile {
        Objects.requireNonNull(header);
        Objects.requireNonNull(patches);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BpmFile(
                BpmHeader header1, Map<PixelPatcher.PatchType.Type, Collection<PixelPatcher.Pixel>> patches1
        )) {
            return (header == header1 && patches == patches1) || (header.equals(header1) && patches.equals(patches1));
        }
        return false;
    }
}
