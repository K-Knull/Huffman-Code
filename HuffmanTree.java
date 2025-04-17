import java.io.IOException;
import java.util.Arrays;

public class HuffmanTree {
    private final TreeNode root;
    private final String[] codes;
    private final int[] freq;

    /**
     * Build a Huffman tree from a frequency array.
     */
    public HuffmanTree(int[] freq) {
        this.freq = Arrays.copyOf(freq, freq.length);
        this.root = buildTree(this.freq);
        this.codes = new String[freq.length];
        buildCodes(root, "", codes);
    }

    /**
     * Private constructor for decoding-tree reuse.
     */
    private HuffmanTree(TreeNode root, int[] freq) {
        this.root = root;
        this.freq = Arrays.copyOf(freq, freq.length);
        this.codes = new String[freq.length];
        buildCodes(root, "", codes);
    }

    /**
     * Read a tree from a BitInputStream (STORE_TREE header).
     */
    public static HuffmanTree readFrom(BitInputStream in) throws IOException {
        TreeNode root = readTreeNode(in);
        int[] dummyFreq = new int[IHuffConstants.ALPH_SIZE + 1];
        return new HuffmanTree(root, dummyFreq);
    }

    /**
     * Return number of bits to write a STORE_COUNTS header.
     */
    public int countHeaderBits() {
        return IHuffConstants.ALPH_SIZE * IHuffConstants.BITS_PER_INT;
    }

    /**
     * Return number of bits to write a STORE_TREE header.
     */
    public int treeHeaderBits() {
        int shapeBits = calculateTreeBits(root);
        int leafBits = countLeaves(root);
        // leafCount stored as 32-bit, plus shape bits, plus extra 1 bit per leaf
        return IHuffConstants.BITS_PER_INT
                + shapeBits + leafBits;
    }

    /**
     * Write a STORE_COUNTS header to the BitOutputStream.
     */
    public void writeCountHeader(BitOutputStream out) throws IOException {
        for (int i = 0; i < IHuffConstants.ALPH_SIZE; i++) {
            out.writeBits(IHuffConstants.BITS_PER_INT, freq[i]);
        }
    }

    /**
     * Write a STORE_TREE header (preorder) to the BitOutputStream.
     */
    public void writeTreeHeader(BitOutputStream out) throws IOException {
        int leafCount = countLeaves(root);
        out.writeBits(IHuffConstants.BITS_PER_INT, leafCount);
        writeTree(out, root);
        writeLeafMarkers(out, root);
    }

    /**
     * Return the code string for a given symbol.
     */
    public String getCode(int symbol) {
        return codes[symbol];
    }

    /**
     * Decode next symbol from the BitInputStream (until a leaf is reached).
     */
    public int decodeNext(BitInputStream in) throws IOException {
        TreeNode current = root;
        while (!current.isLeaf()) {
            int bit = in.readBits(1);
            if (bit == -1) throw new IOException("Unexpected EOF in decode");
            current = (bit == 0) ? current.getLeft() : current.getRight();
        }
        return current.getValue();
    }

    /**
     * Compute number of bits needed to encode data with this tree (including PSEUDO_EOF).
     */
    public int dataBitsCount() {
        int count = 0;
        for (int i = 0; i < freq.length; i++) {
            if (freq[i] > 0) {
                count += freq[i] * codes[i].length();
            }
        }
        return count;
    }

    /**
     * Helper method to build the Huffman tree from the frequency array.
     * @param freq the frequency array
     * @return the root of the Huffman tree
     */
    private TreeNode buildTree(int[] freq) {
        FairPriorityQueue<TreeNode> pq = new FairPriorityQueue<>();
        for (int i = 0; i < freq.length; i++) {
            if (freq[i] > 0) {
                pq.enqueue(new TreeNode(i, freq[i]));
            }
        }
        while (pq.size() > 1) {
            TreeNode left = pq.dequeue();
            TreeNode right = pq.dequeue();
            pq.enqueue(new TreeNode(left, -1, right));
        }
        return pq.dequeue();
    }

    /**
     * helper method to build the codes for each symbol.
     * @param node the current node in the tree
     * @param prefix the current prefix for the code
     * @param table the array to store the codes
     */
    private void buildCodes(TreeNode node, String prefix, String[] table) {
        if (node.isLeaf()) {
            table[node.getValue()] = prefix;
        } else {
            buildCodes(node.getLeft(), prefix + "0", table);
            buildCodes(node.getRight(), prefix + "1", table);
        }
    }

    /**
     * Helper method to read a tree node from the BitInputStream.
     * @param in the BitInputStream to read from
     * @return the TreeNode read from the stream
     * @throws IOException 
     */
    private static TreeNode readTreeNode(BitInputStream in) throws IOException {
        int bit = in.readBits(1);
        if (bit == -1) throw new IOException("Unexpected EOF during tree read");
        if (bit == 1) {
            int val = in.readBits(IHuffConstants.BITS_PER_WORD);
            return new TreeNode(val, 0);
        } else {
            TreeNode left = readTreeNode(in);
            TreeNode right = readTreeNode(in);
            return new TreeNode(left, -1, right);
        }
    }

    /**
     * Helper method to calculate the number of bits needed to store the tree.
     * @param node the current node in the tree
     * @return the number of bits needed to store the tree
     */
    private int calculateTreeBits(TreeNode node) {
        if (node.isLeaf()) {
            return 1 + IHuffConstants.BITS_PER_WORD;
        }
        return 1 + calculateTreeBits(node.getLeft()) + calculateTreeBits(node.getRight());
    }

    /**
     * Helper method to write the tree to the BitOutputStream.
     * @param out the BitOutputStream to write to
     * @param node the current node in the tree
     * @throws IOException
     */
    private void writeTree(BitOutputStream out, TreeNode node) throws IOException {
        if (node.isLeaf()) {
            out.writeBits(1, 1);
            out.writeBits(IHuffConstants.BITS_PER_WORD, node.getValue());
        } else {
            out.writeBits(1, 0);
            writeTree(out, node.getLeft());
            writeTree(out, node.getRight());
        }
    }

    /**
     * Helper method to write the leaf marker to the BitOutputStream.
     * @param node the current node in the tree
     * @return the number of leaves in the tree
     */
    private int countLeaves(TreeNode node) {
        if (node == null) {
            return 0;
        }
        if (node.isLeaf()) {
            return 1;
        }
        return countLeaves(node.getLeft()) + countLeaves(node.getRight());
    }

    private void writeLeafMarkers(BitOutputStream out, TreeNode node) throws IOException {
        if (node.isLeaf()) {
            out.writeBits(1, 1);
        } else {
            writeLeafMarkers(out, node.getLeft());
            writeLeafMarkers(out, node.getRight());
        }
    } 

}

