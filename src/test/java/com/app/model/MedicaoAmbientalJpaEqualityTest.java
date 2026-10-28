package com.app.model;

import org.hibernate.Hibernate;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class MedicaoAmbientalJpaEqualityTest {

    @Test
    @DisplayName("JPA Equality: Instância deve ser igual a si mesma (reflexividade)")
    void deveSerIgualASiMesmo() {
        MedicaoAmbiental m = new MedicaoAmbiental();
        assertEquals(m, m);

        m.setId(10L);
        assertEquals(m, m);
    }

    @Test
    @DisplayName("JPA Equality: Não deve ser igual a nulo ou objeto de outra classe")
    void naoDeveSerIgualANuloOuOutraClasse() {
        MedicaoAmbiental m = new MedicaoAmbiental();
        m.setId(10L);

        assertNotEquals(m, null);
        assertNotEquals(m, "TextoQualquer");
    }

    @Test
    @DisplayName("JPA Equality: Duas instâncias transientes distintas (id nulo) não devem ser iguais")
    void transientesDistintosNaoDevemSerIguais() {
        MedicaoAmbiental m1 = new MedicaoAmbiental();
        MedicaoAmbiental m2 = new MedicaoAmbiental();

        assertNotEquals(m1, m2);
        assertNotEquals(m2, m1);
    }

    @Test
    @DisplayName("JPA Equality: Duas instâncias com o mesmo ID devem ser iguais e ter o mesmo hashCode")
    void instanciasComMesmoIdDevemSerIguaisETerMesmoHashCode() {
        MedicaoAmbiental m1 = new MedicaoAmbiental();
        m1.setId(42L);
        m1.setCidade("Curitiba");

        MedicaoAmbiental m2 = new MedicaoAmbiental();
        m2.setId(42L);
        m2.setCidade("Tokyo");

        assertEquals(m1, m2, "Entidades persistidas com o mesmo ID são consideradas iguais");
        assertEquals(m2, m1);
        assertEquals(m1.hashCode(), m2.hashCode(), "Contrato hashCode deve coincidir quando equals é true");
    }

    @Test
    @DisplayName("JPA Equality: Instâncias com IDs diferentes não devem ser iguais")
    void instanciasComIdsDiferentesNaoDevemSerIguais() {
        MedicaoAmbiental m1 = new MedicaoAmbiental();
        m1.setId(1L);

        MedicaoAmbiental m2 = new MedicaoAmbiental();
        m2.setId(2L);

        assertNotEquals(m1, m2);
    }

    @Test
    @DisplayName("JPA Equality: Estabilidade no HashSet antes e depois da persistência (transição transient -> persisted)")
    void estabilidadeEmHashSetAntesEDepoisPersistencia() {
        Set<MedicaoAmbiental> set = new HashSet<>();
        MedicaoAmbiental m = new MedicaoAmbiental();
        m.setCidade("Londres");

        // 1. Adiciona estado transiente (id == null)
        set.add(m);
        assertTrue(set.contains(m), "Set deve conter o objeto transiente");

        // 2. Simula persistência no banco (IDENTITY atribui ID)
        m.setId(100L);

        // 3. Verifica estabilidade: hashCode constante evita perda do bucket no HashMap/HashSet
        assertTrue(set.contains(m), "Set deve continuar encontrando o objeto após atribuição de ID");
        assertEquals(1, set.size());
    }

    @Test
    @DisplayName("JPA Equality: Comparação bidirecional com Proxy do Hibernate")
    void compatibilidadeComHibernateProxy() {
        MedicaoAmbiental real = new MedicaoAmbiental();
        real.setId(55L);
        real.setCidade("Paris");

        MockHibernateProxy proxy = new MockHibernateProxy(55L);

        // Verifica que o proxy expõe a classe persistente correta sem inicializar
        assertEquals(MedicaoAmbiental.class, proxy.getHibernateLazyInitializer().getPersistentClass());

        // Igualdade simétrica entre instância real e proxy
        assertEquals(real, proxy, "Instância real deve ser igual ao Proxy com mesmo ID");
        assertEquals(proxy, real, "Proxy deve ser igual à Instância real com mesmo ID");
        assertEquals(real.hashCode(), proxy.hashCode(), "HashCode do Proxy deve ser idêntico ao da entidade");

        // Proxy com ID diferente não deve ser igual
        MockHibernateProxy outroProxy = new MockHibernateProxy(99L);
        assertNotEquals(real, outroProxy);
        assertNotEquals(proxy, outroProxy);
    }

    // Mock simples que emula a estrutura de um HibernateProxy do ByteBuddy
    private static class MockHibernateProxy extends MedicaoAmbiental implements HibernateProxy {
        private final Long proxyId;

        public MockHibernateProxy(Long proxyId) {
            this.proxyId = proxyId;
            super.setId(proxyId);
        }

        @Override
        public Long getId() {
            return proxyId;
        }

        @Override
        public LazyInitializer getHibernateLazyInitializer() {
            return new org.hibernate.proxy.AbstractLazyInitializer(MedicaoAmbiental.class.getName(), proxyId, null) {
                @Override
                public Class<?> getPersistentClass() {
                    return MedicaoAmbiental.class;
                }

                @Override
                public Class<?> getImplementationClass() {
                    return MedicaoAmbiental.class;
                }
            };
        }

        @Override
        public Object writeReplace() {
            return this;
        }
    }
}
