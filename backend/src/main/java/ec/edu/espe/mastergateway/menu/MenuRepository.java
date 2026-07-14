package ec.edu.espe.mastergateway.menu;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MenuRepository extends JpaRepository<Menu, UUID> {

    /**
     * Árbol de menú de un rol en una sola consulta (evita el N+1 que exige
     * prevenir la sección 6.4 del documento). Caso base: items/submenús
     * asignados directamente en rol_menu. Caso recursivo: sube por parent_id
     * para traer toda la cadena de ancestros necesaria para reconstruir el
     * árbol completo (módulo -> submenú -> item) en memoria en el service.
     *
     * Nota: @SQLRestriction no se aplica a consultas nativas, por eso el
     * filtro "estado = 'ACTIVO'" se repite explícitamente en cada paso del CTE.
     * Si un nodo padre está INACTIVO, la recursión se corta ahí y sus
     * descendientes dejan de ser alcanzables (comportamiento esperado según
     * la tabla de endpoints: "si se elimina un padre... ignorar los hijos").
     */
    @Query(value = """
            WITH RECURSIVE menu_tree AS (
                SELECT m.*
                FROM menu m
                INNER JOIN rol_menu rm ON rm.menu_id = m.id AND rm.estado = 'ACTIVO'
                WHERE rm.rol_id = :rolId AND m.estado = 'ACTIVO'

                UNION

                SELECT p.*
                FROM menu p
                INNER JOIN menu_tree mt ON mt.parent_id = p.id
                WHERE p.estado = 'ACTIVO'
            )
            SELECT DISTINCT * FROM menu_tree
            """, nativeQuery = true)
    List<Menu> findArbolByRolId(@Param("rolId") UUID rolId);

    List<Menu> findByModuloId(UUID moduloId);
}
