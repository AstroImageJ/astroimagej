package astroj.astrometry.index.data;

import astroj.astrometry.index.data.IndexSeries.IndexFamily.IndexFile;

import java.util.List;


/// @param name        display name
/// @param description catalog description
/// @param files       the files making up the catalog
public record IndexCatalog(String name, String description, List<IndexFile> files) {
    /// @return the combined size in bytes of every file in this catalog
    public long totalSize() {
        return files.stream().mapToLong(IndexFile::size).sum();
    }

    @Override
    public String toString() {
        return name;
    }
}