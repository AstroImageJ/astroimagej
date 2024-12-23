package Astronomy.multiaperture.io.transformers;

import Astronomy.multiaperture.io.Section;
import Astronomy.multiaperture.io.Transformer;

import java.util.Properties;

public class PrefsTransformer implements Transformer<Properties> {
    private final String name;

    public PrefsTransformer(String name) {
        this.name = name;
    }

    @Override
    public Properties load(Section section) {
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
    public Section write(Properties prefs) {
        var s = new Section(name);

        prefs.forEach((k, v) -> {
            if (k instanceof String ks) {
                s.addSubsection(Section.createSection(ks, v.toString()));
            }
        });

        return s;
    }
}
