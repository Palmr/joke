/*
 * Copyright (c) 1998-2017 Kx Systems Inc.
 * Modifications copyright (C) 2024 Nick Palmer
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.co.palmr.joke;

import uk.co.palmr.joke.messages.AuthenticateResponse;
import uk.co.palmr.joke.messages.KdbMessageHeader;
import uk.co.palmr.joke.types.MessageType;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;


public class KdbClient implements AutoCloseable {
    private static final String DEFAULT_STRING_ENCODING = "ISO-8859-1";
    private static final int KDB_IPC_VERSION = 3;
    public static final int DEFAULT_BUFFER_SIZE = 4096;

    private final SocketChannel socketChannel;
    private final KdbProtocol kdbProtocol;
    private final ByteBuffer messageBuffer;
    private final AuthenticateResponse authenticateResponse;
    private final KdbMessageHeader kdbMessageHeader;

    /**
     * Initializes a new {@link KdbClient} instance and connects to KDB+ over TCP.
     *
     * @param hostname Host of remote q process
     * @param port     Port of remote q process
     * @param username Username for remote authorization
     * @param password Password for remote authorization
     * @throws KdbException if access denied
     * @throws IOException  if an I/O error occurs.
     */
    public KdbClient(final String hostname, final int port, final String username, final String password) throws IOException, KdbException {
        this(hostname, port, username, password, false, DEFAULT_STRING_ENCODING, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Initializes a new {@link KdbClient} instance and connects to KDB+ over TCP with optional TLS support for encryption.
     *
     * @param hostname         Host of remote q process
     * @param port             Port of remote q process
     * @param username         Username for remote authorization
     * @param password         Password for remote authorization
     * @param allowCompression consider compression on outgoing messages (given uncompressed serialized data also has a
     *                         length greater than 2000 bytes and connection is not localhost)
     * @param stringEncoding   character encoding to use when [de]-serializing strings
     * @param bufferSize       size of the data buffer
     * @throws KdbException if access denied
     * @throws IOException  if an I/O error occurs.
     * @see <a href="https://code.kx.com/q/ref/ipc/#compression">IPC compression</a>
     */
    public KdbClient(final String hostname, final int port, final String username, final String password, final boolean allowCompression, final String stringEncoding, final int bufferSize) throws IOException, KdbException {
        this.messageBuffer = ByteBuffer.allocate(bufferSize);
        this.authenticateResponse = new AuthenticateResponse(messageBuffer);
        this.kdbMessageHeader = new KdbMessageHeader(messageBuffer);

        final var inetSocketAddress = new InetSocketAddress(hostname, port);
        socketChannel = SocketChannel.open(inetSocketAddress);
        socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
        socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);

        final var compressionAllowed = allowCompression && !isLoopback(inetSocketAddress.getAddress());
        kdbProtocol = new KdbProtocol(stringEncoding, compressionAllowed);

        handshake(username, password, kdbProtocol);
    }

    /**
     * Sends a sync message to the remote kdb+ process. This blocks until the message has been sent in full, and a message
     * is received from the remote; typically the received message would be the corresponding response message.
     *
     * @param expr The expression to send
     * @return deserialised response to request {@code x}
     * @throws KdbException if request evaluation resulted in an error
     * @throws IOException  if an I/O error occurs.
     */
    public Object send(final String expr) throws KdbException, IOException {
        assert KdbClientThreadAssertion.isSameThread(this) : KdbClientThreadAssertion.buildMessage(this);

        return sendSync(expr.toCharArray());
    }

    /**
     * Sends a sync message to the remote kdb+ process. This blocks until the message has been sent in full, and, if a MsgHandler
     * is set, will process any queued, incoming async or sync message in order to reach the response message.
     * If the caller has already indicated via {@code setCollectResponseAsync} that the response message will be read async, later, then return
     * without trying to read any messages at this point; the caller can collect(read) the response message by calling readMsg();
     *
     * @param x The object to send
     * @return deserialised response to request {@code x}
     * @throws KdbException if request evaluation resulted in an error
     * @throws IOException  if an I/O error occurs.
     */
    private Object sendSync(Object x) throws KdbException, IOException {
        sendSyncMessage(x);

        readFromKdb(KdbMessageHeader.SIZE);
        messageBuffer.order(kdbMessageHeader.getByteOrder());

        assert kdbMessageHeader.getMessageType() == MessageType.response : "Expected response type message when sync message sent";

        readFromKdb(kdbMessageHeader.getMessageSize());

        return kdbProtocol.deserialize(kdbMessageHeader, messageBuffer);
    }

    private void sendSyncMessage(final Object x) throws IOException, KdbException {
        resetBuffer();
        kdbProtocol.serialiseMessage(MessageType.sync, x, kdbMessageHeader, messageBuffer);
        sendToKdb();
    }

    private void handshake(final String username, final String password, final KdbProtocol kdbProtocol) throws IOException, KdbException {
        var usernamepassword = username + ":" + password;
        kdbProtocol.writeStringToBuffer(usernamepassword + (char) KDB_IPC_VERSION, messageBuffer);
        sendToKdb();

        resetBuffer();

        try {
            readFromKdb(AuthenticateResponse.SIZE);
            kdbProtocol.setVersion(Math.min(authenticateResponse.getVersion(), KDB_IPC_VERSION));
        } catch (IOException e) {
            close();
            throw new KdbException("Access Denied");
        }
    }

    private void sendToKdb() throws IOException {
        socketChannel.write(messageBuffer.flip());
        resetBuffer();
    }

    private void resetBuffer() {
        messageBuffer.limit(messageBuffer.capacity());
        messageBuffer.compact();
        messageBuffer.clear();
    }

    private void readFromKdb(final int limit) throws IOException {
        messageBuffer.limit(limit);

        while (messageBuffer.hasRemaining()) {
            if (-1 == socketChannel.read(messageBuffer)) {
                throw new EOFException("end of stream");
            }
        }
    }

    private static boolean isLoopback(InetAddress addr) {
        return addr.isAnyLocalAddress() || addr.isLoopbackAddress();
    }

    @Override
    public void close() throws IOException {
        socketChannel.close();
    }


    private static class KdbClientThreadAssertion {
        static final Map<KdbClient, Thread> CLIENTS = new HashMap<>();

        protected static boolean isSameThread(final KdbClient kdbClient) {
            synchronized (CLIENTS) {
                final Thread currentThread = Thread.currentThread();
                if (!CLIENTS.containsKey(kdbClient)) {
                    CLIENTS.put(kdbClient, currentThread);
                }
                return CLIENTS.get(kdbClient).equals(currentThread);
            }
        }

        public static String buildMessage(final KdbClient kdbClient) {
            return "KdbClient is not thread safe, but accessed by two threads.\n" +
                    "First use thread: " + CLIENTS.get(kdbClient) + "\n" +
                    "But now thread: " + Thread.currentThread();
        }
    }
}
