<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  Resultados de la búsqueda de empleados.
  Recibe del servlet (request): "empleados" -> List<Empleado> (puede estar vacía)
  Cada fila enlaza a: empleados?opcion=editar&dni=XXXXXXXXX
--%>
<jsp:include page="/WEB-INF/jspf/cabecera.jsp"><jsp:param name="titulo" value="Resultados de la búsqueda"/></jsp:include>

<c:choose>
    <c:when test="${empty empleados}">
        <div class="msg vacio">No se ha encontrado ningún empleado con esos criterios.</div>
    </c:when>
    <c:otherwise>
        <p class="sub">${empleados.size()} resultado(s).</p>
        <table>
            <thead>
            <tr>
                <th>Nombre</th><th>DNI</th><th>Sexo</th>
                <th class="num">Categoría</th><th class="num">Años</th><th>Acciones</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="e" items="${empleados}">
                <tr>
                    <td><c:out value="${e.nombre}"/></td>
                    <td><c:out value="${e.dni}"/></td>
                    <td><c:out value="${e.sexo}"/></td>
                    <td class="num"><c:out value="${e.categoria}"/></td>
                    <td class="num"><c:out value="${e.anyos}"/></td>
                    <td>
                        <c:url var="urlEditar" value="/empleados">
                            <c:param name="opcion" value="editar"/>
                            <c:param name="dni" value="${e.dni}"/>
                        </c:url>
                        <a href="${urlEditar}">Modificar</a>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="/WEB-INF/jspf/pie.jsp"/>
