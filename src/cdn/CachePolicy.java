package cdn;

public interface CachePolicy<K,V>
{
	V get(K key);
	V put(K key, V value);
	boolean contains(K key);
	int size();
}