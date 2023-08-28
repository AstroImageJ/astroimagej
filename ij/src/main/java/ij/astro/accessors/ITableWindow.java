package ij.astro.accessors;

import ij.measure.ResultsTable;

public interface ITableWindow {
    ResultsTable getTable();

    void rename(String name);

    String getTitle();
}
