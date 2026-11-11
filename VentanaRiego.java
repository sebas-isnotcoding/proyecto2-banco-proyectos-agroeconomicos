import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class VentanaRiego extends JFrame {

    private final ModeloRiego modelo = new ModeloRiego();
    private final DefaultTableModel modeloTabla =
            new DefaultTableModel(new String[]{"S1", "S2", "S3", "S4", "Caudal b (L/s)"}, 4);
    private final JTable tabla = new JTable(modeloTabla);
    private final JTextArea salida = new JTextArea(18, 60);

    private final JTextField txtTol = new JTextField("0.001", 6);
    private final JTextField txtIter = new JTextField("50", 6);
    private final JComboBox<Integer> cmbSector = new JComboBox<>(new Integer[]{1, 2, 3, 4});
    private final JComboBox<String> cmbCausa = new JComboBox<>(ModeloRiego.CAUSAS);
    private final JTextField txtFuga = new JTextField("2", 6);
    private final JComboBox<Integer> cmbSectorHum = new JComboBox<>(new Integer[]{1, 2, 3, 4});
    private final JTextField txtHumedad = new JTextField("35", 6);
    private final JLabel lblTurno = new JLabel();

    public VentanaRiego() {
        super("Simulador de Riego por Goteo");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        salida.setEditable(false);
        salida.setLineWrap(true);
        salida.setWrapStyleWord(true);
        salida.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        salida.setMargin(new Insets(6, 8, 6, 8));

        tabla.setRowHeight(24);
        tabla.setPreferredScrollableViewportSize(new Dimension(520, 96)); // se ven las 4 filas
        cargarTabla();
        actualizarTurno();

        JScrollPane scrollTabla = new JScrollPane(tabla);
        scrollTabla.setBorder(BorderFactory.createTitledBorder(
                "Datos del sistema: cuánto se afectan los sectores entre sí (S1 a S4) y caudal que debe recibir cada uno"));

        JScrollPane scrollSalida = new JScrollPane(salida);
        scrollSalida.setBorder(BorderFactory.createTitledBorder("Resultados (aquí se explica todo)"));

        JPanel centro = new JPanel(new BorderLayout(6, 6));
        centro.add(scrollTabla, BorderLayout.NORTH);
        centro.add(scrollSalida, BorderLayout.CENTER);

        add(centro, BorderLayout.CENTER);
        add(panelLateral(), BorderLayout.EAST);
        pack();
        setLocationRelativeTo(null);

        log(bienvenida());
    }

    private String bienvenida() {
        return "BIENVENIDO AL SIMULADOR DE RIEGO\n"
                + "Calcula con qué fuerza (presión) llega el agua a 4 sectores de cultivo.\n\n"
                + "Cómo usarlo:\n"
                + " 1. Pulse \"Calcular presiones\" para ver cómo está el sistema.\n"
                + " 2. Registre una fuga (dónde, causa y cuánta agua pierde) y vea sus consecuencias.\n"
                + " 3. Guarde lecturas de humedad del suelo y revíselas.\n"
                + " 4. Use \"Siguiente turno\" para ver qué sector riega.\n\n"
                + "Estados: NORMAL = bien | ATENCIÓN = llega con menos fuerza de la ideal | CRÍTICO = revisar ya.";
    }

    private JPanel panelLateral() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JPanel sol = grupo("1. Calcular presión (vectores, lista simple, pila)");
        sol.add(new JLabel("Precisión (tolerancia):")); sol.add(txtTol);
        sol.add(new JLabel("Vueltas máximas:"));        sol.add(txtIter);
        sol.add(boton("Calcular presiones", this::calcular));
        sol.add(boton("Deshacer último cálculo", () -> log(modelo.deshacer())));
        sol.add(boton("Ver pasos del cálculo", () -> log(modelo.getPasos())));
        sol.add(new JLabel());

        JPanel fug = grupo("2. Registrar una fuga (lista doble)");
        fug.add(new JLabel("¿Dónde? Sector:"));         fug.add(cmbSector);
        fug.add(new JLabel("¿Qué la causó?"));          fug.add(cmbCausa);
        fug.add(new JLabel("¿Cuánta agua pierde (L/s)?")); fug.add(txtFuga);
        fug.add(boton("Registrar fuga", this::registrarFuga));
        fug.add(boton("Ver historial de fugas", () -> log(modelo.historialFugas())));

        JPanel hum = grupo("3. Humedad del suelo (cola)");
        hum.add(new JLabel("Sector medido:"));          hum.add(cmbSectorHum);
        hum.add(new JLabel("Humedad (%):"));            hum.add(txtHumedad);
        hum.add(boton("Guardar lectura", this::guardarHumedad));
        hum.add(boton("Revisar lecturas", () -> log(modelo.procesarHumedad())));

        JPanel sec = grupo("4. Turnos de riego (lista circular)");
        sec.add(lblTurno);
        sec.add(boton("Siguiente turno", () -> {
            modelo.getSectores().avanzar();
            actualizarTurno();
            log(modelo.getSectores().mostrar());
        }));

        txtTol.setToolTipText("Qué tan exacto debe ser el cálculo. Más pequeño = más preciso (ej. 0.001)");
        txtIter.setToolTipText("Máximo de vueltas de ajuste (ej. 50)");
        txtFuga.setToolTipText("Litros por segundo que se escapan. Debe ser menor al caudal del sector");
        txtHumedad.setToolTipText("Porcentaje de agua en el suelo, de 0 a 100");

        p.add(sol); p.add(fug); p.add(hum); p.add(sec);
        return p;
    }

    private JPanel grupo(String titulo) {
        JPanel g = new JPanel(new GridLayout(0, 2, 4, 4));
        g.setBorder(BorderFactory.createTitledBorder(titulo));
        return g;
    }

    // Todos los botones pasan por aquí: así los avisos de error se muestran en ventana
    private JButton boton(String texto, Runnable accion) {
        JButton b = new JButton(texto);
        b.addActionListener(e -> {
            try {
                accion.run();
            } catch (NumberFormatException ex) {
                aviso("Escriba solo números en los campos (use punto para decimales, ej. 0.5).");
            } catch (RuntimeException ex) {
                aviso(ex.getMessage() == null ? "Ocurrió un error inesperado." : ex.getMessage());
            }
        });
        return b;
    }

    private void calcular() {
        leerTabla();
        double tol = Double.parseDouble(txtTol.getText().trim());
        int iter = Integer.parseInt(txtIter.getText().trim());
        log(modelo.resolver(tol, iter));
    }

    private void registrarFuga() {
        leerTabla();
        log(modelo.registrarFuga((Integer) cmbSector.getSelectedItem(),
                Double.parseDouble(txtFuga.getText().trim()),
                (String) cmbCausa.getSelectedItem()));
        cargarTabla(); // refleja el nuevo caudal del sector
    }

    private void guardarHumedad() {
        modelo.registrarHumedad((Integer) cmbSectorHum.getSelectedItem(),
                Double.parseDouble(txtHumedad.getText().trim()));
        log("Lectura guardada. Lecturas por revisar: " + modelo.getLecturasPendientes());
    }

    private void leerTabla() {
        if (tabla.isEditing()) tabla.getCellEditor().stopCellEditing();
        double[][] A = new double[4][4];
        double[] b = new double[4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++)
                A[i][j] = Double.parseDouble(tabla.getValueAt(i, j).toString().trim());
            b[i] = Double.parseDouble(tabla.getValueAt(i, 4).toString().trim());
        }
        modelo.setSistema(A, b);
    }

    private void cargarTabla() {
        double[][] A = modelo.getMatriz();
        double[] b = modelo.getB();
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) modeloTabla.setValueAt(A[i][j], i, j);
            modeloTabla.setValueAt(b[i], i, 4);
        }
    }

    private void actualizarTurno() {
        lblTurno.setText("Riega ahora: " + modelo.getSectores().sectorActual());
    }

    private void log(String texto) {
        salida.append(texto + "\n\n");
        salida.setCaretPosition(salida.getDocument().getLength());
    }

    private void aviso(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Aviso", JOptionPane.WARNING_MESSAGE);
    }
}