package empresa.servlets;

import empresa.dao.EmpleadoDAO;
import empresa.exceptions.DatosNoCorrectosException;
import empresa.model.Empleado;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Servlet controlador único de la aplicación (URL: /empleados).
 * Según el parámetro "opcion" consulta la base de datos y reenvía a un JSP.
 *
 * <ul>
 *   <li>GET  listar   -> views/listado.jsp</li>
 *   <li>GET  salario  -> views/salario.jsp</li>
 *   <li>GET  buscar   -> views/resultados.jsp</li>
 *   <li>GET  editar   -> views/editar.jsp</li>
 *   <li>POST actualizar / crear -> views/mensaje.jsp</li>
 * </ul>
 *
 * @author Adrián Acevedo Reinoso
 */
@WebServlet("/empleados")
public class EmpleadoServlet extends HttpServlet {

    /** Carpeta donde están los JSP de las vistas. */
    private static final String VISTAS = "/views/";

    /**
     * Atiende las peticiones GET. Según el parámetro "opcion" (por defecto
     * "listar") carga los datos y reenvía a la vista correspondiente:
     * listar, salario, buscar o editar. Si ocurre un error, reenvía a error.jsp.
     *
     * @param req  petición HTTP; parámetros usados: opcion, dni, nombre, sexo, categoria y anyos
     * @param resp respuesta HTTP
     * @throws ServletException si falla el reenvío a la vista
     * @throws IOException      si ocurre un error de entrada/salida
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String opcion = req.getParameter("opcion");
        if (opcion == null) {
            opcion = "listar";
        }

        try {
            switch (opcion) {
                case "listar":
                    req.setAttribute("empleados", EmpleadoDAO.listarEmpleados());
                    reenviar(req, resp, "listado.jsp");
                    break;

                case "salario": {
                    String dni = req.getParameter("dni");
                    Empleado emp = EmpleadoDAO.buscarEmpleado(dni);
                    req.setAttribute("empleado", emp);
                    if (emp != null) {
                        req.setAttribute("sueldo", EmpleadoDAO.obtenerSueldo(dni));
                    }
                    reenviar(req, resp, "salario.jsp");
                    break;
                }

                case "buscar": {
                    List<Empleado> lista = EmpleadoDAO.buscarEmpleados(
                            req.getParameter("nombre"),
                            req.getParameter("dni"),
                            req.getParameter("sexo"),
                            aEntero(req.getParameter("categoria")),
                            aEntero(req.getParameter("anyos")));
                    req.setAttribute("empleados", lista);
                    reenviar(req, resp, "resultados.jsp");
                    break;
                }

                case "editar": {
                    String dni = req.getParameter("dni");
                    Empleado emp = EmpleadoDAO.buscarEmpleado(dni);
                    req.setAttribute("empleado", emp);
                    if (emp != null) {
                        req.setAttribute("sueldo", EmpleadoDAO.obtenerSueldo(dni));
                    }
                    reenviar(req, resp, "editar.jsp");
                    break;
                }

                default:
                    error(req, resp, "Opción no válida: " + opcion);
            }
        } catch (SQLException e) {
            error(req, resp, "Error al acceder a la base de datos: " + e.getMessage());
        } catch (NumberFormatException e) {
            error(req, resp, "Hay un campo numérico con un valor no válido.");
        }
    }

    /**
     * Atiende las peticiones POST. Valida los datos creando un {@link Empleado}
     * y, según el parámetro "opcion", da de alta ("crear") o modifica
     * ("actualizar") al empleado. Después reenvía a mensaje.jsp, o a error.jsp
     * si los datos no son correctos, faltan, o falla la base de datos.
     *
     * @param req  petición HTTP; parámetros usados: opcion, nombre, dni, sexo, categoria y anyos
     * @param resp respuesta HTTP
     * @throws ServletException si falla el reenvío a la vista
     * @throws IOException      si ocurre un error de entrada/salida
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8"); // para tildes y ñ en nombres

        String opcion = req.getParameter("opcion");
        if (opcion == null) {
            opcion = "";
        }

        try {
            // Se valida creando el Empleado: lanza DatosNoCorrectosException si hay datos mal.
            Empleado emp = new Empleado(
                    req.getParameter("nombre"),
                    req.getParameter("dni"),
                    req.getParameter("sexo").charAt(0),
                    Integer.parseInt(req.getParameter("categoria")),
                    Integer.parseInt(req.getParameter("anyos")));

            if (opcion.equals("crear")) {
                if (EmpleadoDAO.buscarEmpleado(emp.getDni()) != null) {
                    error(req, resp, "Ya existe un empleado con el DNI " + emp.getDni() + ".");
                    return;
                }
                EmpleadoDAO.altaEmpleado(emp);
                req.setAttribute("mensaje", "Empleado creado correctamente. Su sueldo se ha calculado automáticamente.");
            } else if (opcion.equals("actualizar")) {
                if (EmpleadoDAO.buscarEmpleado(emp.getDni()) == null) {
                    error(req, resp, "No existe ningún empleado con el DNI " + emp.getDni() + ".");
                    return;
                }
                EmpleadoDAO.actualizarEmpleado(emp);
                req.setAttribute("mensaje", "Empleado actualizado correctamente. Su sueldo se ha recalculado.");
            } else {
                error(req, resp, "Opción no válida: " + opcion);
                return;
            }
            reenviar(req, resp, "mensaje.jsp");

        } catch (DatosNoCorrectosException e) {
            error(req, resp, "Datos no correctos: revisa el DNI (9 caracteres), el sexo (M/F), "
                    + "la categoría (1-10) y los años (0 o más).");
        } catch (NumberFormatException | NullPointerException e) {
            error(req, resp, "Faltan datos o hay un campo numérico no válido.");
        } catch (SQLException e) {
            error(req, resp, "Error al acceder a la base de datos: " + e.getMessage());
        }
    }

    /**
     * Convierte un parámetro de texto a Integer.
     *
     * @param valor texto recibido en el parámetro (puede ser {@code null} o vacío)
     * @return el número, o {@code null} si el texto es {@code null} o está vacío
     * @throws NumberFormatException si el texto no es un número válido
     */
    private Integer aEntero(String valor) {
        return (valor == null || valor.isEmpty()) ? null : Integer.valueOf(valor.trim());
    }

    /**
     * Reenvía la petición a una vista (JSP) de la carpeta de vistas.
     *
     * @param req   petición HTTP, con los atributos que usará la vista
     * @param resp  respuesta HTTP
     * @param vista nombre del JSP dentro de /views/ (por ejemplo "listado.jsp")
     * @throws ServletException si falla el reenvío
     * @throws IOException      si ocurre un error de entrada/salida
     */
    private void reenviar(HttpServletRequest req, HttpServletResponse resp, String vista)
            throws ServletException, IOException {
        req.getRequestDispatcher(VISTAS + vista).forward(req, resp);
    }

    /**
     * Guarda un mensaje de error en la petición (atributo "error")
     * y reenvía a error.jsp.
     *
     * @param req     petición HTTP
     * @param resp    respuesta HTTP
     * @param mensaje texto del error que verá el usuario
     * @throws ServletException si falla el reenvío
     * @throws IOException      si ocurre un error de entrada/salida
     */
    private void error(HttpServletRequest req, HttpServletResponse resp, String mensaje)
            throws ServletException, IOException {
        req.setAttribute("error", mensaje);
        reenviar(req, resp, "error.jsp");
    }
}
