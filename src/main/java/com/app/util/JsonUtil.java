package com.app.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class JsonUtil {

    private JsonUtil() {}

    public static String extrairTexto(String json, String chave) {
        if (json == null) return null;
        Pattern pattern = Pattern.compile("\"" + chave + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    public static Double extrairNumero(String json, String chave) {
        if (json == null) return null;
        Pattern pattern = Pattern.compile("\"" + chave + "\"\\s*:\\s*(-?\\d+(\\.\\d+)?)");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? Double.parseDouble(matcher.group(1)) : null;
    }

    public static Boolean extrairBooleano(String json, String chave) {
        if (json == null) return null;
        Pattern pattern = Pattern.compile("\"" + chave + "\"\\s*:\\s*(true|false)");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? Boolean.parseBoolean(matcher.group(1)) : null;
    }

    public static List<String> extrairListaTexto(String json, String chave) {
        List<String> itens = new ArrayList<>();
        if (json == null) return itens;

        Pattern arrayPattern = Pattern.compile("\"" + chave + "\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
        Matcher arrayMatcher = arrayPattern.matcher(json);
        if (arrayMatcher.find()) {
            String conteudo = arrayMatcher.group(1);
            Matcher itemMatcher = Pattern.compile("\"([^\"]*)\"").matcher(conteudo);
            while (itemMatcher.find()) {
                itens.add(itemMatcher.group(1).trim());
            }
        }
        return itens;
    }
}
