package com.tareia.app.ingesta

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Lectura del calendario que exporta el aula virtual (Moodle) y plan de sincronización. */
class CalendarioAulaTest {
    private val lima = ZoneId.of("America/Lima")

    // Formato real de Moodle: CRLF, líneas plegadas a 75 caracteres, horas en UTC y CATEGORIES con el curso.
    private val ics = listOf(
        "BEGIN:VCALENDAR",
        "METHOD:PUBLISH",
        "PRODID:-//Moodle Pty Ltd//NONSGML Moodle Version 2024100700//EN",
        "VERSION:2.0",
        "BEGIN:VEVENT",
        "UID:133370@aulavirtual.upt.edu.pe",
        "SUMMARY:Proyecto de Unidad II vence",
        "DESCRIPTION:Enlace: https://classroom50.org/UPT-FAING-EPIS/si784\\, grupo de",
        "  2 estudiantes\\nEntregar FD01 a FD04",
        "CLASS:PUBLIC",
        "LAST-MODIFIED:20261110T120000Z",
        "DTSTAMP:20261120T030000Z",
        "DTSTART:20261116T045900Z",
        "DTEND:20261116T045900Z",
        "CATEGORIES:SI-784 - EPIS A",
        "END:VEVENT",
        "BEGIN:VEVENT",
        "UID:2001@aulavirtual.upt.edu.pe",
        "SUMMARY:Cuestionario 3 abre",
        "DTSTART:20261201T130000Z",
        "DTEND:20261201T130000Z",
        "CATEGORIES:SI-783",
        "END:VEVENT",
        "BEGIN:VEVENT",
        "UID:2002@aulavirtual.upt.edu.pe",
        "SUMMARY:Cuestionario 3 cierra",
        "DTSTART:20261203T235900",
        "CATEGORIES:SI-783",
        "END:VEVENT",
        "BEGIN:VEVENT",
        "UID:2003@aulavirtual.upt.edu.pe",
        "SUMMARY:Feriado regional",
        "DTSTART;VALUE=DATE:20261208",
        "END:VEVENT",
        "BEGIN:VEVENT",
        "SUMMARY:Evento sin UID vence",
        "DTSTART:20261210T120000Z",
        "END:VEVENT",
        "BEGIN:VEVENT",
        "UID:2004@aulavirtual.upt.edu.pe",
        "SUMMARY:Informe con zona está en fecha de entrega",
        "DTSTART;TZID=America/New_York:20261205T010000",
        "END:VEVENT",
        "END:VCALENDAR",
    ).joinToString("\r\n")

    @Test
    fun leeEntregasConvierteUtcALimaYLimpiaElTitulo() {
        val eventos = LectorIcs.leer(ics, lima)
        val proyecto = eventos.first { it.uid == "133370@aulavirtual.upt.edu.pe" }
        assertEquals("Proyecto de Unidad II", proyecto.titulo)
        // 04:59 UTC del 16 = 23:59 del 15 en Lima
        assertEquals(LocalDate.of(2026, 11, 15), proyecto.fecha)
        assertEquals("SI-784 - EPIS A", proyecto.curso)
        assertEquals("aula:133370@aulavirtual.upt.edu.pe", proyecto.idExterno)
        assertTrue(proyecto.descripcion.contains("si784, grupo de 2 estudiantes\nEntregar FD01"))
    }

    @Test
    fun descartaAperturasYEventosSinUid() {
        val titulos = LectorIcs.leer(ics, lima).map { it.titulo }
        assertFalse(titulos.any { it.contains("abre") })
        assertFalse(titulos.any { it.contains("sin UID") })
        assertEquals(listOf("Proyecto de Unidad II", "Cuestionario 3", "Informe con zona", "Feriado regional"), titulos)
    }

    @Test
    fun soportaFechaLocalDiaCompletoYZonaHoraria() {
        val e = LectorIcs.leer(ics, lima).associateBy { it.titulo }
        assertEquals(LocalDate.of(2026, 12, 3), e["Cuestionario 3"]!!.fecha)
        assertEquals(LocalDate.of(2026, 12, 8), e["Feriado regional"]!!.fecha)
        assertNull(e["Feriado regional"]!!.curso)
        // 01:00 en Nueva York = 01:00 en Lima (ambos UTC-5 en diciembre)
        assertEquals(LocalDate.of(2026, 12, 5), e["Informe con zona"]!!.fecha)
    }

    @Test
    fun noRepiteUnUidYToleraSaltosDeLineaUnix() {
        val doble = ics.replace("\r\n", "\n") + "\nBEGIN:VEVENT\nUID:133370@aulavirtual.upt.edu.pe\nSUMMARY:Proyecto de Unidad II vence\nDTSTART:20261118T045900Z\nEND:VEVENT\n"
        val eventos = LectorIcs.leer(doble, lima).filter { it.uid.startsWith("133370") }
        assertEquals(1, eventos.size)
        assertEquals(LocalDate.of(2026, 11, 17), eventos[0].fecha)
    }

    @Test
    fun textoInvalidoNoRompe() {
        assertTrue(LectorIcs.leer("", lima).isEmpty())
        assertTrue(LectorIcs.leer("<html>Inicia sesión</html>", lima).isEmpty())
        assertTrue(LectorIcs.leer("BEGIN:VEVENT\nUID:x\nSUMMARY:A vence\nDTSTART:fecha-mala\nEND:VEVENT", lima).isEmpty())
    }

    @Test
    fun limpiezaDeTitulosYEscapes() {
        assertEquals("Informe 3", LectorIcs.limpiarTitulo("Informe 3 vence"))
        assertEquals("Quiz 1", LectorIcs.limpiarTitulo("Quiz 1 closes"))
        assertEquals("Tarea 2", LectorIcs.limpiarTitulo("Tarea 2 is due"))
        assertNull(LectorIcs.limpiarTitulo("Examen se abre"))
        assertNull(LectorIcs.limpiarTitulo("  vence"))
        assertEquals("Charla", LectorIcs.limpiarTitulo("Charla"))
        assertEquals("a,b;c\\d\ne", LectorIcs.desescapar("a\\,b\\;c\\\\d\\ne"))
        assertEquals(listOf("ABC", "D"), LectorIcs.desplegar("AB\r\n C\r\n\r\nD"))
    }

    @Test
    fun planSeparaNuevosCambiosIgualesYVencidos() {
        val hoy = LocalDate.of(2026, 11, 20)
        val e = { uid: String, f: LocalDate -> EventoAula(uid, "T$uid", f, null, "") }
        val eventos = listOf(
            e("1", hoy.plusDays(3)),   // nuevo
            e("2", hoy.plusDays(5)),   // ya existe, misma fecha
            e("3", hoy.plusDays(9)),   // ya existe, el docente cambió la fecha
            e("4", hoy.minusDays(1)),  // nuevo pero ya venció: no se importa
            e("5", hoy),               // vence hoy: sí se importa
        )
        val existentes = mapOf("aula:2" to hoy.plusDays(5), "aula:3" to hoy.plusDays(7))
        val plan = PlanSincronizacionAula.calcular(eventos, existentes, hoy)
        assertEquals(listOf("1", "5"), plan.nuevos.map { it.uid })
        assertEquals(1, plan.sinCambios)
        assertEquals(1, plan.pasados)
        assertEquals(listOf(CambioFechaAula("aula:3", "T3", hoy.plusDays(7), hoy.plusDays(9))), plan.cambiosDeFecha)
    }

    @Test
    fun unaActividadSinFechaQueAhoraTieneFechaEsUnCambio() {
        val hoy = LocalDate.of(2026, 11, 20)
        val plan = PlanSincronizacionAula.calcular(listOf(EventoAula("9", "Tarea", hoy, null, "")), mapOf("aula:9" to null), hoy)
        assertEquals(1, plan.cambiosDeFecha.size)
        assertNull(plan.cambiosDeFecha[0].fechaAnterior)
    }
}
