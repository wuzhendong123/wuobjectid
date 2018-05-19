package com.wu;

import com.wu.util.Base32;

import java.io.Serializable;
import java.net.NetworkInterface;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicInteger;

public final class ObjectId implements Comparable<ObjectId>, Serializable {

    private static final long serialVersionUID = 3670079982654483072L;

    private static final int LOW_ORDER_THREE_BYTES = 0x00ffffff;

    private static final int MACHINE_IDENTIFIER;
    private static final short PROCESS_IDENTIFIER;
    private static final AtomicInteger NEXT_COUNTER = new AtomicInteger(new SecureRandom().nextInt());

    private final int timestamp;
    private final int machineIdentifier;
    private final short processIdentifier;
    private final int counter;

    /**
     * Gets a new object id.
     * @return the new id
     */
    public static ObjectId get() {
        return new ObjectId();
    }

    /**
     * Checks if a string could be an {@code ObjectId}.
     * @param base32String a potential ObjectId as a String.
     * @return whether the string could be an object id
     * @throws IllegalArgumentException if base32String is null
     */
    public static boolean isValid(final String base32String) {
        if (base32String == null) {
            throw new IllegalArgumentException();
        }

        int len = base32String.length();
        if (len != 20) {
            return false;
        }

        for (int i = 0; i < len; i++) {
            char c = base32String.charAt(i);
            if (c >= '0' && c <= '9') {
                continue;
            }
            if (c >= 'a' && c <= 'x' && c != 'i' && c != 'o') {
                continue;
            }
            if (c >= 'A' && c <= 'X' && c != 'i' && c != 'o') {
                continue;
            }

            return false;
        }

        return true;
    }

    /**
     * Gets the generated machine identifier.
     * @return an int representing the machine identifier
     */
    public static int getGeneratedMachineIdentifier() {
        return MACHINE_IDENTIFIER;
    }

    /**
     * Gets the generated process identifier.
     * @return the process id
     */
    public static int getGeneratedProcessIdentifier() {
        return PROCESS_IDENTIFIER;
    }

    /**
     * Gets the current value of the auto-incrementing counter.
     * @return the current counter value.
     */
    public static int getCurrentCounter() {
        return NEXT_COUNTER.get();
    }

    /**
     * Create a new object id.
     */
    public ObjectId() {
        this(new Date());
    }

    /**
     * Constructs a new instance using the given date.
     * @param date the date
     */
    public ObjectId(final Date date) {
        this(dateToTimestampSeconds(date), MACHINE_IDENTIFIER, PROCESS_IDENTIFIER, NEXT_COUNTER.getAndIncrement(), false);
    }

    /**
     * Constructs a new instances using the given date and counter.
     * @param date the date
     * @param counter the counter
     * @throws IllegalArgumentException if the high order byte of counter is not zero
     */
    public ObjectId(final Date date, final int counter) {
        this(date, MACHINE_IDENTIFIER, PROCESS_IDENTIFIER, counter);
    }

    /**
     * Constructs a new instances using the given date, machine identifier, process identifier, and counter.
     * @param date the date
     * @param machineIdentifier the machine identifier
     * @param processIdentifier the process identifier
     * @param counter the counter
     * @throws IllegalArgumentException if the high order byte of machineIdentifier or counter is not zero
     */
    public ObjectId(final Date date, final int machineIdentifier, final short processIdentifier, final int counter) {
        this(dateToTimestampSeconds(date), machineIdentifier, processIdentifier, counter);
    }

    /**
     * Creates an ObjectId using the given time, machine identifier, process identifier, and counter.
     * @param timestamp the time in seconds
     * @param machineIdentifier the machine identifier
     * @param processIdentifier the process identifier
     * @param counter the counter
     * @throws IllegalArgumentException if the high order byte of machineIdentifier or counter is not zero
     */
    public ObjectId(final int timestamp, final int machineIdentifier, final short processIdentifier, final int counter) {
        this(timestamp, machineIdentifier, processIdentifier, counter, true);
    }

    private ObjectId(final int timestamp, final int machineIdentifier, final short processIdentifier, final int counter, final boolean checkCounter) {
        if ((machineIdentifier & 0xff000000) != 0) {
            throw new IllegalArgumentException("The machine identifier must be between 0 and 16777215 (it must fit in three bytes).");
        }
        if (checkCounter && ((counter & 0xff000000) != 0)) {
            throw new IllegalArgumentException("The counter must be between 0 and 16777215 (it must fit in three bytes).");
        }
        this.timestamp = timestamp;
        this.machineIdentifier = machineIdentifier;
        this.processIdentifier = processIdentifier;
        this.counter = counter & LOW_ORDER_THREE_BYTES;
    }

    /**
     * Constructs a new instance from a 20-byte Base32 string representation.
     * @param base32String the string to convert
     * @throws IllegalArgumentException if the string is not a valid Base32 string representation of an ObjectId
     */
    public ObjectId(final String base32String) {
        this(parseBase32String(base32String));
    }

    /**
     * Constructs a new instance from the given byte array
     * @param bytes the byte array
     * @throws IllegalArgumentException if array is null or not of length 12
     */
    public ObjectId(final byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException();
        }
        if (bytes.length != 12) {
            throw new IllegalArgumentException("need 12 bytes");
        }

        timestamp = makeInt(bytes[0], bytes[1], bytes[2], bytes[3]);
        machineIdentifier = makeInt((byte) 0, bytes[4], bytes[5], bytes[6]);
        processIdentifier = (short) makeInt((byte) 0, (byte) 0, bytes[7], bytes[8]);
        counter = makeInt((byte) 0, bytes[9], bytes[10], bytes[11]);
    }

    /**
     * Convert to a byte array. Note that the numbers are stored in big-endian order.
     * @return the byte array
     */
    public byte[] toByteArray() {
        byte[] bytes = new byte[12];
        bytes[0] = int3(timestamp);
        bytes[1] = int2(timestamp);
        bytes[2] = int1(timestamp);
        bytes[3] = int0(timestamp);
        bytes[4] = int2(machineIdentifier);
        bytes[5] = int1(machineIdentifier);
        bytes[6] = int0(machineIdentifier);
        bytes[7] = short1(processIdentifier);
        bytes[8] = short0(processIdentifier);
        bytes[9] = int2(counter);
        bytes[10] = int1(counter);
        bytes[11] = int0(counter);
        return bytes;
    }

    /**
     * Gets the timestamp (number of seconds since the Unix epoch).
     * @return the timestamp
     */
    public int getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the machine identifier.
     * @return the machine identifier
     */
    public int getMachineIdentifier() {
        return machineIdentifier;
    }

    /**
     * Gets the process identifier.
     * @return the process identifier
     */
    public short getProcessIdentifier() {
        return processIdentifier;
    }

    /**
     * Gets the counter.
     * @return the counter
     */
    public int getCounter() {
        return counter;
    }

    /**
     * Gets the timestamp as a {@code Date} instance.
     * @return the Date
     */
    public Date getDate() {
        return new Date(timestamp * 1000L);
    }

    /**
     * Converts this instance into a 20-byte Base32 string representation.
     * @return a string representation of the ObjectId in Base32 format
     */
    private String toBase32String() {
        return Base32.encode(toByteArray());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ObjectId objectId = (ObjectId) o;

        if (counter != objectId.counter) {
            return false;
        }
        if (machineIdentifier != objectId.machineIdentifier) {
            return false;
        }
        if (processIdentifier != objectId.processIdentifier) {
            return false;
        }
        if (timestamp != objectId.timestamp) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = timestamp;
        result = 31 * result + machineIdentifier;
        result = 31 * result + (int) processIdentifier;
        result = 31 * result + counter;
        return result;
    }

    @Override
    public int compareTo(final ObjectId other) {
        if (other == null) {
            throw new NullPointerException();
        }

        byte[] byteArray = toByteArray();
        byte[] otherByteArray = other.toByteArray();
        for (int i = 0; i < 12; i++) {
            if (byteArray[i] != otherByteArray[i]) {
                return ((byteArray[i] & 0xff) < (otherByteArray[i] & 0xff)) ? -1 : 1;
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        return toBase32String();
    }

    static {
        try {
            MACHINE_IDENTIFIER = createMachineIdentifier();
            PROCESS_IDENTIFIER = createProcessIdentifier();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static int createMachineIdentifier() {
        // build a 2-byte machine piece based on NICs info
        int machinePiece;
        try {
            StringBuilder sb = new StringBuilder();
            Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                NetworkInterface ni = e.nextElement();
                sb.append(ni.toString());
                byte[] mac = ni.getHardwareAddress();
                if (mac != null) {
                    ByteBuffer bb = ByteBuffer.wrap(mac);
                    try {
                        sb.append(bb.getChar());
                        sb.append(bb.getChar());
                        sb.append(bb.getChar());
                    } catch (BufferUnderflowException shortHardwareAddressException) { // NOPMD
                        // mac with less than 6 bytes. continue
                    }
                }
            }
            machinePiece = sb.toString().hashCode();
        } catch (Throwable t) {
            // exception sometimes happens with IBM JVM, use random
            machinePiece = (new SecureRandom().nextInt());
            System.out.println("Failed to get machine identifier from network interface, using random number instead");
            t.printStackTrace();
        }
        machinePiece = machinePiece & LOW_ORDER_THREE_BYTES;
        return machinePiece;
    }

    // Creates the process identifier. This does not have to be unique per class loader because
    // NEXT_COUNTER will provide the uniqueness.
    private static short createProcessIdentifier() {
        short processId;
        try {
            String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
            if (processName.contains("@")) {
                processId = (short) Integer.parseInt(processName.substring(0, processName.indexOf('@')));
            } else {
                processId = (short) java.lang.management.ManagementFactory.getRuntimeMXBean().getName().hashCode();
            }

        } catch (Throwable t) {
            processId = (short) new SecureRandom().nextInt();
            System.out.println("Failed to get process identifier from JMX, using random number instead");
            t.printStackTrace();
        }

        return processId;
    }

    private static byte[] parseBase32String(final String s) {
        if (!isValid(s)) {
            throw new IllegalArgumentException("invalid Base32 representation of an ObjectId: [" + s + "]");
        }

        return Base32.decode(s);
    }

    private static int dateToTimestampSeconds(final Date time) {
        return (int) (time.getTime() / 1000);
    }

    // Big-Endian helpers, in this class because all other BSON numbers are little-endian
    private static int makeInt(final byte b3, final byte b2, final byte b1, final byte b0) {
        // CHECKSTYLE:OFF
        return (((b3) << 24) | ((b2 & 0xff) << 16) | ((b1 & 0xff) << 8) | ((b0 & 0xff)));
        // CHECKSTYLE:ON
    }

    private static byte int3(final int x) {
        return (byte) (x >> 24);
    }

    private static byte int2(final int x) {
        return (byte) (x >> 16);
    }

    private static byte int1(final int x) {
        return (byte) (x >> 8);
    }

    private static byte int0(final int x) {
        return (byte) (x);
    }

    private static byte short1(final short x) {
        return (byte) (x >> 8);
    }

    private static byte short0(final short x) {
        return (byte) (x);
    }
}
