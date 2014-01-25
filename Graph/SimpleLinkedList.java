package team108.Graph;

import java.util.Iterator;
import java.util.LinkedList;

public class SimpleLinkedList<E> extends LinkedList<E> implements Iterable<E>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6524158000127100209L;
	int size = 0;
	SLL_Node<E> head = null;
	
	public SimpleLinkedList() {	}
	
	public boolean add(E data) {
		size++;
		SLL_Node<E> newNode = new SLL_Node<E>(data);
		newNode.next = head;
		head = newNode;
		return true;
	}
	
	public int size() { return size; }
	
	
	private static class SLL_Node<E> {
		
		SLL_Node<E> next = null;
		E data = null;
		
		public SLL_Node(E dat) {
			data = dat;
		}
		
		public SLL_Node<E> clone() { return new SLL_Node<E>(data); }
		
	}
	
	@Override
	public SimpleLinkedList<E> clone() {
		SimpleLinkedList<E> sll = new SimpleLinkedList<E>();
		
		if ( head != null ) { 
			sll.head = head.clone();

			SLL_Node<E> current = head;
			SLL_Node<E> currentClone = sll.head;
			
			while ( current.next != null ) {
				currentClone.next = current.next.clone();
				current = current.next;
				currentClone = currentClone.next;
			}
		}
		
		
		//System.out.println("CLONE TEST!");
		return sll;
	}


	@Override
	public Iterator<E> iterator() {
		Iterator<E> newIter = new SimpleLinkedList.Iterator<E>(this);
		return newIter;
	}
	
	private static class Iterator<E> implements java.util.Iterator<E>{
		
		SLL_Node<E> current;
		
		public Iterator(SimpleLinkedList<E> in) {
			current = in.head;
		}

		@Override
		public boolean hasNext() {
			if ( current != null ) return true;
			return false;
		}

		@Override
		public E next() {
			if ( current != null ) {
				E ret = current.data;
				current = current.next;
				return ret;				
			}
			return null;
		}

		@Override
		public void remove() { }
		
	}

}
