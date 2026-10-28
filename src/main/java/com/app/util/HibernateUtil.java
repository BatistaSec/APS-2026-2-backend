package com.app.util;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

public class HibernateUtil {
    private static StandardServiceRegistry registry;
    private static SessionFactory sessionFactory;

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            synchronized (HibernateUtil.class) {
                if (sessionFactory == null) {
                    try {
                        StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder().configure();

                        String dbUrl = System.getenv("DB_URL");
                        if (dbUrl == null || dbUrl.isBlank()) dbUrl = System.getProperty("db.url");
                        // Se ainda null, deixa o XML fornecer o valor (hibernate.connection.url)

                        String dbUser = System.getenv("DB_USER");
                        if (dbUser == null || dbUser.isBlank()) dbUser = System.getProperty("db.user");

                        String dbPassword = System.getenv("DB_PASSWORD");
                        if (dbPassword == null) dbPassword = System.getProperty("db.password");

                        // Só sobrescreve as configurações do XML se as variáveis de ambiente estiverem definidas
                        if (dbUrl != null && !dbUrl.isBlank()) {
                            registryBuilder.applySetting(AvailableSettings.JAKARTA_JDBC_URL, dbUrl);
                        }
                        if (dbUser != null && !dbUser.isBlank()) {
                            registryBuilder.applySetting(AvailableSettings.JAKARTA_JDBC_USER, dbUser);
                        }
                        if (dbPassword != null) {
                            registryBuilder.applySetting(AvailableSettings.JAKARTA_JDBC_PASSWORD, dbPassword);
                        }

                        registry = registryBuilder.build();


                        MetadataSources sources = new MetadataSources(registry);
                        Metadata metadata = sources.getMetadataBuilder().build();

                        sessionFactory = metadata.getSessionFactoryBuilder().build();
                        System.out.println("[+] Hibernate SessionFactory (MySQL) inicializada com sucesso.");
                    } catch (Exception e) {
                        System.err.println("[-] Falha ao inicializar Hibernate SessionFactory: " + e.getMessage());
                        if (registry != null) {
                            StandardServiceRegistryBuilder.destroy(registry);
                        }
                        throw new ExceptionInInitializerError(e);
                    }
                }
            }
        }
        return sessionFactory;
    }

    public static void shutdown() {
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
        }
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
            System.out.println("[+] Hibernate SessionFactory encerrada.");
        }
    }
}
