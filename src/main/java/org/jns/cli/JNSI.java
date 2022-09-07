package org.jns.cli;

import org.jns.lib.DNSService;
import org.jns.ex.ApplicationException;
import org.jns.model.Config;
import org.jns.model.DNSResponse;
import org.jns.util.IOUtil;

import java.util.*;

/**
 * Java Naming Server Interface
 */
public class JNSI {
    // Command names
    private static final String CMD_QUERY = "query";
    private static final String CMD_HELP  = "help";

    // CLI flag keys
    private static final String FLAG_DNS_PORT  = "dnsport";
    private static final String FLAG_DNS_HOST  = "dnshost";
    private static final String FLAG_AUTH_ONLY = "authonly";
    private static final String FLAG_VERBOSE   = "verbose";

    // CLI flag values
    private static final String FLAG_VAL_TRUE  = "true";

    private static void printHelp() {
        System.out.println(IOUtil.readFileFromResources("doc/help.txt"));
    }

    /**
     * Parse CLI arguments into flags and positional values
     *
     * ex.
     * INPUT:
     *      args = ["www.google.com", "-flag", "value", "another.positional"]
     * OUTPUT:
     *      positional = [ "www.google.com", "another.positional" ]
     *      flags      = { "flag" : "value" }
     *
     * @param args       IN  - list of cli args
     * @param flags      OUT - key value pair of -flag and value terms
     * @param positional OUT - any args which aren't preceded by a -flag
     */
    private static void parseFlags(String [] args, Map<String, String> flags, List<String> positional) {
        for(int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (arg.startsWith("-")) {
                if (i + 1 >= args.length || args[i + 1].startsWith("-")) {
                    throw new ApplicationException("Invalid flag at index " + i);
                }

                if (arg.length() < 2 || arg.charAt(1) == '-') {
                    throw new ApplicationException("Invalid flag at index " + i);
                }

                flags.put(arg.substring(1), args[i + 1]);
                i++;
            }
            else {
                positional.add(arg);
            }
        }
    }

    public static void main(String [] args) {
        if (args.length == 0) {
            printHelp();
            return;
        }

        Map<String, String> flags        = new HashMap<>();
        List<String>        positional   = new ArrayList<>();
        parseFlags(args, flags, positional);

        if (positional.isEmpty()) {
            throw new ApplicationException("Missing command");
        }

        String command = positional.get(0);
        Config config = new Config();
        
        if (flags.containsKey(FLAG_DNS_PORT)) {
            int port = Integer.parseInt(flags.get(FLAG_DNS_PORT));
            if (port < 1) {
                throw new ApplicationException("Invalid value for dns-port");
            }
            
            config.port = port;
        }
        
        if (flags.containsKey(FLAG_DNS_HOST)) {
            config.host = flags.get(FLAG_DNS_HOST);
        }

        if (flags.containsKey(FLAG_AUTH_ONLY)) {
            config.authoritativeAnswer = FLAG_VAL_TRUE.equalsIgnoreCase(flags.get(FLAG_AUTH_ONLY));
        }

        if (flags.containsKey(FLAG_VERBOSE)) {
            config.verbose = FLAG_VAL_TRUE.equalsIgnoreCase(flags.get(FLAG_VERBOSE));
        }

        if (CMD_QUERY.equals(command)) {
            if (positional.size() < 2) {
                throw new ApplicationException("Missing host name");
            }

            DNSResponse res = DNSService.query(config, positional.subList(1, positional.size()));

            if (res == null) {
                System.out.println("Failed to get a response from the DNS server");
                return;
            }

            System.out.println(res);
        }
        else if (CMD_HELP.equals(command)) {
            printHelp();
        }
        else {
            System.out.println("Unknown command. Use the 'help' command for more information");
        }
    }
}