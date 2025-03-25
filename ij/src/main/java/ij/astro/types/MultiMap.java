package ij.astro.types;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * A MultiMap implementation that allows duplicate keys and maps each key to a list of values.
 *
 * @param <K> the type of keys maintained by this MultiMap
 * @param <V> the type of mapped values
 */
public class MultiMap<K, V> {
    private final Map<K, List<V>> map;

    /**
     * Constructs an empty MultiMap.
     */
    public MultiMap() {
        this.map = new HashMap<>();
    }

    /**
     * Adds a value to the list of values associated with the specified key.
     * If the key does not exist, it will be created.
     *
     * @param key   the key to which the value will be added
     * @param value the value to add
     */
    public void put(K key, V value) {
        map.computeIfAbsent(Objects.requireNonNull(key), k -> new ArrayList<>()).add(Objects.requireNonNull(value));
    }

    /**
     * Returns a list of values associated with the specified key.
     * If the key does not exist, an empty list is returned.
     *
     * @param key the key whose associated values are to be returned
     * @return a list of values associated with the key, or an empty list if the key does not exist
     */
    public List<V> get(K key) {
        return map.getOrDefault(key, Collections.emptyList());
    }

    /**
     * Removes a specific value associated with the specified key.
     * If the value is removed and no more values remain for the key, the key is also removed.
     *
     * @param key   the key whose value is to be removed
     * @param value the value to remove
     * @return {@code true} if the value was removed, {@code false} otherwise
     */
    public boolean remove(K key, V value) {
        List<V> values = map.get(key);
        if (values != null) {
            boolean removed = values.remove(value);
            if (values.isEmpty()) {
                map.remove(key); // Clean up the key if no values remain
            }
            return removed;
        }
        return false;
    }

    /**
     * Removes all values associated with the specified key.
     *
     * @param key the key to be removed along with its associated values
     */
    public void removeAll(K key) {
        map.remove(key);
    }

    /**
     * Checks if the MultiMap contains the specified value for the given key.
     *
     * @param key   the key whose associated values are to be checked
     * @param value the value to look for
     * @return {@code true} if the value is associated with the key, {@code false} otherwise
     */
    public boolean contains(K key, V value) {
        List<V> values = map.get(key);
        return values != null && values.contains(value);
    }

    /**
     * Checks if the MultiMap contains the given key.
     *
     * @param key   the key whose associated values are to be checked
     * @return {@code true} if the key is associated with a value, {@code false} otherwise
     */
    public boolean contains(K key) {
        List<V> values = map.get(key);
        return values != null && !values.isEmpty();
    }

    public void forEach(BiConsumer<K, List<V>> consumer) {
        map.forEach(consumer);
    }

    /**
     * Returns a set of all keys in the MultiMap.
     *
     * @return a set of all keys
     */
    public Set<K> keySet() {
        return map.keySet();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public int size() {
        return map.size();
    }

    @Override
    public String toString() {
        return map.toString();
    }
}
