// LISTA DOBLEMENTE ENLAZADA: historial de fugas (se recorre hacia adelante y hacia atrás)
public class ListaDoble {

    private static class Nodo {
        int numero;
        int sector;
        double perdida;
        String causa;
        String consecuencia;
        Nodo anterior, siguiente; // dos enlaces

        Nodo(int numero, int sector, double perdida, String causa, String consecuencia) {
            this.numero = numero;
            this.sector = sector;
            this.perdida = perdida;
            this.causa = causa;
            this.consecuencia = consecuencia;
        }
    }

    private Nodo primero, ultimo;
    private int tamano;

    public void agregar(int sector, double perdida, String causa, String consecuencia) {
        Nodo nuevo = new Nodo(tamano + 1, sector, perdida, causa, consecuencia);
        if (primero == null) {
            primero = nuevo;
            ultimo = nuevo;
        } else {
            nuevo.anterior = ultimo;
            ultimo.siguiente = nuevo;
            ultimo = nuevo;
        }
        tamano++;
    }

    // De la más antigua a la más reciente (usa el enlace "siguiente")
    public String mostrarAdelante() {
        if (primero == null) return "Aún no hay fugas registradas.";
        StringBuilder sb = new StringBuilder("=== HISTORIAL DE FUGAS (de la más antigua a la más reciente) ===\n");
        for (Nodo n = primero; n != null; n = n.siguiente) {
            sb.append(String.format("Fuga %d | Dónde: Sector %d | Causa: %s | Pierde: %.2f L/s%n",
                    n.numero, n.sector, n.causa, n.perdida));
            sb.append("   Consecuencia: ").append(n.consecuencia).append("\n");
        }
        return sb.toString();
    }

    // De la más reciente a la más antigua (usa el enlace "anterior")
    public String mostrarAtras() {
        if (ultimo == null) return "";
        StringBuilder sb = new StringBuilder("Recorrido inverso (de la más reciente a la más antigua): ");
        for (Nodo n = ultimo; n != null; n = n.anterior) {
            sb.append("Fuga ").append(n.numero).append(" (Sector ").append(n.sector).append(")");
            if (n.anterior != null) sb.append(" -> ");
        }
        return sb.append("\n").toString();
    }

    public double totalPerdida() {
        double total = 0;
        for (Nodo n = primero; n != null; n = n.siguiente) total += n.perdida;
        return total;
    }

    public int getTamano() { return tamano; }
}