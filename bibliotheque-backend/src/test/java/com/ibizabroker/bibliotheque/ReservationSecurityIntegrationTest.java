package com.ibizabroker.bibliotheque;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Reservation;
import com.ibizabroker.bibliotheque.entity.ReservationStatus;
import com.ibizabroker.bibliotheque.entity.Role;
import com.ibizabroker.bibliotheque.entity.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration (contexte Spring complet + MockMvc + Postgres via Testcontainers)
 * des regles de securite RS-01 a RS-05 du module reservation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class ReservationSecurityIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private BooksRepository booksRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private Users adherent1;
    private Users adherent2;
    private Books book;
    private Books freeBook;
    private Reservation reservationAdherent1;

    @BeforeEach
    void setUp() {
        Role roleUser = new Role();
        roleUser.setRoleName("User");

        Role roleAdmin = new Role();
        roleAdmin.setRoleName("Admin");

        adherent1 = new Users();
        adherent1.setUsername("adherent1");
        adherent1.setName("Adherent Un");
        adherent1.setPassword("placeholder");
        adherent1.setRole(Set.of(roleUser));
        adherent1 = usersRepository.save(adherent1);

        Role roleUser2 = new Role();
        roleUser2.setRoleName("User");
        adherent2 = new Users();
        adherent2.setUsername("adherent2");
        adherent2.setName("Adherent Deux");
        adherent2.setPassword("placeholder");
        adherent2.setRole(Set.of(roleUser2));
        adherent2 = usersRepository.save(adherent2);

        book = new Books();
        book.setBookName("Test Book");
        book.setBookAuthor("Author");
        book.setNoOfCopies(0);
        book = booksRepository.save(book);

        freeBook = new Books();
        freeBook.setBookName("Free Book");
        freeBook.setBookAuthor("Author");
        freeBook.setNoOfCopies(0);
        freeBook = booksRepository.save(freeBook);

        reservationAdherent1 = new Reservation();
        reservationAdherent1.setBookId(book.getBookId());
        reservationAdherent1.setUserId(adherent1.getUserId());
        reservationAdherent1.setStatus(ReservationStatus.EN_ATTENTE);
        reservationAdherent1 = reservationRepository.save(reservationAdherent1);

        Reservation reservationAdherent2 = new Reservation();
        reservationAdherent2.setBookId(book.getBookId());
        reservationAdherent2.setUserId(adherent2.getUserId());
        reservationAdherent2.setStatus(ReservationStatus.EN_ATTENTE);
        reservationRepository.save(reservationAdherent2);
    }

    @Test
    @DisplayName("RS-01 : GET /api/reservations sans token renvoie 401")
    void shouldReturn401WhenNoTokenOnReservationsList() throws Exception {
        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RS-05 : un ADHERENT authentifie ne voit que ses propres reservations")
    @WithMockUser(username = "adherent1", roles = "User")
    void shouldReturn200AndOnlyOwnReservationsWhenAdherentAuthenticated() throws Exception {
        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(adherent1.getUserId()));
    }

    @Test
    @DisplayName("RS-03 : un ADHERENT peut consulter sa propre reservation")
    @WithMockUser(username = "adherent1", roles = "User")
    void shouldReturn200WhenAdherentAccessesOwnReservationById() throws Exception {
        mockMvc.perform(get("/api/reservations/" + reservationAdherent1.getReservationId()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("RS-03 : un ADHERENT qui consulte la reservation d'un autre recoit 403 avec un message explicite")
    @WithMockUser(username = "adherent2", roles = "User")
    void shouldReturn403WhenAdherentAccessesAnotherUsersReservationById() throws Exception {
        mockMvc.perform(get("/api/reservations/" + reservationAdherent1.getReservationId()))
                .andExpect(status().isForbidden())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("consulter cette réservation")));
    }

    @Test
    @DisplayName("RS-03 : un ADHERENT peut annuler sa propre reservation")
    @WithMockUser(username = "adherent1", roles = "User")
    void shouldReturn200WhenAdherentCancelsOwnReservation() throws Exception {
        mockMvc.perform(patch("/api/reservations/" + reservationAdherent1.getReservationId() + "/annuler"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("RS-03 : un ADHERENT qui annule la reservation d'un autre recoit 403")
    @WithMockUser(username = "adherent2", roles = "User")
    void shouldReturn403WhenAdherentCancelsAnotherUsersReservation() throws Exception {
        mockMvc.perform(patch("/api/reservations/" + reservationAdherent1.getReservationId() + "/annuler"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RS-02 : DELETE /api/reservations/{id} par un ADHERENT renvoie 403")
    @WithMockUser(username = "adherent1", roles = "User")
    void shouldReturn403WhenAdherentTriesToDelete() throws Exception {
        mockMvc.perform(delete("/api/reservations/" + reservationAdherent1.getReservationId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RS-02 : DELETE /api/reservations/{id} par un BIBLIOTHECAIRE renvoie 204")
    @WithMockUser(username = "biblio", roles = "Admin")
    void shouldReturn204WhenBibliothecaireDeletes() throws Exception {
        mockMvc.perform(delete("/api/reservations/" + reservationAdherent1.getReservationId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("RS-04 : un ADHERENT qui fournit l'id d'un autre adherent recoit 403 avec un message explicite")
    @WithMockUser(username = "adherent1", roles = "User")
    void shouldReturn403WhenAdherentCreatesReservationForAnotherAdherent() throws Exception {
        String body = objectMapper.writeValueAsString(new ReservationRequestBody(freeBook.getBookId(), adherent2.getUserId()));

        mockMvc.perform(post("/api/reservations")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("au nom d'un autre adhérent")));
    }

    @Test
    @DisplayName("RS-01 : DELETE /api/reservations/{id} sans token renvoie 401, pas 403")
    void shouldReturn401NotForbiddenWhenNoTokenOnDelete() throws Exception {
        // Verifie l'ordre des controles : l'absence d'authentification doit etre
        // detectee avant meme que la regle @PreAuthorize("hasRole('Admin')") ne
        // s'evalue - sinon un anonyme recevrait a tort un 403 (droits insuffisants)
        // au lieu d'un 401 (identite inconnue).
        mockMvc.perform(delete("/api/reservations/" + reservationAdherent1.getReservationId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RS-04 : un ADHERENT qui fournit son propre id peut creer une reservation")
    @WithMockUser(username = "adherent1", roles = "User")
    void shouldReturn201WhenAdherentCreatesReservationForSelf() throws Exception {
        String body = objectMapper.writeValueAsString(new ReservationRequestBody(freeBook.getBookId(), adherent1.getUserId()));

        mockMvc.perform(post("/api/reservations")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(adherent1.getUserId()));
    }

    @Test
    @DisplayName("RS-04 : un BIBLIOTHECAIRE peut creer une reservation au nom d'un autre adherent")
    @WithMockUser(username = "biblio", roles = "Admin")
    void shouldReturn201WhenBibliothecaireCreatesReservationForAnotherAdherent() throws Exception {
        String body = objectMapper.writeValueAsString(new ReservationRequestBody(freeBook.getBookId(), adherent2.getUserId()));

        mockMvc.perform(post("/api/reservations")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(adherent2.getUserId()));
    }

    private record ReservationRequestBody(Integer bookId, Integer adherentId) {
    }
}
