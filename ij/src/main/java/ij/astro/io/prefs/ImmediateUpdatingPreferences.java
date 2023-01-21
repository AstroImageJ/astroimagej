package ij.astro.io.prefs;

import ij.Prefs;
import ij.astro.logging.AIJLogger;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.HashMap;

public class ImmediateUpdatingPreferences {
    private final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();
    private final HashMap<String, String> settingsSuffix = new HashMap<>();

    protected ImmediateUpdatingPreferences() {
        for (Field field : getClass().getDeclaredFields()) {
            field.getName();
        }
    }

    protected void preferenceChanged() {
        var maybeStackFrame = STACK_WALKER.walk(s -> s.skip(1)/*.limit(1)*/.findFirst());
        maybeStackFrame.ifPresent(stackFrame -> {
            AIJLogger.log(stackFrame.getClassName());
            AIJLogger.log(stackFrame.getMethodName());
            AIJLogger.log(getCurrentValue(stackFrame));
        });
    }

    protected void preferenceChanged(String settingsSuffix) {

    }

    protected void updatePreferencesFile(StackWalker.StackFrame stackFrame) {
        Prefs.set(getPropertyName(stackFrame), getCurrentValue(stackFrame));
    }

    public void loadPreferences() {
        for (Field field : getClass().getDeclaredFields()) {
            try {
                var vh = LOOKUP.unreflectVarHandle(field);//todo handle settings suffix
                var t = field.getType();
                if (t == Double.TYPE) {
                    vh.set(this, Prefs.get(getPropertyName(field), (double) vh.get(this)));
                } else if (t == Integer.TYPE) {
                    vh.set(this, (int) Prefs.get(getPropertyName(field), (double) vh.get(this)));
                } else if (t == Boolean.TYPE) {
                    vh.set(this, Prefs.get(getPropertyName(field), (boolean) vh.get(this)));
                } else if (t == String.class) {
                    vh.set(this, Prefs.get(getPropertyName(field), (String) vh.get(this)));
                } else {
                    vh.set(this, Prefs.get(getPropertyName(field), vh.get(this).toString()));
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void savePreferences() {
        Prefs.savePreferences();
    }

    protected String getSettingsOwner(StackWalker.StackFrame stackFrame) {
        return stackFrame.getClassName();
    }

    protected String getPropertyName(StackWalker.StackFrame stackFrame) {
        return getPropertyName(stackFrame, "");
    }

    protected String getPropertyName(StackWalker.StackFrame stackFrame, String settingsSuffix) {
        return getSettingsOwner(stackFrame) + "." + // Class name
                stackFrame.getMethodName().replaceFirst("get", "")
                        .replaceFirst("set", "").toLowerCase()
                + settingsSuffix;
    }

    protected String getPropertyName(Field field) {
        return getPropertyName(field, "");
    }

    protected String getPropertyName(Field field, String settingsSuffix) {
        return getClass().getName() + "." + field.getName().toLowerCase() + settingsSuffix;
    }

    //todo cache lookup results
    protected String getCurrentValue(StackWalker.StackFrame stackFrame) {
        var setterType = stackFrame.getMethodType();
        if (setterType.parameterCount() != 1 && setterType.returnType() != void.class) {
            throw new IllegalCallerException("Cannot handle non setter-type methods");
        } else {
            setterType = setterType.changeReturnType(setterType.lastParameterType());
            setterType = setterType.dropParameterTypes(0, 1);
            try {
                var mh = LOOKUP.findVirtual(stackFrame.getDeclaringClass(), stackFrame.getMethodName().replaceFirst("set", "get"), setterType);
                return mh.invoke(this).toString();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }
}
