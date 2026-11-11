import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;

// Reúne TODAS las estructuras obligatorias y las reglas del problema
public class ModeloRiego {

    // Causas posibles de una fuga (se eligen en una lista, no se escriben)
    public static final String[] CAUSAS = {
            "Emisor de goteo roto",
            "Manguera desconectada",
            "Tubería perforada",
            "Unión floja",
            "Válvula con filtración"
    };

    // ARREGLO BIDIMENSIONAL: coeficientes de pérdida de carga y fricción entre sectores
    private double[][] A = {
            {10, -2, -1, 0},
            {-2, 12, -3, -1},
            {-1, -3, 11, -2},
            {0, -1, -2, 9}
    };

    // ARREGLOS UNIDIMENSIONALES
    private double[] b = {20, 30, 25, 18};                       // caudal que debe recibir cada sector (L/s)
    private double[] presiones = new double[4];                  // presión calculada (m.c.a.)
    private final double[] presionObjetivo = {3.5, 4.5, 4.5, 3.5};

    // LISTAS ENLAZADAS (simple, doble y circular)
    private final ListaSimple aproximaciones = new ListaSimple();
    private final ListaDoble fugas = new ListaDoble();
    private final ListaCircular sectores = new ListaCircular();

    // PILA (LIFO): estados previos de presión
    private final Deque<double[]> estadosPrevios = new ArrayDeque<>();

    // COLA (FIFO): lecturas de humedad por revisar
    private static class Lectura {
        final int sector;
        final double valor;
        Lectura(int sector, double valor) { this.sector = sector; this.valor = valor; }
    }
    private final Queue<Lectura> lecturasHumedad = new LinkedList<>();

    public ModeloRiego() {
        for (int i = 1; i <= 4; i++) sectores.agregar("Sector " + i);
    }

    public void setSistema(double[][] nuevaA, double[] nuevoB) {
        A = nuevaA;
        b = nuevoB;
    }

    // ---------- Cálculo de presiones ----------

    public String resolver(double tol, int maxIter) {
        if (tol <= 0 || maxIter <= 0)
            throw new IllegalArgumentException("La tolerancia y las iteraciones deben ser mayores que 0.");
        if (!GaussSeidel.esDiagonalDominante(A))
            throw new IllegalArgumentException(
                    "Los datos de la tabla no permiten un cálculo confiable: en cada fila, el número de la "
                            + "diagonal debe ser mayor que la suma de los demás.");

        estadosPrevios.push(presiones.clone()); // guarda el estado ANTES de sobrescribirlo
        presiones = GaussSeidel.resolver(A, b, tol, maxIter, aproximaciones);

        StringBuilder sb = new StringBuilder("=== ESTADO DE LA PRESIÓN DEL AGUA ===\n");
        if (aproximaciones.getUltimoError() < tol) {
            sb.append("Cálculo listo (").append(aproximaciones.getTamano()).append(" vueltas de ajuste).\n");
        } else {
            sb.append("AVISO: se agotaron las vueltas antes de lograr la precisión pedida; "
                    + "el resultado es aproximado.\n");
        }
        sb.append(describirPresiones(presiones));
        return sb.toString();
    }

    // Traduce los números a una explicación sencilla por sector
    private String describirPresiones(double[] p) {
        StringBuilder sb = new StringBuilder("(Presión en m.c.a.: qué tan fuerte llega el agua)\n");
        StringBuilder revisar = new StringBuilder();
        for (int i = 0; i < p.length; i++) {
            double pct = Math.abs(p[i] - presionObjetivo[i]) / presionObjetivo[i] * 100;
            boolean baja = p[i] < presionObjetivo[i];
            String estado;
            if (pct <= 5) {
                estado = "NORMAL: el agua llega con buena fuerza";
            } else if (pct <= 10) {
                estado = "ATENCIÓN: llega con " + (baja ? "menos" : "más") + " fuerza de la ideal";
            } else {
                estado = "CRÍTICO: presión muy " + (baja ? "baja" : "alta") + ", revisar el sector";
            }
            if (pct > 5) revisar.append(revisar.length() > 0 ? ", " : "").append(i + 1);
            sb.append(String.format("Sector %d: %.2f (ideal %.2f) -> %s%n", i + 1, p[i], presionObjetivo[i], estado));
        }
        sb.append(revisar.length() == 0
                ? "Conclusión: todos los sectores están bien.\n"
                : "Conclusión: conviene revisar el/los sector(es) " + revisar + ".\n");
        return sb.toString();
    }

    public String getPasos() {
        if (aproximaciones.getTamano() == 0)
            return "Aún no hay cálculo. Pulse \"Calcular presiones\" primero.";
        return "=== PASOS DEL CÁLCULO (cada fila es una vuelta; el error va bajando) ===\n"
                + aproximaciones.mostrar();
    }

    public String deshacer() {
        if (estadosPrevios.isEmpty()) return "No hay cálculos anteriores para deshacer.";
        presiones = estadosPrevios.pop();
        boolean todoCero = true;
        for (double v : presiones) if (v != 0) todoCero = false;
        if (todoCero) return "Se volvió al estado inicial (todavía sin cálculo).";
        return "Se restauró el cálculo anterior:\n" + describirPresiones(presiones);
    }

    // ---------- Fugas ----------

    // Presiones "de prueba" para comparar antes y después de una fuga (no altera el estado)
    private double[] presionesEstimadas() {
        if (!GaussSeidel.esDiagonalDominante(A)) return null;
        return GaussSeidel.resolver(A, b, 1e-6, 500, new ListaSimple());
    }

    public String registrarFuga(int sector, double perdida, String causa) {
        if (sector < 1 || sector > b.length)
            throw new IllegalArgumentException("Elija un sector válido (1 a 4).");
        if (perdida <= 0 || perdida >= b[sector - 1])
            throw new IllegalArgumentException(String.format(
                    "La pérdida debe ser mayor que 0 y menor que el caudal actual del sector %d (%.2f L/s).",
                    sector, b[sector - 1]));
        if (causa == null || causa.trim().isEmpty())
            throw new IllegalArgumentException("Elija la causa de la fuga.");

        double caudalAntes = b[sector - 1];
        double[] antes = presionesEstimadas();
        b[sector - 1] -= perdida; // la fuga reduce el caudal que llega al sector
        double[] despues = presionesEstimadas();

        double pct = perdida / caudalAntes * 100;
        String gravedad;
        String queHacer;
        if (pct < 10) {
            gravedad = "LEVE";
            queHacer = "Programar la reparación en los próximos días.";
        } else if (pct < 25) {
            gravedad = "MODERADA";
            queHacer = "Reparar pronto: las plantas del sector recibirán menos agua.";
        } else {
            gravedad = "GRAVE";
            queHacer = "Reparar de inmediato: el cultivo puede quedarse sin riego suficiente.";
        }

        String consecuencia = String.format(
                "el sector pasa de recibir %.1f a %.1f L/s (pierde el %.0f%% del agua). Gravedad: %s.",
                caudalAntes, b[sector - 1], pct, gravedad);
        fugas.agregar(sector, perdida, causa, consecuencia);

        StringBuilder sb = new StringBuilder("=== FUGA REGISTRADA ===\n");
        sb.append("Dónde: Sector ").append(sector).append("\n");
        sb.append("Causa: ").append(causa).append("\n");
        sb.append(String.format("Pérdida: %.2f L/s%n", perdida));
        sb.append("Consecuencia: ").append(consecuencia).append("\n");
        if (antes != null && despues != null) {
            int peor = 0;
            for (int i = 1; i < antes.length; i++) {
                if (antes[i] - despues[i] > antes[peor] - despues[peor]) peor = i;
            }
            sb.append(String.format("Efecto en la presión: baja en todos los sectores; el más afectado es el "
                            + "Sector %d (de %.2f a %.2f m.c.a.).%n",
                    peor + 1, antes[peor], despues[peor]));
        }
        sb.append("Qué hacer: ").append(queHacer).append("\n");
        return sb.toString();
    }

    public String historialFugas() {
        if (fugas.getTamano() == 0) return "Aún no hay fugas registradas.";
        return fugas.mostrarAdelante() + fugas.mostrarAtras()
                + String.format("Pérdida total acumulada: %.2f L/s", fugas.totalPerdida());
    }

    // ---------- Humedad (cola) ----------

    public void registrarHumedad(int sector, double humedad) {
        if (sector < 1 || sector > 4)
            throw new IllegalArgumentException("Elija un sector válido (1 a 4).");
        if (humedad < 0 || humedad > 100)
            throw new IllegalArgumentException("La humedad debe estar entre 0 y 100 %.");
        lecturasHumedad.offer(new Lectura(sector, humedad));
    }

    public String procesarHumedad() {
        if (lecturasHumedad.isEmpty()) return "No hay lecturas guardadas. Use \"Guardar lectura\" primero.";
        StringBuilder sb = new StringBuilder("=== REVISIÓN DE HUMEDAD (en el orden en que se guardaron) ===\n");
        int n = 1;
        while (!lecturasHumedad.isEmpty()) {
            Lectura l = lecturasHumedad.poll();
            String estado;
            if (l.valor < 40) estado = "SUELO SECO: conviene regar este sector";
            else if (l.valor > 80) estado = "SUELO SATURADO: suspender el riego";
            else estado = "HUMEDAD ÓPTIMA: no hace falta regar";
            sb.append(String.format("Lectura %d | Sector %d | %.1f %% -> %s%n", n++, l.sector, l.valor, estado));
        }
        return sb.toString();
    }

    public double[][] getMatriz() { return A; }
    public double[] getB() { return b; }
    public ListaCircular getSectores() { return sectores; }
    public int getLecturasPendientes() { return lecturasHumedad.size(); }
}