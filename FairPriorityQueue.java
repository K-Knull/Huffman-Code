import java.util.ArrayList;
import java.util.List;

/**
 * This class implements a priority queue that is used to build the Huffman Code Tree.
 * 
 * Your priority queue must handle ties in a fair way. This means that if you add a value to
 * the queue that has equal priority to an existing value, it must be enqueued after the 
 * existing values. For your assignment, this will occur in the form of two nodes having the
 * same frequency. The node added to the queue last should be behind all other nodes of
 * the same or lower frequencies.
 * When dequeuing elements from the priority queue to build your Huffman Code Tree,
 * the first element dequeued must be the left child of the new connecting node and the
 * second element dequeued must be the right child of the new connecting node.
 * 
 */
public class FairPriorityQueue<E extends Comparable<? super E>> {
    private List<E> queue; // the queue of nodes

    public FairPriorityQueue() {
        this.queue = new ArrayList<>();
    }

    public void enqueue(E n) {
        int index = 0;

        while (index < queue.size() && n.compareTo(queue.get(index)) >= 0) {
            index++; // Find the position to insert the new element
        }
        queue.add(index, n); // Insert the new element at the found position
    }

    public E dequeue() {
        if (queue.isEmpty()) {
            return null; // Queue is empty
        }
        return queue.remove(0); // Remove the first element (highest priority)
    }

    public int size() {
        return queue.size(); // Return the size of the queue
    }
}
