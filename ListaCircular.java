// LISTA CIRCULAR: ciclo de turnos de riego entre sectores (el último apunta al primero)
public class ListaCircular {

    private static class Nodo {
        String sector;
        Nodo siguiente;
        Nodo(String sector) { this.sector = sector; }
    }

    private Nodo ultimo; // ultimo.siguiente es el primero
    private Nodo turno;  // sector que está regando ahora
    private int tamano;

    public void agregar(String sector) {
        Nodo nuevo = new Nodo(sector);
        if (ultimo == null) {
            nuevo.siguiente = nuevo; // se apunta a sí mismo
            ultimo = nuevo;
            turno = nuevo;
        } else {
            nuevo.siguiente = ultimo.siguiente; // apunta al primero
            ultimo.siguiente = nuevo;
            ultimo = nuevo;
        }
        tamano++;
    }

    public String sectorActual() {
        return turno == null ? "(sin sectores)" : turno.sector;
    }

    public String avanzar() {
        if (turno != null) turno = turno.siguiente; // al llegar al final vuelve al inicio
        return sectorActual();
    }

    public String mostrar() {
        if (turno == null) return "Ciclo vacío.";
        StringBuilder sb = new StringBuilder("Ciclo de riego: ");
        Nodo n = turno;
        do {
            sb.append(n.sector);
            if (n == turno) sb.append(" (riega ahora)");
            sb.append(" -> ");
            n = n.siguiente;
        } while (n != turno);
        return sb.append("vuelve a ").append(turno.sector).toString();
    }

    public int getTamano() { return tamano; }
}