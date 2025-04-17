/*  Student information for assignment:
 *
 *  On OUR honor, Diego Ortiz (and Nathan Martinez),
 *  this programming assignment is OUR own work
 *  and WE have not provided this code to any other student.
 *
 *  Number of slip days used: 0
 *
 *  Student 1: Diego Ortiz
 *  UTEID: dso463
 *  email address: diegosebortiz@gmail.com
 *
 *  Student 2: Nathan Martinez
 *  UTEID: ncm18395
 *  email address: nathanmtz.1905@gmail.com
 * 
 *  Grader name: Brayden Strong
 *  Section number: 
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SimpleHuffProcessor implements IHuffProcessor {

    private IHuffViewer myViewer;

    // Instance variables to store state between preprocessCompress and compress
    private int preprocessCompressSavedBits;
    private int[] freq;
    private int totalBits;
    private int headerFormat;
    private HuffmanTree huffTree;

    /**
     * Preprocess data so that compression is possible ---
     * count characters/create tree/store state so that
     * a subsequent call to compress will work. The InputStream
     * is <em>not</em> a BitInputStream, so wrap it int one as needed.
     * @param in is the stream which could be subsequently compressed
     * @param headerFormat a constant from IHuffProcessor that determines what kind of
     * header to use, standard count format, standard tree format, or
     * possibly some format added in the future.
     * @return number of bits saved by compression or some other measure
     * Note, to determine the number of
     * bits saved, the number of bits written includes
     * ALL bits that will be written including the
     * magic number, the header format number, the header to
     * reproduce the tree, AND the actual data.
     * @throws IOException if an error occurs while reading from the input file.
     */
    public int preprocessCompress(InputStream in, int headerFormat) throws IOException {
        showString("Starting preprocessing compression...");
        this.headerFormat = headerFormat;

        BitInputStream bitIn = new BitInputStream(in);
        freq = new int[IHuffConstants.ALPH_SIZE + 1]; // 256 + EOF
        totalBits = 0;

        // Count the frequency of each character in the input stream
        int bit;
        while ((bit = bitIn.readBits(IHuffConstants.BITS_PER_WORD)) != -1) {
            freq[bit]++;
            totalBits+= IHuffConstants.BITS_PER_WORD;
        }
        freq[IHuffConstants.PSEUDO_EOF] = 1; // Add EOF character
        bitIn.close();

        // Create the Huffman tree
        huffTree = new HuffmanTree(freq);

        // Calculate header bits
        int magicAndFormatBits = IHuffConstants.BITS_PER_INT * 2; // magic number + header format
        int headerBits = (headerFormat == IHuffConstants.STORE_COUNTS) ?
                huffTree.countHeaderBits() : huffTree.treeHeaderBits();

        //Calculate dataBits
        int dataBits = huffTree.dataBitsCount();

        int compressedBits = magicAndFormatBits + headerBits + dataBits;
        preprocessCompressSavedBits = totalBits - compressedBits;
        showString("Total bits: " + totalBits);
        showString("Preprocess complete. Saved bits: " + preprocessCompressSavedBits);
        return preprocessCompressSavedBits;
    }

    /**
	 * Compresses input to output, where the same InputStream has
     * previously been pre-processed via <code>preprocessCompress</code>
     * storing state used by this call.
     * <br> pre: <code>preprocessCompress</code> must be called before this method
     * @param in is the stream being compressed (NOT a BitInputStream)
     * @param out is bound to a file/stream to which bits are written
     * for the compressed file (not a BitOutputStream)
     * @param force if this is true create the output file even if it is larger than the input file.
     * If this is false do not create the output file if it is larger than the input file.
     * @return the number of bits written.
     * @throws IOException if an error occurs while reading from the input file or
     * writing to the output file.
     */
    public int compress(InputStream in, OutputStream out, boolean force) throws IOException {
        showString("Compressing data...");
        // Check if compression should be forced
        if (!force && preprocessCompressSavedBits <= 0) {
            throw new IOException("Compressed file is larger than original file");
        }

        // Convert InputStream and OutputStream to BitInputStream and BitOutputStream
        BitInputStream bitIn = new BitInputStream(in);
        BitOutputStream bitOut = new BitOutputStream(out);

        // Write the magic number and header format
        bitOut.writeBits(IHuffConstants.BITS_PER_INT, IHuffConstants.MAGIC_NUMBER);
        bitOut.writeBits(IHuffConstants.BITS_PER_INT, headerFormat);

        // Write the header based on the selected format
        if (headerFormat == IHuffConstants.STORE_COUNTS) {
            // Write frequency counts for STORE_COUNTS format
            huffTree.writeCountHeader(bitOut);
        } else {
            // Write the tree structure for STORE_TREE format
            huffTree.writeTreeHeader(bitOut);
        }

        // Write the compressed data
        int writtenBits = 0;
        while ((writtenBits = bitIn.readBits(IHuffConstants.BITS_PER_WORD)) != -1) {
            String code = huffTree.getCode(writtenBits);
            for (char bit : code.toCharArray()) {
                bitOut.writeBits(1, bit == '1' ? 1 : 0);
            }
        }
        // Write the PSEUDO_EOF character to indicate the end of the compressed data
        String eofCode = huffTree.getCode(IHuffConstants.PSEUDO_EOF);
        for (char bit : eofCode.toCharArray()) {
            bitOut.writeBits(1, bit == '1' ? 1 : 0);
        }

        // Close the streams
        bitIn.close();
        bitOut.close();

        int totalBitsWritten = (IHuffConstants.BITS_PER_INT * 2)
            + ((headerFormat == IHuffConstants.STORE_COUNTS) ?
            huffTree.countHeaderBits() : huffTree.treeHeaderBits()) + huffTree.dataBitsCount();

        showString("Compression completed. Total bits written: " + totalBitsWritten);
        // Return the total number of bits written
        return totalBitsWritten;
    }

    /**
     * Uncompress a previously compressed stream in, writing the
     * uncompressed bits/data to out.
     * @param in is the previously compressed data (not a BitInputStream)
     * @param out is the uncompressed file/stream
     * @return the number of bits written to the uncompressed file/stream
     * @throws IOException if an error occurs while reading from the input file or
     * writing to the output file.
     */
    public int uncompress(InputStream in, OutputStream out) throws IOException {
        showString("Uncompressing data...");
        // Convert InputStream and OutputStream to BitInputStream and BitOutputStream
        BitInputStream bitIn = new BitInputStream(in);
        BitOutputStream bitOut = new BitOutputStream(out);

        // Read the magic number and header format
        int magicNumber = bitIn.readBits(IHuffConstants.BITS_PER_INT);
        if (magicNumber != IHuffConstants.MAGIC_NUMBER) {
            bitIn.close();
            bitOut.close();
            throw new IOException("Invalid magic number");
        }

        //Read the header format
        int format = bitIn.readBits(IHuffConstants.BITS_PER_INT);

        HuffmanTree tree;
        if (format == IHuffConstants.STORE_COUNTS) {
            int[] localFreq = new int[IHuffConstants.ALPH_SIZE + 1];
            for (int i = 0; i < IHuffConstants.ALPH_SIZE;i++) {
                localFreq[i] = bitIn.readBits(IHuffConstants.BITS_PER_INT);
            }
            localFreq[IHuffConstants.PSEUDO_EOF] = 1; // Add EOF character
            tree = new HuffmanTree(localFreq);
        } else {
            tree = HuffmanTree.readFrom(bitIn);
        }

        // Decode compressed data
        int bitsWritten = 0;
        int symbol = tree.decodeNext(bitIn);
        while (symbol != IHuffConstants.PSEUDO_EOF) {
            bitOut.writeBits(IHuffConstants.BITS_PER_WORD, symbol);
            bitsWritten += IHuffConstants.BITS_PER_WORD;
            symbol = tree.decodeNext(bitIn);
        }

        bitIn.close();
        bitOut.close();
        return bitsWritten;
    }

    public void setViewer(IHuffViewer viewer) {
        myViewer = viewer;
    }

    private void showString(String s){
        if (myViewer != null) {
            myViewer.update(s);
        }
    }
}
