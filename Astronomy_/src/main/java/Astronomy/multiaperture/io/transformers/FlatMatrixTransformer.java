package Astronomy.multiaperture.io.transformers;

import Astronomy.multiaperture.io.Section;
import Astronomy.multiaperture.io.Transformer;

import java.util.Objects;

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

        for (int j = 0; j < dim.height; j++) {
            var row = section.getSubSections().get(j);

            try {
                matrix[j * dim.width] = Double.parseDouble(row.name());
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Failed to read matrix value m%s%s".formatted(0, j), e);
            }

            for (int i = 1; i < dim.width; i++) {
                try {
                    matrix[j * dim.width + i] = Double.parseDouble(row.getParameter(i-1, "m%s%s".formatted(i-1, j)));
                } catch (NumberFormatException e) {
                    throw new IllegalStateException("Failed to read matrix value m%s%s".formatted(i-1, j), e);
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
        for (int j = 0; j < dim.height; j++) {
            var row = Section.createSection(Double.toString(flatMatrix[j * dim.width]));
            for (int i = 1; i < dim.width; i++) {
                row.addParameter(Double.toString(flatMatrix[j * dim.width + i]));
            }
            s.addSubsection(row);
        }

        return s;
    }

    public record Dimensions(int width, int height) {
        public int size() {
            return width * height;
        }
    }
}
