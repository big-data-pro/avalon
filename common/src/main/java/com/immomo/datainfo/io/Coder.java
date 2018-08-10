package com.immomo.datainfo.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;

/**
 * Created by kangkai on 18/2/1.
 */
public class Coder {
    private static final byte [] EMPTY_BYTES = new byte[0];
    private static ThreadLocal<CharsetEncoder> ENCODER_FACTORY =
            new ThreadLocal<CharsetEncoder>() {
                @Override
                protected CharsetEncoder initialValue() {
                    return Charset.forName("UTF-8").newEncoder().
                            onMalformedInput(CodingErrorAction.REPORT).
                            onUnmappableCharacter(CodingErrorAction.REPORT);
                }
            };
    private static ThreadLocal<CharsetDecoder> DECODER_FACTORY =
            new ThreadLocal<CharsetDecoder>() {
                @Override
                protected CharsetDecoder initialValue() {
                    return Charset.forName("UTF-8").newDecoder().
                            onMalformedInput(CodingErrorAction.REPORT).
                            onUnmappableCharacter(CodingErrorAction.REPORT);
                }
            };

    private byte[] bytes;
    private int length;

    public Coder() {
        bytes = EMPTY_BYTES;
    }

    /** Read a UTF8 encoded string from in
     */
    public static String readString(DataInput in) throws IOException {
        return readString(in, Integer.MAX_VALUE);
    }
    /** Write a UTF8 encoded string to out
     */
    public static int writeString(DataOutput out, String s) throws IOException {
        ByteBuffer bytes = encode(s);
        int length = bytes.limit();
        WritableUtils.writeVInt(out, length);
        out.write(bytes.array(), 0, length);
        return length;
    }

    /** Write a UTF8 encoded string with a maximum size to out
     */
    public static int writeString(DataOutput out, String s, int maxLength)
            throws IOException {
        ByteBuffer bytes = encode(s);
        int length = bytes.limit();
        if (length > maxLength) {
            throw new IOException("string was too long to write!  Expected " +
                    "less than or equal to " + maxLength + " bytes, but got " +
                    length + " bytes.");
        }
        WritableUtils.writeVInt(out, length);
        out.write(bytes.array(), 0, length);
        return length;
    }

    /**
     * Converts the provided String to bytes using the
     * UTF-8 encoding. If the input is malformed,
     * invalid chars are replaced by a default value.
     * @return ByteBuffer: bytes stores at ByteBuffer.array()
     *                     and length is ByteBuffer.limit()
     */

    public static ByteBuffer encode(String string)
            throws CharacterCodingException {
        return encode(string, true);
    }

    /**
     * Converts the provided String to bytes using the
     * UTF-8 encoding. If <code>replace</code> is true, then
     * malformed input is replaced with the
     * substitution character, which is U+FFFD. Otherwise the
     * method throws a MalformedInputException.
     * @return ByteBuffer: bytes stores at ByteBuffer.array()
     *                     and length is ByteBuffer.limit()
     */
    public static ByteBuffer encode(String string, boolean replace)
            throws CharacterCodingException {
        CharsetEncoder encoder = ENCODER_FACTORY.get();
        if (replace) {
            encoder.onMalformedInput(CodingErrorAction.REPLACE);
            encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        }
        ByteBuffer bytes =
                encoder.encode(CharBuffer.wrap(string.toCharArray()));
        if (replace) {
            encoder.onMalformedInput(CodingErrorAction.REPORT);
            encoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        }
        return bytes;
    }

    /** Read a UTF8 encoded string with a maximum size
     */
    public static String readString(DataInput in, int maxLength)
            throws IOException {
        int length = WritableUtils.readVIntInRange(in, 0, maxLength);
        byte [] bytes = new byte[length];
        in.readFully(bytes, 0, length);
        return decode(bytes);
    }

/// STATIC UTILITIES FROM HERE DOWN
    /**
     * Converts the provided byte array to a String using the
     * UTF-8 encoding. If the input is malformed,
     * replace by a default value.
     */
    public static String decode(byte[] utf8) throws CharacterCodingException {
        return decode(ByteBuffer.wrap(utf8), true);
    }

    public static String decode(byte[] utf8, int start, int length)
            throws CharacterCodingException {
        return decode(ByteBuffer.wrap(utf8, start, length), true);
    }

    /**
     * Converts the provided byte array to a String using the
     * UTF-8 encoding. If <code>replace</code> is true, then
     * malformed input is replaced with the
     * substitution character, which is U+FFFD. Otherwise the
     * method throws a MalformedInputException.
     */
    public static String decode(byte[] utf8, int start, int length, boolean replace)
            throws CharacterCodingException {
        return decode(ByteBuffer.wrap(utf8, start, length), replace);
    }


    private static String decode(ByteBuffer utf8, boolean replace)
            throws CharacterCodingException {
        CharsetDecoder decoder = DECODER_FACTORY.get();
        if (replace) {
            decoder.onMalformedInput(
                    CodingErrorAction.REPLACE);
            decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        }
        String str = decoder.decode(utf8).toString();
        // set decoder back to its default value: REPORT
        if (replace) {
            decoder.onMalformedInput(CodingErrorAction.REPORT);
            decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        }
        return str;
    }
}
