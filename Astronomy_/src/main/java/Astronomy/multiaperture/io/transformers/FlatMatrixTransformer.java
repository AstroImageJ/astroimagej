package Astronomy.multiaperture.io.transformers;

import Astronomy.multiaperture.io.Section;
import Astronomy.multiaperture.io.Transformer;

import java.util.Objects;
import java.util.function.IntBinaryOperator;

public class FlatMatrixTransformer implements Transformer<double[], FlatMatrixTransformer.Dimensions> {

    @Override
    public double[] load(Dimensions dim, Section section) {
        Objects.requireNonNull(dim, "Matrix transformer requires dimensions");

        // Hack for when reading back directly written sections
        if (section.getSubSections().size() == 1 && section.getSubSections().get(0).isRoot()) {
            section = section.getSubSections().get(0);
        }

        if (section.getSubSections().size() != dim.height) {
            throw new IllegalArgumentException("Expected %sx%s matrix, but got %sx%s"
                    .formatted(dim.width, dim.height,
                            (section.getSubSections().isEmpty() ? 0 : section.getSubSections().get(0).getParameters().size() + 1),
                            section.getSubSections().size()));
        }

        var matrix = new double[dim.size()];

        for (int row = 0; row < dim.height; row++) {
            var rowSec = section.getSubSections().get(row);

            try {
                matrix[dim.toIndex(row, 0)] = Double.parseDouble(rowSec.name());
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Failed to read matrix value m%s%s".formatted(row, 0), e);
            }

            for (int col = 1; col < dim.width; col++) {
                try {
                    matrix[dim.toIndex(row, col)] = Double.parseDouble(rowSec.getParameter(col-1, "m%s%s".formatted(row, col)));
                } catch (NumberFormatException e) {
                    throw new IllegalStateException("Failed to read matrix value m%s%s".formatted(col, row), e);
                }
            }
        }

        return matrix;
    }

    @Override
    public Section write(Dimensions dim, double[] flatMatrix) {
        Objects.requireNonNull(dim, "Matrix transformer requires dimensions");

        if (flatMatrix.length != dim.size()) {
            throw new IllegalArgumentException("Expected %s matrix elements, but got %s".formatted(dim.size(), flatMatrix.length));
        }

        var s = new Section("matrixRoot", true);
        for (int row = 0; row < dim.height; row++) {
            var rowSec = Section.createSection(Double.toString(flatMatrix[dim.toIndex(row, 0)]));
            for (int col = 1; col < dim.width; col++) {
                rowSec.addParameter(Double.toString(flatMatrix[dim.toIndex(row, col)]));
            }
            s.addSubsection(rowSec);
        }

        return s;
    }

    public record Dimensions(int width, int height, IntBinaryOperator toIndex) {
        public int size() {
            return width * height;
        }

        public Dimensions(int width, int height) {
            this(width, height, null);
        }

        public int toIndex(int row, int col) {
            if (toIndex == null) {
                return row * width + col;
            }

            return toIndex.applyAsInt(col, row);
        }
    }
}
