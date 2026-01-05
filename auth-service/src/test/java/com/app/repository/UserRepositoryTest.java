package com.app.repository;
import com.app.entity.User;
import com.app.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("UserRepository Tests")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        // create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword123");
        testUser.setRole(Role.GUEST);
        testUser.setFullName("Test User");
        testUser.setPhoneNumber("1234567890");
        testUser.setActive(true);

        // persist and flush to database
        entityManager.persist(testUser);
        entityManager.flush();
    }

    @Test
    @DisplayName("Should find user by username")
    void testFindByUsername_Success() {
        // when
        Optional<User> found = userRepository.findByUsername("testuser");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should return empty when username not found")
    void testFindByUsername_NotFound() {
        // when
        Optional<User> found = userRepository.findByUsername("nonexistent");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find user by email")
    void testFindByEmail_Success() {
        // when
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should return empty when email not found")
    void testFindByEmail_NotFound() {
        // when
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should return true when username exists")
    void testExistsByUsername_True() {
        // when
        boolean exists = userRepository.existsByUsername("testuser");

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when username does not exist")
    void testExistsByUsername_False() {
        // when
        boolean exists = userRepository.existsByUsername("nonexistent");

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should return true when email exists")
    void testExistsByEmail_True() {
        // when
        boolean exists = userRepository.existsByEmail("test@example.com");

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when email does not exist")
    void testExistsByEmail_False() {
        // when
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should return true when username exists for different user")
    void testExistsByUsernameAndIdNot_True() {
        // given - create another user
        User anotherUser = new User();
        anotherUser.setUsername("anotheruser");
        anotherUser.setEmail("another@example.com");
        anotherUser.setPassword("password123");
        anotherUser.setRole(Role.GUEST);
        anotherUser.setActive(true);
        entityManager.persist(anotherUser);
        entityManager.flush();

        // when - check if testuser exists excluding anotherUser's id
        boolean exists = userRepository.existsByUsernameAndIdNot("testuser", anotherUser.getId());

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when username only exists for current user")
    void testExistsByUsernameAndIdNot_False() {
        // when - check if testuser exists excluding testUser's own id
        boolean exists = userRepository.existsByUsernameAndIdNot("testuser", testUser.getId());

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should return false when username does not exist at all")
    void testExistsByUsernameAndIdNot_NotExists() {
        // when
        boolean exists = userRepository.existsByUsernameAndIdNot("nonexistent", 999L);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should save user with all fields")
    void testSaveUser_Success() {
        // given
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("new@example.com");
        newUser.setPassword("encodedPassword");
        newUser.setRole(Role.MANAGER);
        newUser.setHotelId(1L);
        newUser.setFullName("New Manager");
        newUser.setPhoneNumber("9876543210");
        newUser.setActive(true);

        // when
        User saved = userRepository.save(newUser);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUsername()).isEqualTo("newuser");
        assertThat(saved.getRole()).isEqualTo(Role.MANAGER);
        assertThat(saved.getHotelId()).isEqualTo(1L);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should save GUEST user without hotelId")
    void testSaveGuestUser_WithoutHotelId() {
        // given
        User guestUser = new User();
        guestUser.setUsername("guest");
        guestUser.setEmail("guest@example.com");
        guestUser.setPassword("password");
        guestUser.setRole(Role.GUEST);
        guestUser.setHotelId(null); // guests don't have hotelId
        guestUser.setFullName("Guest User");
        guestUser.setPhoneNumber("1111111111");
        guestUser.setActive(true);

        // when
        User saved = userRepository.save(guestUser);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getHotelId()).isNull();
        assertThat(saved.getRole()).isEqualTo(Role.GUEST);
    }
}