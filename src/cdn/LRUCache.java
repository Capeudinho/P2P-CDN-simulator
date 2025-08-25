package cdn;

import java.util.HashMap;
import java.util.Map;

public class LRUCache<K,V> implements CachePolicy<K,V>
{
	private final int capacity;
	private final Map<K, Node<K,V>> cache;
	private Node<K,V> head;
	private Node<K,V> tail;

	private static class Node<K,V>
	{
		K key;
		V value;
		Node<K,V> prev;
		Node<K,V> next;

		Node(K key, V value)
		{
			this.key = key;
			this.value = value;
		}
	}

	public LRUCache(int capacity)
	{
		this.capacity = capacity;
		this.cache = new HashMap<>();
		this.head = null;
		this.tail = null;
	}

	@Override
	public V get(K key)
	{
		Node<K,V> node = cache.get(key);
		if (node == null)
		{
			return null;
		}
		moveToHead(node);
		return node.value;
	}

	@Override
	public V put(K k, V v)
	{
		Node<K,V> node = cache.get(k);
		if (node != null)
		{
			node.value = v;
			moveToHead(node);
			return v;
		}
		node = new Node<>(k, v);
		cache.put(k, node);
		addToHead(node);
		if (cache.size() > capacity)
		{
			removeTail();
		}
		return v;
	}

	@Override
	public boolean contains(K k)
	{
		return cache.containsKey(k);
	}

	@Override
	public int size()
	{
		return cache.size();
	}

	private void addToHead(Node<K,V> node)
	{
		node.next = head;
		node.prev = null;
		if (head != null)
		{
			head.prev = node;
		}
		head = node;
		if (tail == null)
		{
			tail = node;
		}
	}

	private void moveToHead(Node<K,V> node)
	{
		if (node == head)
		{
			return;
		}
		if (node.prev != null)
		{
			node.prev.next = node.next;
		}
		if (node.next != null)
		{
			node.next.prev = node.prev;
		}
		if (node == tail)
		{
			tail = node.prev;
		}
		node.prev = null;
		node.next = head;
		if (head != null)
		{
			head.prev = node;
		}
		head = node;
	}

	private void removeTail()
	{
		if (tail == null)
		{
			return;
		}
		cache.remove(tail.key);
		if (tail.prev != null)
		{
			tail.prev.next = null;
		}
		tail = tail.prev;
		if (tail == null)
		{
			head = null;
		}
	}
}