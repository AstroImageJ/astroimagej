package Astronomy.multiaperture.io;

import ij.astro.types.MultiMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

public class Section {
    protected final String name;
    private List<String> parameters = new ArrayList<>();
    private List<Section> subSections = new ArrayList<>();
    private final boolean isRoot;
    private Section parent;

    public Section(String name) {
        this(name, false);
    }

    public Section(String name, boolean isRoot) {
        this.name = name;
        this.isRoot = isRoot;
    }

    public void addSubsection(Section section) {
        subSections.add(Objects.requireNonNull(section));
        section.parent = this;
    }

    public void addParameter(String s) {
        parameters.add(Objects.requireNonNull(s));
    }

    public String getParameter(int i, String paramName) {
        if (i >= parameters.size()) {
            //System.out.println(this);
            throw new IllegalArgumentException("Could not get %s (%s) parameter for %s".formatted(paramName, i, name));
        }

        return parameters.get(i);
    }

    public <T> T getParameter(Parameter<T> parameter) {
        var paramString = getParameter(parameter.index(), parameter.name());

        return parameter.deserialize(paramString);
    }

    public String getParametersLine() {
        if (parameters.isEmpty()) {
            return "";
        }

        var sb = new StringBuilder(parameters.get(0));

        for (int i = 1; i < parameters.size(); i++) {
            sb.append('\t').append(parameters.get(i));
        }

        return sb.toString();
    }

    public List<String> getParameters() {
        return parameters;
    }

    public List<Section> getSubSections() {
        return subSections;
    }

    public boolean hasParameters() {
        return !parameters.isEmpty();
    }

    public boolean hasSubsections() {
        return !subSections.isEmpty();
    }

    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }

    public static Section createSection(String name, String... parameters) {
        var s = new Section(name);

        for (String parameter : parameters) {
            s.addParameter(parameter);
        }

        return s;
    }

    public static <T1> Section createSection(String name, Parameter<T1> t1, T1 o1) {
        assert t1.index == 0;
        return createSection(name, t1.serialize(o1));
    }

    public static <T1, T2> Section createSection(String name, Parameter<T1> t1, T1 o1, Parameter<T2> t2, T2 o2) {
        assert t1.index == 0 && t2.index == 1;
        return createSection(name, t1.serialize(o1), t2.serialize(o2));
    }

    public static <T1, T2, T3> Section createSection(String name, Parameter<T1> t1, T1 o1, Parameter<T2> t2, T2 o2,
                                                     Parameter<T3> t3, T3 o3) {
        assert t1.index == 0 && t2.index == 1 && t3.index == 2;
        return createSection(name, t1.serialize(o1), t2.serialize(o2), t3.serialize(o3));
    }

    public static <T1, T2, T3, T4> Section createSection(String name, Parameter<T1> t1, T1 o1, Parameter<T2> t2, T2 o2,
                                                     Parameter<T3> t3, T3 o3, Parameter<T4> t4, T4 o4) {
        assert t1.index == 0 && t2.index == 1 && t3.index == 2 && t4.index == 3;
        return createSection(name, t1.serialize(o1), t2.serialize(o2), t3.serialize(o3), t4.serialize(o4));
    }

    public static <T1, T2, T3, T4, T5> Section createSection(String name, Parameter<T1> t1, T1 o1,
                                                             Parameter<T2> t2, T2 o2, Parameter<T3> t3, T3 o3,
                                                             Parameter<T4> t4, T4 o4, Parameter<T5> t5, T5 o5) {
        assert t1.index == 0 && t2.index == 1 && t3.index == 2 && t4.index == 3 && t5.index == 4;
        return createSection(name, t1.serialize(o1), t2.serialize(o2), t3.serialize(o3),
                t4.serialize(o4), t5.serialize(o5));
    }

    public static <T1, T2, T3, T4, T5, T6> Section createSection(String name, Parameter<T1> t1, T1 o1, Parameter<T2> t2,
                                                                 T2 o2, Parameter<T3> t3, T3 o3, Parameter<T4> t4, T4 o4,
                                                                 Parameter<T5> t5, T5 o5, Parameter<T6> t6, T6 o6) {
        assert t1.index == 0 && t2.index == 1 && t3.index == 2 && t4.index == 3 && t5.index == 4 && t6.index == 5;
        return createSection(name, t1.serialize(o1), t2.serialize(o2), t3.serialize(o3), t4.serialize(o4),
                t5.serialize(o5), t6.serialize(o6));
    }

    public MultiMap<String, Section> createMapView() {
        var map = new MultiMap<String, Section>();

        if (!subSections.isEmpty()) {
            for (Section subSection : subSections) {
                map.put(subSection.name, subSection);
            }
        }

        return map;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"name\":\"")
                .append(Objects.toString(name, "")).append('\"');

        sb.append(",\"parameters\":");
        if ((parameters) != null && !(parameters).isEmpty()) {
            sb.append("[");
            final int listSize = (parameters).size();
            for (int i = 0; i < listSize; i++) {
                final Object listValue = (parameters).get(i);
                if (listValue instanceof CharSequence) {
                    sb.append("\"").append(Objects.toString(listValue, "")).append("\"");
                } else {
                    sb.append(Objects.toString(listValue, ""));
                }
                if (i < listSize - 1) {
                    sb.append(",");
                } else {
                    sb.append("]");
                }
            }
        } else {
            sb.append("[]");
        }

        sb.append(",\"subSections\":");
        if ((subSections) != null && !(subSections).isEmpty()) {
            sb.append("[");
            final int listSize = (subSections).size();
            for (int i = 0; i < listSize; i++) {
                final Object listValue = (subSections).get(i);
                if (listValue instanceof CharSequence) {
                    sb.append("\"").append(Objects.toString(listValue, "")).append("\"");
                } else {
                    sb.append(Objects.toString(listValue, ""));
                }
                if (i < listSize - 1) {
                    sb.append(",");
                } else {
                    sb.append("]");
                }
            }
        } else {
            sb.append("[]");
        }

        sb.append(",\"isRoot\":")
                .append(isRoot);

        sb.append('}');
        return sb.toString();
    }

    public boolean isRoot() {
        return isRoot;
    }

    public String name() {
        return name;
    }

    public Section getParent() {
        return parent;
    }

    public record Parameter<T>(String name, int index, Class<T> type, BiFunction<Parameter<T>, String, T> deserialize,
                               BiFunction<Parameter<T>, T, String> serialize) {
        public Parameter(String name, int index, Class<T> type) {
            this(name, index, type, createDeserializer(type), createSerializer(type));
        }

        public T deserialize(String s) {
            return deserialize.apply(this, s);
        }

        public String serialize(T o) {
            return serialize.apply(this, o);
        }

        private static <T> BiFunction<Parameter<T>, String, T> createDeserializer(Class<T> clazz) {
            if (clazz == Boolean.class || clazz == Boolean.TYPE) {
                return (parameter, s) -> {
                    try {
                        //noinspection unchecked
                        return (T) Boolean.valueOf(s);
                    } catch (Exception e) {
                        throw new IllegalStateException("Error converting '%s' to boolean for parameter '%s'"
                                .formatted(s, parameter.name()), e);
                    }
                };
            } else if (clazz == Integer.class || clazz == Integer.TYPE) {
                return (parameter, s) -> {
                    try {
                        //noinspection unchecked
                        return (T) Integer.valueOf(s);
                    } catch (Exception e) {
                        throw new IllegalStateException("Error converting '%s' to integer for parameter '%s'"
                                .formatted(s, parameter.name()), e);
                    }
                };
            } else if (clazz == Double.class || clazz == Double.TYPE) {
                return (parameter, s) -> {
                    try {
                        //noinspection unchecked
                        return (T) Double.valueOf(s);
                    } catch (Exception e) {
                        throw new IllegalStateException("Error converting '%s' to double for parameter '%s'"
                                .formatted(s, parameter.name()), e);
                    }
                };
            } else if (clazz == Byte.class || clazz == Byte.TYPE) {
                return (parameter, s) -> {
                    try {
                        //noinspection unchecked
                        return (T) Byte.valueOf(s);
                    } catch (Exception e) {
                        throw new IllegalStateException("Error converting '%s' to byte for parameter '%s'"
                                .formatted(s, parameter.name()), e);
                    }
                };
            } else if (clazz == Short.class || clazz == Short.TYPE) {
                return (parameter, s) -> {
                    try {
                        //noinspection unchecked
                        return (T) Short.valueOf(s);
                    } catch (Exception e) {
                        throw new IllegalStateException("Error converting '%s' to short for parameter '%s'"
                                .formatted(s, parameter.name()), e);
                    }
                };
            } else if (clazz == Long.class || clazz == Long.TYPE) {
                return (parameter, s) -> {
                    try {
                        //noinspection unchecked
                        return (T) Long.valueOf(s);
                    } catch (Exception e) {
                        throw new IllegalStateException("Error converting '%s' to long for parameter '%s'"
                                .formatted(s, parameter.name()), e);
                    }
                };
            } else if (clazz == Float.class || clazz == Float.TYPE) {
                return (parameter, s) -> {
                    try {
                        //noinspection unchecked
                        return (T) Float.valueOf(s);
                    } catch (Exception e) {
                        throw new IllegalStateException("Error converting '%s' to float for parameter '%s'"
                                .formatted(s, parameter.name()), e);
                    }
                };
            } else if (clazz == String.class) {
                //noinspection unchecked
                return ((parameter, s) -> (T) s);
            }

            throw new IllegalArgumentException("Must specify deserializer for parameter of type: " + clazz);
        }

        private static <T> BiFunction<Parameter<T>, T, String> createSerializer(Class<T> clazz) {
            if (clazz == Boolean.class || clazz == Boolean.TYPE) {
                return ((parameter, t) -> t.toString());
            } else if (clazz == Integer.class || clazz == Integer.TYPE) {
                return ((parameter, t) -> t.toString());
            } else if (clazz == Double.class || clazz == Double.TYPE) {
                return ((parameter, t) -> t.toString());
            } else if (clazz == Byte.class || clazz == Byte.TYPE) {
                return ((parameter, t) -> t.toString());
            } else if (clazz == Short.class || clazz == Short.TYPE) {
                return ((parameter, t) -> t.toString());
            } else if (clazz == Long.class || clazz == Long.TYPE) {
                return ((parameter, t) -> t.toString());
            } else if (clazz == Float.class || clazz == Float.TYPE) {
                return ((parameter, t) -> t.toString());
            } else if (clazz == String.class) {
                return ((parameter, t) -> (String) t);
            }

            throw new IllegalArgumentException("Must specify serializer for parameter of type: " + clazz);
        }
    }
}
