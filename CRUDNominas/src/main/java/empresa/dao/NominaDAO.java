package empresa.dao;

import empresa.conexion.conexionDB;
import empresa.model.Empleado;
import empresa.utils.Nomina;


import java.sql.*;

import static empresa.dao.EmpleadoDAO.buscarEmpleado;
import static empresa.dao.EmpleadoDAO.listarEmpleados;

/**
 * Acceso a datos de la tabla Nominas: calcula y guarda el sueldo
 * de los empleados.
 *
 * @author Adrián Acevedo Reinoso
 */
public class NominaDAO {

    /** Calculadora de sueldos según categoría y antigüedad. */
    private static final Nomina nomina = new Nomina();

    /**
     * Calcula e inserta la nómina de un empleado usando una conexión ya abierta.
     * Si el empleado ya tenía nómina, se actualiza su sueldo.
     * No hace commit ni rollback: lo gestiona quien llama, para poder
     * incluirla en su misma transacción (por ejemplo, el alta del empleado).
     *
     * @param con conexión abierta en la que se ejecuta la inserción
     * @param emp empleado cuyo sueldo se calcula y se guarda
     * @throws SQLException si ocurre un error de base de datos
     */
    public static void asignarSueldo(Connection con, Empleado emp) throws SQLException {
        String sql = "INSERT INTO Nominas(dni,sueldo) VALUES (?,?) " +
                "ON DUPLICATE KEY UPDATE sueldo = VALUES(sueldo)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, emp.dni);
            ps.setDouble(2, nomina.sueldo(emp.getCategoria(), emp.getAnyos()));
            ps.executeUpdate();
        }
    }

    /**
     * Versión independiente de {@link #asignarSueldo(Connection, Empleado)}:
     * abre su propia conexión y transacción, y hace commit al terminar
     * (o rollback si falla).
     *
     * @param emp empleado cuyo sueldo se calcula y se guarda
     * @throws SQLException si ocurre un error de base de datos
     */
    public static void asignarSueldo(Empleado emp) throws SQLException {
        try (Connection con = conexionDB.getConnection()) {
            con.setAutoCommit(false);
            try {
                asignarSueldo(con, emp);
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /**
     * Recalcula el sueldo de un empleado a partir de su categoría y años
     * actuales, y lo guarda en Nominas (lo crea si no existía).
     *
     * @param dni DNI del empleado cuyo sueldo se recalcula
     * @throws SQLException si no existe un empleado con ese DNI
     *                      o si ocurre un error de base de datos
     */
    public static void actualizarSueldo(String dni) throws SQLException {
        Empleado emp = buscarEmpleado(dni);
        if (emp == null) {
            throw new SQLException("No existe empleado con DNI " + dni);
        }

        double sueldo = nomina.sueldo(emp.getCategoria(), emp.getAnyos());

        String sql = "INSERT INTO Nominas(dni, sueldo) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE sueldo = VALUES(sueldo)";

        try (Connection con = conexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, dni);
            ps.setDouble(2, sueldo);
            ps.executeUpdate();
        }
    }

    /**
     * Recalcula y guarda el sueldo de todos los empleados.
     * Sirve, por ejemplo, para crear la nómina de empleados que no la tenían.
     *
     * @throws SQLException si ocurre un error de base de datos
     */
    public static void actualizarTodosSueldos() throws SQLException {
        for (Empleado e : listarEmpleados()) {
            actualizarSueldo(e.dni);
        }
    }
}
