package org.jns.lib;

import org.jns.ex.ApplicationException;
import org.jns.model.DNSResponse;
import org.jns.model.Config;
import org.jns.util.TextUtil;

import java.io.IOException;

import java.net.*;

import java.util.List;
import java.util.Random;

/**
 * <a href="https://mislove.org/teaching/cs4700/spring11/handouts/project1-primer.pdf">Reference for DNS Packet Spec</a>
 */
public class DNSService {
    private static int addQuestionToQueryRequest(String hostName, byte[] buf, int index) {
        String[] parts = hostName.split("\\.");

        if (parts.length == 0) {
            throw new ApplicationException("Invalid host name");
        }

        // For each octet (www, google, com)
        for (String part : parts) {
            // Octet length
            buf[index++] = (byte) part.length();

            // Convert Unicode characters to Punycode encoding
            char[] characters = TextUtil.unicodeToPunyCode(part).toCharArray();

            // Octet characters
            for (char c : characters) {
                buf[index++] = (byte) c;
            }
        }

        // Query name terminator
        buf[index++] = 0x00;

        // Type: A (Host Address)
        buf[index++] = 0x00;
        buf[index++] = 0x01;

        // Class: IN
        buf[index++] = 0x00;
        buf[index++] = 0x01;

        return index;
    }

    /**
     * Generates a request packet to query the DNS server for a given host name
     *
     * @param config
     * @param questions
     * @return
     */
    private static byte[] queryRequest(Config config, List<String> questions) {
        byte[] buf = new byte[256];
        int    index = 0;

        // Transaction ID
        byte[] id  = new byte[2];
        new Random().nextBytes(id);
        buf[index++] = id[0];
        buf[index++] = id[1];

        // Flags for: QR, Opcode, AA, TC, RD
        byte flagsLeft = 0x01;
        if (config.authoritativeAnswer) {
            flagsLeft |= 0x04;
        }
        buf[index++] = flagsLeft;

        // Flags for: RA, Z, RCODE
        buf[index++] = 0x00;

        // # of Questions: 1
        buf[index++] = 0x00;
        buf[index++] = (byte) questions.size();

        // # of Answers: 0
        buf[index++] = 0x00;
        buf[index++] = 0x00;

        // # of Authority RRs: 0
        buf[index++] = 0x00;
        buf[index++] = 0x00;

        // # of Additional RRs: 0
        buf[index++] = 0x00;
        buf[index++] = 0x00;

        for (String hostName : questions) {
            index = addQuestionToQueryRequest(hostName, buf, index);
        }

        return buf;
    }

    public static DNSResponse query(Config config, List<String> hostNames) {
        try (DatagramSocket socket = new DatagramSocket(config.port)){
            InetAddress iaddr = InetAddress.getByName(config.host);

            byte[] request = queryRequest(config, hostNames);
            DatagramPacket requestPacket = new DatagramPacket(request, request.length);
            requestPacket.setAddress(iaddr);
            requestPacket.setPort(config.port);

            socket.send(requestPacket);

            byte[] responseBuf = new byte[512];
            DatagramPacket responsePacket = new DatagramPacket(responseBuf, responseBuf.length);

            socket.receive(responsePacket);

            return new DNSResponse(responsePacket.getData());
        }
        catch (UnknownHostException ex) {
            System.out.println("UnknownHostException:");
            System.out.println(ex.getMessage());
        }
        catch (IOException ex) {
            System.out.println("IOException:");
            System.out.println(ex.getMessage());
        }

        return null;
    }
}