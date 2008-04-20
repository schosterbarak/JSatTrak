package gov.nasa.worldwind.formats.nitfs;

import gov.nasa.worldwind.util.StringUtil;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
/*
Copyright (C) 2001, 2007 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

/**
 * @author Lado Garakanidze
 * @version $Id: NitfsUtil Mar 30, 2007 12:43:29 PM lado
 */
public class NITFSUtil
{
    public static String getString(java.nio.ByteBuffer buffer, int offset, int len)
    {
        String s = StringUtil.EMPTY;
        if (null != buffer && buffer.capacity() >= offset + len)
        {
            byte[] dest = new byte[len];
            buffer.position(offset);
            buffer.get(dest, 0, len);
            s = new String(dest).trim();
        }
        return s;
    }

    public static String getString(java.nio.ByteBuffer buffer, int len)
    {
        String s = StringUtil.EMPTY;
        if (null != buffer && buffer.remaining() >= len)
        {
            byte[] dest = new byte[len];
            buffer.get(dest, 0, len);
            s = new String(dest).trim();
        }
        return s;
    }

    public static int getNumeric(java.nio.ByteBuffer buffer, int len)
    {
        String s = StringUtil.EMPTY;
        if (null != buffer && buffer.remaining() >= len)
        {
            byte[] dest = new byte[len];
            buffer.get(dest, 0, len);
            s = new String(dest);
        }
        return Integer.parseInt(s);
    }

    public static short getShortNumeric(java.nio.ByteBuffer buffer, int len)
    {
        String s = StringUtil.EMPTY;
        if (null != buffer && buffer.remaining() >= len)
        {
            byte[] dest = new byte[len];
            buffer.get(dest, 0, len);
            s = new String(dest);
        }
        return (short) (0xFFFF & Integer.parseInt(s));
    }

    public static boolean getBoolean(java.nio.ByteBuffer buffer)
    {
        return !((byte) 0 == buffer.get()); // 0 = false, non-zero = true
    }

    public static short getByteAsShort(java.nio.ByteBuffer buffer)
    {
        return (short) (0xFF & buffer.get());
    }

    public static int getUShort(java.nio.ByteBuffer buffer)
    {
        return 0xFFFF & buffer.getShort();
    }

    public static long getUInt(java.nio.ByteBuffer buffer)
    {
        return 0xFFFFFFFFL & (long) buffer.getInt();
    }

    private static final int PAGE_SIZE = 4096;


    public static java.nio.ByteBuffer readEntireFile(java.io.File file) throws java.io.IOException
    {
        return memoryMapFile(file);
        // return NITFSUtil.readFile(file);
    }

    private static java.nio.ByteBuffer readFile(java.io.File file) throws java.io.IOException
    {
        java.io.FileInputStream fis = new java.io.FileInputStream(file);
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(PAGE_SIZE);
        java.nio.channels.ReadableByteChannel channel = java.nio.channels.Channels.newChannel(fis);

        int count = 0;
        while (count >= 0)
        {
            count = channel.read(buffer);
            if (count > 0 && !buffer.hasRemaining())
            {
                java.nio.ByteBuffer biggerBuffer = java.nio.ByteBuffer.allocate(buffer.limit() + PAGE_SIZE);
                biggerBuffer.put((java.nio.ByteBuffer) buffer.rewind());
                buffer = biggerBuffer;
            }
        }

        if (buffer != null)
            buffer.flip();

        return buffer;
    }

    private static java.nio.ByteBuffer memoryMapFile(java.io.File file) throws IOException
    {
        FileChannel roChannel = new RandomAccessFile(file, "r").getChannel();
        long fileSize = roChannel.size();
        MappedByteBuffer mapFile = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize);
        if (!mapFile.isLoaded())
            mapFile.load();
        roChannel.close();
        return mapFile;
    }
}