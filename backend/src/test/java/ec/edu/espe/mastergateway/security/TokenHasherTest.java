package ec.edu.espe.mastergateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenHasherTest {

    @Test
    void sha256Hex_esDeterministico_mismaEntradaMismoHash() {
        String token = "un-refresh-token-de-prueba";

        assertThat(TokenHasher.sha256Hex(token)).isEqualTo(TokenHasher.sha256Hex(token));
    }

    @Test
    void sha256Hex_entradasDistintasProducenHashesDistintos() {
        assertThat(TokenHasher.sha256Hex("token-a")).isNotEqualTo(TokenHasher.sha256Hex("token-b"));
    }

    @Test
    void sha256Hex_devuelve64CaracteresHexadecimales() {
        String hash = TokenHasher.sha256Hex("cualquier-valor");

        assertThat(hash).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    void sha256Hex_noDevuelveElValorOriginal() {
        // El hash de un refresh token se guarda en la BD (RefreshToken.tokenHash);
        // si alguien filtra esa tabla, no debe poder reconstruir el token real.
        String token = "secreto-de-alta-entropia";

        assertThat(TokenHasher.sha256Hex(token)).doesNotContain(token);
    }
}
