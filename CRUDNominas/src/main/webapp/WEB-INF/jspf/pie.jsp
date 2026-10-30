<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%-- Pie común: botón "Volver" (pantalla anterior) + enlace al menú, y cierre del HTML. --%>
    <div class="acciones">
        <button type="button" class="sec" onclick="history.back()">&larr; Volver</button>
        <a class="btn sec" href="${pageContext.request.contextPath}/index.html">Menú principal</a>
    </div>
</div>
</body>
</html>
