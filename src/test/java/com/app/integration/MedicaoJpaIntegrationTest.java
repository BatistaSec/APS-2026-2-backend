package com.app.integration;

import com.app.dao.consulta.ConsultaMedicaoDao;
import com.app.dao.consulta.ConsultaMedicaoDaoImpl;
import com.app.dao.exclusao.DeletarMedicaoDao;
import com.app.dao.exclusao.DeletarMedicaoDaoImpl;
import com.app.dao.salvar.SalvarMedicaoDao;
import com.app.dao.salvar.SalvarMedicaoDaoImpl;
import com.app.model.MedicaoAmbiental;
import com.app.model.dto.response.EstatisticasClimaticas;
import com.app.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MedicaoJpaIntegrationTest {

    private static boolean bancoDisponivel = false;
    private static Long idPersistido;

    private final SalvarMedicaoDao salvarDao = new SalvarMedicaoDaoImpl();
    private final ConsultaMedicaoDao consultaDao = new ConsultaMedicaoDaoImpl();
    private final DeletarMedicaoDao deletarDao = new DeletarMedicaoDaoImpl();

    @BeforeAll
    static void verificarAmbienteBanco() {
        String dbUrl = System.getenv("DB_URL");
        if (dbUrl == null || dbUrl.isBlank()) dbUrl = System.getProperty("db.url");

        String dbUser = System.getenv("DB_USER");
        if (dbUser == null || dbUser.isBlank()) dbUser = System.getProperty("db.user");

        String dbPassword = System.getenv("DB_PASSWORD");
        if (dbPassword == null) dbPassword = System.getProperty("db.password");

        boolean credenciaisInformadas = dbUrl != null && !dbUrl.isBlank()
                && dbUser != null && !dbUser.isBlank()
                && dbPassword != null;

        if (credenciaisInformadas) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("localhost", 3306), 1000);
                bancoDisponivel = true;
            } catch (Exception ignored) {
                bancoDisponivel = false;
            }
        } else {
            bancoDisponivel = false;
        }
    }

    @BeforeEach
    void checarCondicao() {
        Assumptions.assumeTrue(bancoDisponivel,
                "Teste de integração Hibernate ignorado: DB_URL, DB_USER e DB_PASSWORD não configurados ou MySQL inacessível (Docker daemon offline).");
    }

    @Test
    @Order(1)
    @DisplayName("Ciclo de Vida 1: Persistência de entidade transiente e geração de ID via IDENTITY")
    void devePersistirEntidadeComIdGerado() {
        MedicaoAmbiental m = new MedicaoAmbiental();
        m.setEstacaoId("INTEG-TEST-01");
        m.setCidade("CidadeIntegracao");
        m.setDataHora(LocalDateTime.now());
        m.setTemperatura(23.4);
        m.setUmidade(65.0);
        m.setPrecipitacao(false);
        m.setVelocidadeVento(12.0);
        m.setSensacaoTermica(24.0);

        assertNull(m.getId(), "Antes da persistência, o ID deve ser nulo (transient)");

        MedicaoAmbiental salva = salvarDao.salvar(m);

        assertNotNull(salva.getId(), "Após persistência, o ID deve ser gerado pelo banco");
        idPersistido = salva.getId();
    }

    @Test
    @Order(2)
    @DisplayName("Ciclo de Vida 2: Recuperação por ID e validação de propriedades persistidas")
    void deveRecuperarEntidadePorId() {
        assertNotNull(idPersistido, "ID da medição persistida deve existir");

        Optional<MedicaoAmbiental> opt = consultaDao.buscarPorId(idPersistido);
        assertTrue(opt.isPresent(), "Medição persistida deve ser encontrada por ID");

        MedicaoAmbiental recuperada = opt.get();
        assertEquals(idPersistido, recuperada.getId());
        assertEquals("CidadeIntegracao", recuperada.getCidade());
        assertEquals(23.4, recuperada.getTemperatura(), 0.01);
        assertEquals(65.0, recuperada.getUmidade(), 0.01);
    }

    @Test
    @Order(3)
    @DisplayName("Ciclo de Vida 3: Transição de estado (update) e ordenação determinística das mais recentes")
    void deveAtualizarEstadoEConsultarOrdenacaoRecente() {
        assertNotNull(idPersistido);

        // Atualiza estado gerenciado
        SessionFactory sf = HibernateUtil.getSessionFactory();
        try (Session session = sf.openSession()) {
            var tx = session.beginTransaction();
            MedicaoAmbiental m = session.find(MedicaoAmbiental.class, idPersistido);
            assertNotNull(m);
            m.setTemperatura(28.0);
            tx.commit();
        }

        // Verifica ordenação determinística por data_hora DESC e id DESC
        List<MedicaoAmbiental> recentes = consultaDao.listarPorCidade("CidadeIntegracao", 10);
        assertFalse(recentes.isEmpty());
        assertEquals(idPersistido, recentes.get(0).getId());
        assertEquals(28.0, recentes.get(0).getTemperatura(), 0.01);

        // Valida cálculo agregado considerando a alteração
        EstatisticasClimaticas stats = consultaDao.calcularEstatisticasAgregadas("CidadeIntegracao", 10);
        assertEquals(1, stats.totalAnalisadas());
        assertEquals(28.0, stats.tempMax(), 0.01);
    }

    @Test
    @Order(4)
    @DisplayName("Ciclo de Vida 4: Exclusão física por ID e verificação de não existência")
    void deveExcluirEntidadePorId() {
        assertNotNull(idPersistido);

        boolean removido = deletarDao.deletarPorId(idPersistido);
        assertTrue(removido, "Deleção por ID deve retornar true");

        Optional<MedicaoAmbiental> opt = consultaDao.buscarPorId(idPersistido);
        assertTrue(opt.isEmpty(), "Medição deletada não deve mais existir no banco");
    }

    @AfterAll
    static void limparDadosDeTeste() {
        if (bancoDisponivel && idPersistido != null) {
            try {
                new DeletarMedicaoDaoImpl().deletarPorId(idPersistido);
            } catch (Exception ignored) {}
        }
    }
}
