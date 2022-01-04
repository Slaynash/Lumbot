package slaynash.lum.bot.utils;

//source https://github.com/mikeyjay39/whoisclient

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class Whois {

    /**
     * Imports Whois servers from a txt file.
     * @return Whois Servers
     */
    static List<String> getServerList() {
        List<String> list = new ArrayList<>();

        try (
            BufferedReader br = new BufferedReader(new FileReader("WhoisServers.txt"))
        ) {
            String s;
            while ((s = br.readLine()) != null) {

                list.add(s);
            }
        }
        catch (Exception ignored) {
        }
        return list;
    }

    /**
     * Builds a HashMap of Whois servers.
     * key is the TLD
     * value is a list of Whois servers
     * @param list of Whois Servers
     * @return Map of Whois Servers
     */
    private static Map<String, ArrayList<String>> buildMap(List<String> list) {
        return list.stream()
            .collect(Collectors.toMap(
                (String s) -> s.split(" ")[0],
                (String s) -> {
                    ArrayList<String> aL1 = new ArrayList<>();
                    aL1.add(s.split(" ")[1]);
                    return aL1;
                },
                (s1, s2) -> {
                    ArrayList<String> aL2 = new ArrayList<>(s1);
                    aL2.add(s2.get(0));
                    return aL2;
                },
                    HashMap::new));
    }

    /**
     * Method that queries Whois via sockets connection.
     * @param domain Domain and TLD. No subdomains allowed
     * @param hostName the target Whois server
     * @return Whois results
     */
    private static String whoisQuery(String domain, String hostName) {
        int port = 43;
        StringBuilder result = new StringBuilder();
        try (
                Socket echoSocket = new Socket(hostName, port);
                PrintWriter out =
                        new PrintWriter(echoSocket.getOutputStream(), true);
                BufferedReader in =
                        new BufferedReader(
                                new InputStreamReader(echoSocket.getInputStream()))
        ) {
            out.println(domain);
            String s;
            while ((s = in.readLine()) != null) {
                result.append(s).append("\n");
            }
        }
        catch (Exception ignored) {
        }
        return result.toString();
    }


    /**
     * Main Whois call method.
     * @param domain Domain and TLD. No subdomains allowed
     * @return Whois results
     */
    public static String whois(String domain) {
        List<String> servers = getWhoisServers(getTLD(domain));
        int noServers = servers.size();
        String result = "";
        for (int i = 0; i < noServers && result.isBlank(); i++) {
            result = whois(domain, servers.get(i));
            if (result.trim().equalsIgnoreCase("no domain"))
                result = "";
        }
        return result;
    }

    /**
     * Whois that allows you to specify target Whois server.
     * @param domain Domain and TLD. No subdomains allowed
     * @param hostname is the target Whois server
     * @return Whois results
     */
    public static String whois(String domain, String hostname) {
        return whoisQuery(domain, hostname);
    }

    /**
     * get domain's TLD.
     * @param domain Domain and TLD. No subdomains allowed
     * @return TLD
     */
    static String getTLD(String domain) {
        return domain.replaceFirst(".*\\.", "");
    }

    /**
     * Returns a list of Whois servers for a specific TLD.
     * @param tld TLD
     * @return Whois server
     */
    static List<String> getWhoisServers(String tld) {
        List<String> list = getServerList();
        Map<String, ArrayList<String>> map = buildMap(list);
        return map.get(tld);
    }
}
