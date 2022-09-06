package br.uefs.larsid.dlt.iot.soft.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author Uellington Damasceno
 */
public class CLI {

    public static Optional<String> getEndpoint(String... args) {
        return getArgInList("-ep", args);
    }

    public static Optional<String> getCredentialDefinitionId(String... args) {
        return getArgInList("-cd", args);
    }

    public static Optional<String> getBrokerIp(String... args) {
        return getArgInList("-bi", args);
    }
    
    public static Optional<String> getBrokerPort(String... args){
        return getArgInList("-bp", args);
    }
    
    public static Optional<String> getBrokerPassword(String... args){
        return getArgInList("-pw", args);
    }
    
    public static Optional<String> getBrokerUsername(String... args){
        return getArgInList("-us", args);
    }

    public static Optional<String> getAgentIp(String... args) {
        return getArgInList("-ai", args);
    }
    
    public static Optional<String> getAgentPort(String... args){
        return getArgInList("-ap", args);
    }
    
    public static Optional<String> getTimeout(String... args){
        return getArgInList("-to", args);
    }
    
    public static boolean hasParam(String arg, String... args){
        return Arrays.asList(args).indexOf(arg) != -1;
    }

    private static Optional<String> getArgInList(String arg, String... args) {
        List<String> largs = Arrays.asList(args);
        int index = largs.indexOf(arg);
        return (index == -1) ? Optional.empty() : Optional.of(largs.get(index + 1));
    }
}
