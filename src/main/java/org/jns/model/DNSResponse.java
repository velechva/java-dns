package org.jns.model;

import org.jns.util.BytesUtil;
import org.jns.util.TextUtil;
import org.jns.util.Tuple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DNSResponse {
    private final byte[] buf;

    private final List<Answer> answers;
    private final List<Question> questions;

    private int     questionCount;           // Number of questions asked
    private int     answerCount;             // Number of answers returned
    private int     queryOrResponse;         // Query or Response: Query = 0, Response = 1
    private boolean authoritativeAnswer;     // Authoritative Answer
    private int     returnCode;              // RCode
    private boolean truncated;               // Message has been truncated

    public DNSResponse(byte[] buf) {
        this.buf       = buf;
        this.answers   = new ArrayList<>();
        this.questions = new ArrayList<>();

        parseBuffer();
    }

    public String toString() {
        if (returnCode != 0x00) {
            return returnCodeErrorString(returnCode);
        }

        if (truncated) {
            return "ERROR: Truncated message";
        }

        StringBuilder str = new StringBuilder(queryOrResponse == 1 ? "Response\n" : "Query\n");
        str.append(authoritativeAnswer ? "Authoritative Answers:\n" : "Non-Authoritative Answers:\n");

        for (Answer ans : answers) {
            str.append("\t").append(ans.toString()).append("\n");
        }

        return str.toString();
    }

    private int parseHeader() {
        returnCode = buf[3] & 0xf;
        if (returnCode != 0x0) {
            // Error condition
            return -1;
        }

        truncated = BytesUtil.bitAtPosition(buf[2], 1) == 0x1;
        if (truncated) {
            // Message has been truncated
            return -1;
        }

        queryOrResponse     = BytesUtil.bitAtPosition(buf[2], 7);
        authoritativeAnswer = BytesUtil.bitAtPosition(buf[2], 2) == 0x1;
        questionCount       = BytesUtil.parseUInt16(buf, 4);
        answerCount         = BytesUtil.parseUInt16(buf, 6);

        return 12;
    }

    private void parseBuffer() {
        int index = parseHeader();

        if (index < 0) {
            return;
        }

        for (int i = 0; i < questionCount; i++) {
            index = parseQuestion(index);
        }

        for (int i = 0; i < answerCount; i++) {
            index = parseAnswer(index);
        }
    }

    private Map<Integer, Tuple<String, Integer>> parsedNamedByIndex = new HashMap<>();

    private Tuple<String, Integer> parseName(int index) {
        if (parsedNamedByIndex.containsKey(index)) {
            return parsedNamedByIndex.get(index);
        }

        Tuple<String, Integer> res = _parseName(index);
        parsedNamedByIndex.put(index, res);
        return res;
    }

    private Tuple<String, Integer> _parseName(int index) {
        String  name  = "";
        boolean first = true;

        while(true) {
            byte size = buf[index];
            String label;

            if (size == 0) {
                return new Tuple<>(name, index + 1);
            }
            if (isLabelReference(buf[index])) {
                byte[] pointerAddress = { (byte) (buf[index] & 0x3), buf[index + 1 ] };
                int pointerIndex = BytesUtil.parseUInt16(pointerAddress, 0);

                Tuple<String, Integer> referenceName = parseName(pointerIndex);

                if (!first) {
                    name += ".";
                }
                name += referenceName.first;

                return new Tuple<>(name, index + 2);
            }
            else {
                index++;
                label =  TextUtil.punycodeToUnicode(new String(buf, index, size));
                index += size;
            }

            if (!first) {
                name += ".";
            }
            name += label;

            first = false;
        }
    }

    private int parseQuestion(int index) {
        Question question = new Question();

        Tuple<String, Integer> nameResult = parseName(index);
        index         = nameResult.second;
        question.name = nameResult.first;

        byte[] type = new byte[2];
        type[0] = buf[index++];
        type[1] = buf[index++];
        question.type = type;

        byte[] clazz = new byte[2];
        clazz[0] = buf[index++];
        clazz[1] = buf[index++];
        question.clazz = clazz;

        questions.add(question);
        return index;
    }

    private int parseAnswer(int index) {
        Answer answer = new Answer();

        // Question reference
        Tuple<String, Integer> questionName = parseName(index);
        answer.questionName = questionName.first;
        index = questionName.second;

        // Type
        answer.type = new byte[2];
        answer.type[0] = buf[index];
        answer.type[1] = buf[index + 1];
        index += 2;

        // Class
        answer.clazz = new byte[2];
        answer.clazz[0] = buf[index];
        answer.clazz[1] = buf[index + 1];
        index += 7; // Skip TTL

        int dataLength = buf[index] & 0xff;
        index += 1;

        // CNAME
        if (answer.type[0] == 0x0 && answer.type[1] == 0x5) {
            Tuple<String, Integer> text = parseName(index);
            answer.text = text.first;
            index = text.second;
        }

        // A Record
        else if (answer.type[0] == 0x0 && answer.type[1] == 0x1) {
             answer.text = TextUtil.aRecordToString(buf, index, dataLength);
            index += dataLength;
        }

        answers.add(answer);
        return index;
    }

    private static String returnCodeErrorString(int code) {
        return switch (code) {
            case 0x01 -> "Format error - the name server was unable to interpret the query";
            case 0x02 -> "Server failure - the name server was unable to process this query due to a problen with the name server";
            case 0x03 -> "Name error - domain name referenced in the query does not exist";
            case 0x04 -> "Not implemented - the name server does not support the requested kind of query";
            case 0x05 -> "Refused - the name server refuses to perform the specified operation for policy reasons";
            default   -> "Unknown error - the return code is an unknown value: (" + code + ")";
        };
    }

    private static boolean isLabelReference(byte b) {
        // In a name reference, the first two digits should be 1's
        return (b & 0xC0) == 0xC0;
    }
}