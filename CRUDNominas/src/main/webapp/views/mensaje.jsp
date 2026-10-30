<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  Página de información (operación correcta).
  Recibe del servlet (request): "mensaje" -> String
--%>
<jsp:include page="/WEB-INF/jspf/cabecera.jsp"><jsp:param name="titulo" value="Operación realizada"/></jsp:include>

<div class="msg ok"><c:out value="${mensaje}" default="La operación se ha realizado correctamente."/></div>

<jsp:include page="/WEB-INF/jspf/pie.jsp"/>
