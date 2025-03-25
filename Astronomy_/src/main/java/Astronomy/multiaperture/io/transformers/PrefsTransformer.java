package Astronomy.multiaperture.io.transformers;

import Astronomy.multiaperture.io.Section;
import Astronomy.multiaperture.io.Transformer;

import java.util.Objects;
import java.util.Properties;

public class PrefsTransformer implements Transformer<Properties, String> {

    @Override
    public Properties load(String sectionName, Section section) {
        Objects.requireNonNull(sectionName, "Preferences transformer requires section name");

        var view = section.createMapView();

        var prefs = new Properties();

        view.forEach((k, sections) -> {
            assert !sections.isEmpty();

            var s = sections.get(sections.size()-1); // Silently consume duplicate sections
            var v = s.getParametersLine(); // Allow tabs in values

            prefs.put(s.name(), v);
        });

        return prefs;
    }

    @Override
    public Section write(String sectionName, Properties prefs) {
        Objects.requireNonNull(sectionName, "Preferences transformer requires section name");

        var s = new Section(sectionName);

        prefs.forEach((k, v) -> {
            if (k instanceof String ks) {
                s.addSubsection(Section.createSection(ks, v.toString()));
            }
        });

        return s;
    }
}
