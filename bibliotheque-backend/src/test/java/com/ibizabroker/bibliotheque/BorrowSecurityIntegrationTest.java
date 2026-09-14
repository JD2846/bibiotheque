package com.ibizabroker.bibliotheque;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.BorrowRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Borrow;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration (contexte Spring complet + MockMvc + Postgres via Testcontainers)
 * des regles de securite et de gestion EMP-01 a EMP-05 du module emprunt.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class BorrowSecurityIntegrationTest {

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
    private BorrowRepository borrowRepository;

    private Users adherent1;
    private Users adherent2;
    private Books availableBook;
    private Books outOfStockBook;
    private Borrow borrowAdherent1;

    @BeforeEach
    void setUp() {
        Role roleUser1 = new Role();
        roleUser1.setRoleName("User");
        adherent1 = new Users();
        adherent1.setUsername("adherent1");
        adherent1.setName("Adherent Un");
        adherent1.setPassword("placeholder");
        adherent1.setRole(Set.of(roleUser1));
        adherent1 = usersRepository.save(adherent1);

        Role roleUser2 = new Role();
        roleUser2.setRoleName("User");
        adherent2 = new Users();
        adherent2.setUsername("adherent2");
        adherent2.setName("Adherent Deux");
        adherent2.setPassword("placeholder");
        adherent2.setRole(Set.of(roleUser2));
        adherent2 = usersRepository.save(adherent2);

        availableBook = new Books();
        availableBook.setBookName("Available Book");
        availableBook.setBookAuthor("Author");
        availableBook.setNoOfCopies(3);
        availableBook = booksRepository.save(availableBook);

        outOfStockBook = new Books();
        outOfStockBook.setBookName("Out Of Stock Book");
        outOfStockBook.setBookAuthor("Author");
        outOfStockBook.setNoOfCopies(0);
        outOfStockBook = booksRepository.save(outOfStockBook);

        borrowAdherent1 = new Borrow();
        borrowAdherent1.setBookId(outOfStockBook.getBookId());
        borrowAdherent1.setUserId(adherent1.getUserId());
        borrowAdherent1 = borrowRepository.save(borrowAdherent1);
    }

    @Test
    @DisplayName("EMP-05 : GET /borrow sans token renvoie 401")
    void shouldReturn401WhenNoTokenOnBorrowList() throws Exception {
        mockMvc.perform(get("/borrow"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("EMP-05 : un ADHERENT authentifie ne voit que ses propres emprunts")
    @WithMockUser(username = "adherent1", roles = "User")
    void shouldReturn200AndOnlyOwnBorrowsWhenAdherentAuthenticated() throws Exception {
        mockMvc.perform(get("/borrow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(adherent1.getUserId()));
    }

    @Test
    @DisplayName("EMP-05 : un ADHERENT peut consulter son propre historique")
    @WithMockUser(username = "adherent1", roles = "User")
    void shouldReturn200WhenAdherentReadsOwnHistory() throws Exception {
        mockMvc.perform(get("/borrow/user/" + adherent1.getUserId()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("EMP-05 : un ADHERENT qui consulte l'historique d'un autre recoit 403")
    @WithMockUser(username = "adherent2", roles = "User")
    void shouldReturn403WhenAdherentReadsAnotherUsersHistory() throws Exception {
        mockMvc.perform(get("/borrow/user/" + adherent1.getUserId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("EMP-05 : un ADHERENT qui consulte l'historique d'un livre recoit 403")
    @WithMockUser(username = "adherent1", roles = "User")
    void shouldReturn403WhenAdherentReadsBookHistory() throws Exception {
        mockMvc.perform(get("/borrow/book/" + outOfStockBook.getBookId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("EMP-05 : un BIBLIOTHECAIRE peut consulter l'historique d'un livre")
    @WithMockUser(username = "biblio", roles = "Admin")
    void shouldReturn200WhenBibliothecaireReadsBookHistory() throws Exception {
        mockMvc.perform(get("/borrow/book/" + outOfStockBook.getBookId()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("EMP-01 : emprunter un livre en rupture de stock renvoie 409")
    @WithMockUser(username = "adherent2", roles = "User")
    void shouldReturn409WhenBookOutOfStock() throws Exception {
        String body = objectMapper.writeValueAsString(new BorrowRequestBody(outOfStockBook.getBookId(), null));

        mockMvc.perform(post("/borrow")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("EMP-04 : un ADHERENT qui emprunte voit son userId fourni ignore")
    @WithMockUser(username = "adherent1", roles = "User")
    void shouldReturn201AndIgnoreProvidedUserIdWhenAdherentBorrows() throws Exception {
        String body = objectMapper.writeValueAsString(new BorrowRequestBody(availableBook.getBookId(), adherent2.getUserId()));

        mockMvc.perform(post("/borrow")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(adherent1.getUserId()))
                .andExpect(jsonPath("$.bookTitle").value("Available Book"));
    }

    @Test
    @DisplayName("EMP-04 : un BIBLIOTHECAIRE peut emprunter au nom d'un autre adherent")
    @WithMockUser(username = "biblio", roles = "Admin")
    void shouldReturn201WhenBibliothecaireBorrowsForAnotherAdherent() throws Exception {
        String body = objectMapper.writeValueAsString(new BorrowRequestBody(availableBook.getBookId(), adherent2.getUserId()));

        mockMvc.perform(post("/borrow")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(adherent2.getUserId()));
    }

    @Test
    @DisplayName("EMP-05 : un ADHERENT qui rend l'emprunt d'un autre recoit 403")
    @WithMockUser(username = "adherent2", roles = "User")
    void shouldReturn403WhenAdherentReturnsAnotherUsersBorrow() throws Exception {
        String body = objectMapper.writeValueAsString(new ReturnRequestBody(borrowAdherent1.getBorrowId()));

        mockMvc.perform(put("/borrow")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("EMP-05 : un ADHERENT peut rendre son propre emprunt")
    @WithMockUser(username = "adherent1", roles = "User")
    void shouldReturn200WhenAdherentReturnsOwnBorrow() throws Exception {
        String body = objectMapper.writeValueAsString(new ReturnRequestBody(borrowAdherent1.getBorrowId()));

        mockMvc.perform(put("/borrow")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnDate").exists());
    }

    @Test
    @DisplayName("EMP-04 : rendre un emprunt deja rendu renvoie 409")
    @WithMockUser(username = "adherent1", roles = "User")
    void shouldReturn409WhenBorrowAlreadyReturned() throws Exception {
        String body = objectMapper.writeValueAsString(new ReturnRequestBody(borrowAdherent1.getBorrowId()));

        mockMvc.perform(put("/borrow").contentType("application/json").content(body))
                .andExpect(status().isOk());

        mockMvc.perform(put("/borrow").contentType("application/json").content(body))
                .andExpect(status().isConflict());
    }

    private record BorrowRequestBody(Integer bookId, Integer userId) {
    }

    private record ReturnRequestBody(Integer borrowId) {
    }
}
