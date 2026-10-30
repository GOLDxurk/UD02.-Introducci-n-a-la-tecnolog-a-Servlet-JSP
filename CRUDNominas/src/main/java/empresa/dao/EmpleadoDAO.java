package empresa.dao;
import empresa.conexion.conexionDB;
import empresa.model.Empleado;

import java.io.*;
import java.sql.*;
import java.util.*;

import static empresa.dao.NominaDAO.actualizarSueldo;


/**
 * Acceso a datos de la tabla Empleados (altas, consultas y modificaciones).
 * El sueldo de cada empleado se guarda en la tabla Nominas a través de
 * {@link NominaDAO}.
 *
 * @author Adrián Acevedo Reinoso
 */
public class EmpleadoDAO {


    /**
     * Alta individual. Inserta el empleado y su nómina (sueldo calculado)
     * en una misma transacción: si falla una de las dos inserciones,
     * no se guarda ninguna.
     *
     * @param emp empleado que se da de alta
     * @throws SQLException si ocurre un error de base de datos
     *                      (por ejemplo, DNI duplicado)
     */
    public static void altaEmpleado(Empleado emp) throws SQLException {
        String sqlEmp = "INSERT INTO Empleados(dni,nombre,sexo,categoria,anyos) VALUES (?,?,?,?,?)";

        try (Connection con = conexionDB.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(sqlEmp)) {

                ps.setString(1, emp.dni);
                ps.setString(2, emp.nombre);
                ps.setString(3, String.valueOf(emp.sexo));
                ps.setInt(4, emp.getCategoria());
                ps.setInt(5, emp.getAnyos());
                ps.executeUpdate();

                // La nómina la inserta NominaDAO, en la misma transacción que el empleado
                NominaDAO.asignarSueldo(con, emp);

                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /**
     * Obtiene todos los empleados, ordenados por DNI.
     *
     * @return lista con todos los empleados (vacía si no hay ninguno)
     * @throws SQLException si ocurre un error de base de datos
     */
    public static List<Empleado> listarEmpleados() throws SQLException {
        List<Empleado> lista = new ArrayList<>();
        String sql = "SELECT nombre, dni, sexo, categoria, anyos FROM Empleados ORDER BY dni";

        try (Connection con = conexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(new Empleado(
                        rs.getString("nombre"),
                        rs.getString("dni"),
                        rs.getString("sexo").charAt(0),
                        rs.getInt("categoria"),
                        rs.getInt("anyos")
                ));
            }
        }
        return lista;
    }

    /**
     * Busca un empleado por su DNI exacto.
     *
     * @param dni DNI del empleado que se busca
     * @return el empleado encontrado, o {@code null} si no existe ninguno con ese DNI
     * @throws SQLException si ocurre un error de base de datos
     */
    public static Empleado buscarEmpleado(String dni) throws SQLException {
        String sql = "SELECT nombre, dni, sexo, categoria, anyos FROM Empleados WHERE dni = ?";
        Empleado em = null;
        try (Connection con = conexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, dni);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                   em = new Empleado(
                            rs.getString("nombre"),
                            rs.getString("dni"),
                            rs.getString("sexo").charAt(0),
                            rs.getInt("categoria"),
                            rs.getInt("anyos")
                    );
                }
            } catch (SQLException e){
                con.close();
                throw e;

            }
        } catch (SQLException e){

            throw e;
        }
        return em;
    }

    /**
     * Búsqueda de empleados combinando filtros opcionales. Solo se aplican los
     * filtros que no son {@code null} ni están vacíos. El nombre y el DNI se
     * buscan por coincidencia parcial; el sexo, la categoría y los años, por
     * coincidencia exacta. El resultado se ordena por DNI.
     *
     * @param nombre    texto que debe contener el nombre (opcional)
     * @param dni       texto que debe contener el DNI (opcional)
     * @param sexo      sexo exacto, 'M' o 'F' (opcional)
     * @param categoria categoría exacta (opcional)
     * @param anyos     años de antigüedad exactos (opcional)
     * @return lista de empleados que cumplen todos los filtros (vacía si ninguno)
     * @throws SQLException si ocurre un error de base de datos
     */
    public static List<Empleado> buscarEmpleados(String nombre, String dni, String sexo,
                                                 Integer categoria, Integer anyos) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT nombre, dni, sexo, categoria, anyos FROM Empleados WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (nombre != null && !nombre.isEmpty()) {
            sql.append(" AND nombre LIKE ?");
            params.add("%" + nombre.trim() + "%");
        }
        if (dni != null && !dni.isEmpty()) {
            sql.append(" AND dni LIKE ?");
            params.add("%" + dni.trim() + "%");
        }
        if (sexo != null && !sexo.isEmpty()) {
            sql.append(" AND sexo = ?");
            params.add(sexo.trim());
        }
        if (categoria != null) {
            sql.append(" AND categoria = ?");
            params.add(categoria);
        }
        if (anyos != null) {
            sql.append(" AND anyos = ?");
            params.add(anyos);
        }
        sql.append(" ORDER BY dni");

        List<Empleado> lista = new ArrayList<>();
        try (Connection con = conexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(crearEmpleado(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Construye un {@link Empleado} con la fila actual del {@link ResultSet}.
     *
     * @param rs resultado de una consulta, posicionado en la fila a leer
     *           (debe tener las columnas nombre, dni, sexo, categoria y anyos)
     * @return el empleado creado con los datos de la fila
     * @throws SQLException si no se puede leer alguna columna
     */
    private static Empleado crearEmpleado(ResultSet rs) throws SQLException {
        return new Empleado(
                rs.getString("nombre"),
                rs.getString("dni"),
                rs.getString("sexo").charAt(0),
                rs.getInt("categoria"),
                rs.getInt("anyos"));
    }

    /**
     * Consulta el sueldo guardado de un empleado en la tabla Nominas.
     *
     * @param dni DNI del empleado
     * @return el sueldo guardado, o 0 si el empleado no tiene nómina
     * @throws SQLException si ocurre un error de base de datos
     */
    public static Double obtenerSueldo(String dni) throws SQLException {
        String sql = "SELECT sueldo FROM Nominas WHERE dni = ?";
        double sueldo = 0;
        try (Connection con = conexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, dni);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return sueldo = rs.getDouble("sueldo");
                }
            } catch (SQLException e){
                con.close();
                throw e;
            }
        }
    return sueldo;
    }

    /**
     * Actualiza el nombre, sexo, categoría y años de un empleado (identificado
     * por su DNI) y recalcula su sueldo en la tabla Nominas.
     *
     * @param emp empleado con los datos nuevos; su DNI identifica el registro a modificar
     * @throws SQLException si ocurre un error de base de datos
     */
    public static void actualizarEmpleado(Empleado emp) throws SQLException {
        String sql = "UPDATE Empleados SET nombre=?, sexo=?, categoria=?, anyos=? WHERE dni=?";

        try (Connection con = conexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, emp.nombre);
            ps.setString(2, String.valueOf(emp.sexo));
            ps.setInt(3, emp.getCategoria());
            ps.setInt(4, emp.getAnyos());
            ps.setString(5, emp.dni);
            ps.executeUpdate();
        }

        actualizarSueldo(emp.dni);
    }



}
