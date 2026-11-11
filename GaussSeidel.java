// NÚCLEO MATEMÁTICO: método iterativo de Gauss-Seidel para resolver A·x = b
public class GaussSeidel {

    // Condición suficiente de convergencia: |a_ii| > suma de |a_ij| (j distinto de i)
    public static boolean esDiagonalDominante(double[][] A) {
        for (int i = 0; i < A.length; i++) {
            double suma = 0;
            for (int j = 0; j < A.length; j++) {
                if (j != i) suma += Math.abs(A[i][j]);
            }
            if (Math.abs(A[i][i]) <= suma) return false;
        }
        return true;
    }

    public static double[] resolver(double[][] A, double[] b, double tol,
                                    int maxIter, ListaSimple historial) {
        int n = b.length;
        double[] x = new double[n]; // aproximación inicial: todo en 0
        historial.limpiar();

        for (int k = 1; k <= maxIter; k++) {
            double errorMax = 0;
            for (int i = 0; i < n; i++) {
                double suma = 0;
                for (int j = 0; j < n; j++) {
                    if (j != i) suma += A[i][j] * x[j]; // x[j] ya actualizado si j < i
                }
                double nuevo = (b[i] - suma) / A[i][i];
                errorMax = Math.max(errorMax, Math.abs(nuevo - x[i]));
                x[i] = nuevo; // se actualiza en el acto (esto es lo que distingue a Gauss-Seidel)
            }
            historial.agregarAlFinal(k, x, errorMax);
            if (errorMax < tol) break;
        }
        return x;
    }
}