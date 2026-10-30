package empresa.utils;

import empresa.model.Empleado;

/**
 * Clase encargada de calcular el sueldo de un empleado
 * en función de su categoría y sus años de antiguedad.
 *
 * @author Adrián Acevedo Reinoso
 */
public class Nomina {
    /** Sueldo base anual según la categoría (índice 0 -> categoría 1, etc.). */
    private static final int SUELDO_BASE[] = {50000, 70000, 90000, 110000, 130000,
            150000, 170000, 190000, 210000, 230000};

    /**
     * Calcula el sueldo total como el sueldo base de la categoría
     * más 5000 euros por cada año de antiguedad.
     * (Una categoría 0, sin asignar, se trata como la categoría 1.)
     *
     * @param categoriaEmpleado categoría laboral del empleado (1-10; 0 se trata como 1)
     * @param anyosEmpleado     años de antigüedad del empleado
     * @return el sueldo total en euros
     * @throws ArrayIndexOutOfBoundsException si la categoría no está entre 0 y 10
     */
    public double sueldo(int categoriaEmpleado, int anyosEmpleado) {
        int posicion;
        if (categoriaEmpleado == 0) {
            posicion = categoriaEmpleado;
        } else {
            posicion = categoriaEmpleado - 1;
        }
        return SUELDO_BASE[posicion]+ 5000 * anyosEmpleado;

    }
    /**
     * Calcula el sueldo total de un empleado a partir de su categoría
     * y sus años de antigüedad.
     *
     * @param empleado empleado del que se calcula el sueldo
     * @return el sueldo total en euros
     * @see #sueldo(int, int)
     */
    public double sueldo(Empleado empleado) {
        return sueldo(empleado.getCategoria(), empleado.getAnyos());
    }
}
