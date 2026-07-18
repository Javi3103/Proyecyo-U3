package ec.edu.espe.mastergateway.usuario.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Shift-Left: la política de contraseñas (mínimo 8 caracteres, letras y
 * números) debe rechazarse en la frontera de entrada (@Valid en el
 * Controller), antes de que el Service o el hash de BCrypt siquiera se
 * ejecuten. Estas pruebas validan el DTO directamente, sin arrancar Spring.
 */
class CreateUsuarioRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"corta1", "1234567", "sololetras", "12345678"})
    void password_debilOSinLetrasONumeros_esRechazada(String passwordDebil) {
        var request = new CreateUsuarioRequest("ana@espe.edu.ec", passwordDebil, "Ana");

        Set<ConstraintViolation<CreateUsuarioRequest>> violaciones = validator.validate(request);

        assertThat(violaciones).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void password_validaConLetrasYNumerosYLongitudMinima_esAceptada() {
        var request = new CreateUsuarioRequest("ana@espe.edu.ec", "clave1234", "Ana");

        Set<ConstraintViolation<CreateUsuarioRequest>> violaciones = validator.validate(request);

        assertThat(violaciones).isEmpty();
    }

    @Test
    void email_conFormatoInvalido_esRechazado() {
        var request = new CreateUsuarioRequest("no-es-un-email", "clave1234", "Ana");

        Set<ConstraintViolation<CreateUsuarioRequest>> violaciones = validator.validate(request);

        assertThat(violaciones).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    void nombreCompleto_vacio_esRechazado() {
        var request = new CreateUsuarioRequest("ana@espe.edu.ec", "clave1234", "  ");

        Set<ConstraintViolation<CreateUsuarioRequest>> violaciones = validator.validate(request);

        assertThat(violaciones).anyMatch(v -> v.getPropertyPath().toString().equals("nombreCompleto"));
    }
}
