package cdn;

import java.util.HashMap;
import java.util.Map;

public class LFUCache<K,V> implements CachePolicy<K,V>
{
	private final int capacity;
	private final Map<K, Node<K,V>> cache;
	private final Map<Integer, DoublyLinkedList> freqMap;
	private int minFreq;

	private static class Node<K,V>
	{
		K key;
		V value;
		int freq;
		Node<K,V> prev;
		Node<K,V> next;

		Node(K key, V value)
		{
			this.key = key;
			this.value = value;
			this.freq = 1;
		}
	}

	private class DoublyLinkedList
	{
		Node<K,V> head;
		Node<K,V> tail;

		void addToHead(Node<K,V> node)
		{
			node.prev = null;
			node.next = head;
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

		void remove(Node<K,V> node)
		{
			if (node.prev != null)
			{
				node.prev.next = node.next;
			}
			if (node.next != null)
			{
				node.next.prev = node.prev;
			}
			if (node == head)
			{
				head = node.next;
			}
			if (node == tail)
			{
				tail = node.prev;
			}
			node.prev = null;
			node.next = null;
		}

		Node<K,V> removeTail()
		{
			if (tail == null)
			{
				return null;
			}
			Node<K,V> node = tail;
			remove(node);
			return node;
		}

		boolean isEmpty()
		{
			return head == null;
		}
	}

	public LFUCache(int capacity)
	{
		this.capacity = capacity;
		this.cache = new HashMap<>();
		this.freqMap = new HashMap<>();
		this.minFreq = 0;
	}

	@Override
	public V get(K key)
	{
		Node<K,V> node = cache.get(key);
		if (node == null)
		{
			return null;
		}
		increaseFreq(node);
		return node.value;
	}

	@Override
	public V put(K k, V v)
	{
		if (capacity == 0)
		{
			return null;
		}
		Node<K,V> node = cache.get(k);
		if (node != null)
		{
			node.value = v;
			increaseFreq(node);
			return v;
		}
		if (cache.size() >= capacity)
		{
			DoublyLinkedList minList = freqMap.get(minFreq);
			Node<K,V> evict = minList.removeTail();
			if (evict != null)
			{
				cache.remove(evict.key);
			}
		}
		Node<K,V> newNode = new Node<K,V>(k, v);
		cache.put(k, newNode);
		freqMap.computeIfAbsent(1, f -> new DoublyLinkedList()).addToHead(newNode);
		minFreq = 1;
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

	private void increaseFreq(Node<K,V> node)
	{
		int oldFreq = node.freq;
		DoublyLinkedList oldList = freqMap.get(oldFreq);
		oldList.remove(node);
		if (oldList.isEmpty() && oldFreq == minFreq)
		{
			minFreq++;
		}
		node.freq++;
		freqMap.computeIfAbsent(node.freq, f -> new DoublyLinkedList()).addToHead(node);
	}
}