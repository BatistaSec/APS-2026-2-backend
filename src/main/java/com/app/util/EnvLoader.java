package com.app.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Carrega as variáveis do arquivo {@code .env} (na raiz do projeto) e as
 * define como propriedades do sistema, para que o Hibernate e o
 * OpenWeatherClient as encontrem via {@code System.getProperty()}.
 *
 * <p>O arquivo {@code .env} fica no {@code .gitignore} e nunca vai para o
 * controle de versão, mantendo as credenciais (senha do banco, API key, etc.)
 * fora do repositório.</p>
 *
 * <p>Convenção de mapeamento: {@code DB_URL} &rarr; {@code db.url},
 * {@code OPENWEATHER_API_KEY} &rarr; {@code openweather.api.key}, etc.
 * (maiúsculas com underscore viram minúsculas com ponto).</p>
 */
public final class EnvLoader {

    private EnvLoader() {}

    public static void carregar() {
        carregar(Paths.get(".env"));
    }

    static void carregar(Path arquivoEnv) {
        if (!Files.exists(arquivoEnv)) {
            // .env é opcional — as credenciais podem vir do ambiente real (CI/CD, Docker, etc.)
            return;
        }
        try {
            for (String linha : Files.readAllLines(arquivoEnv)) {
                String trimmed = linha.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                int igual = trimmed.indexOf('=');
                if (igual <= 0) continue;

                String chave = trimmed.substring(0, igual).trim();
                String valor = trimmed.substring(igual + 1).trim();

                // Remove aspas envolventes, se houver
                if (valor.length() >= 2
                        && ((valor.startsWith("\"") && valor.endsWith("\""))
                        || (valor.startsWith("'") && valor.endsWith("'")))) {
                    valor = valor.substring(1, valor.length() - 1);
                }

                // DB_URL -> db.url  (convenção aceita pelo HibernateUtil e OpenWeatherClient)
                String propKey = chave.toLowerCase().replace('_', '.');

                // Não sobrescreve se já definido via -D na linha de comando
                if (System.getProperty(propKey) == null) {
                    System.setProperty(propKey, valor);
                }
                if (System.getProperty(chave) == null) {
                    System.setProperty(chave, valor);
                }
            }
        } catch (IOException e) {
            System.err.println("[EnvLoader] Aviso: não foi possível ler o .env: " + e.getMessage());
        }
    }
}
