// LISTA SIMPLEMENTE ENLAZADA: guarda las aproximaciones de presión de cada iteración
public class ListaSimple {

    private static class Nodo {
        int iteracion;
        double[] valores;
        double error;
        Nodo siguiente; // un solo enlace: hacia adelante

        Nodo(int iteracion, double[] valores, double error) {
            this.iteracion = iteracion;
            this.valores = valores.clone(); // copia, para que no cambie después
            this.error = error;
        }
    }

    private Nodo cabeza, ultimo;
    private int tamano;

    public void agregarAlFinal(int iteracion, double[] valores, double error) {
        Nodo nuevo = new Nodo(iteracion, valores, error);
        if (cabeza == null) {
            cabeza = nuevo;
            ultimo = nuevo;
        } else {
            ultimo.siguiente = nuevo;
            ultimo = nuevo;
        }
        tamano++;
    }

    public void limpiar() { cabeza = null; ultimo = null; tamano = 0; }
    public int getTamano() { return tamano; }
    public double getUltimoError() { return ultimo == null ? 0 : ultimo.error; }

    public String mostrar() {
        StringBuilder sb = new StringBuilder();
        for (Nodo n = cabeza; n != null; n = n.siguiente) {
            sb.append(String.format("Iter %2d | ", n.iteracion));
            for (double v : n.valores) sb.append(String.format("%9.4f ", v));
            sb.append(String.format("| error %.5f%n", n.error));
        }
        return sb.toString();
    }
}