package org.example.adaptive.model;

import org.apache.commons.math3.linear.*;

import java.util.Arrays;

public class RegressionModel {

    public RealVector beta;
    private final double lambda;

    private double[] mean;   // средние для каждого исходного признака
    private double[] std;    // стандартные отклонения

    public RegressionModel(double lambda) {
        this.lambda = lambda;
    }

    // =========================
    // NORMALIZATION
    // =========================
    private double[] standardize(double[] x) {
        if (mean == null || std == null) {
            throw new IllegalStateException("Model is not trained yet");
        }
        double[] scaled = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            scaled[i] = (std[i] == 0.0) ? 0.0 : (x[i] - mean[i]) / std[i];
        }
        return scaled;
    }

    private double[] destandardize(double[] x) {
        double[] raw = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            raw[i] = x[i] * std[i] + mean[i];
        }
        return raw;
    }

    // =========================
    // FEATURE EXPANSION
    // =========================
    public static double[] buildExtendedVector(double[] x) {
        int n = x.length;
        double[] phi = new double[1 + 2 * n];
        phi[0] = 1.0;
        System.arraycopy(x, 0, phi, 1, n);
        for (int i = 0; i < n; i++) {
            phi[1 + n + i] = x[i] * x[i];
        }
        return phi;
    }

    // =========================
    // PREDICTION
    // =========================
    public double predict(double[] rawFeatures) {
        if (beta == null) {
            throw new IllegalStateException("Model is not trained yet");
        }
        double[] scaled = standardize(rawFeatures);
        double[] phi = buildExtendedVector(scaled);
        return beta.dotProduct(new ArrayRealVector(phi));
    }

    // =========================
    // TRAIN
    // =========================
    public void train(double[][] X_raw, double[] T_data) {
        int m = X_raw.length;
        int n = X_raw[0].length;

        mean = new double[n];
        std = new double[n];

        // mean & std
        for (int j = 0; j < n; j++) {
            double sum = 0.0;
            for (double[] x : X_raw) {
                sum += x[j];
            }
            mean[j] = sum / m;

            double var = 0.0;
            for (double[] x : X_raw) {
                double d = x[j] - mean[j];
                var += d * d;
            }
            std[j] = Math.sqrt(var / m);
        }

        // build extended matrix from standardized features
        double[][] Phi = new double[m][1 + 2 * n];
        for (int i = 0; i < m; i++) {
            Phi[i] = buildExtendedVector(standardize(X_raw[i]));
        }

        RealMatrix X = new Array2DRowRealMatrix(Phi);
        RealVector T = new ArrayRealVector(T_data);

        RealMatrix XtX = X.transpose().multiply(X);
        RealMatrix I = MatrixUtils.createRealIdentityMatrix(X.getColumnDimension());
        RealMatrix regularized = XtX.add(I.scalarMultiply(lambda));

        DecompositionSolver solver =
                new SingularValueDecomposition(regularized).getSolver();
        this.beta = solver.getInverse().operate(X.transpose().operate(T));
    }

    // =========================
    // 🔥 PARAMETER OPTIMIZATION
    // =========================
    /**
     * Вычисляет оптимальные значения для указанных управляемых параметров,
     * минимизируя предсказанное моделью время выполнения.
     *
     * @param currentRawFeatures текущие сырые значения ВСЕХ признаков
     * @param adjustable         маска управляемых признаков (true = можно менять)
     * @return новый вектор сырых признаков с изменёнными управляемыми параметрами
     */
    public double[] optimizeParameters(double[] currentRawFeatures, boolean[] adjustable) {
        if (beta == null) {
            throw new IllegalStateException("Model is not trained yet");
        }
        if (currentRawFeatures.length != mean.length) {
            throw new IllegalArgumentException("Feature vector length mismatch");
        }
        if (adjustable.length != currentRawFeatures.length) {
            throw new IllegalArgumentException("Adjustable mask length mismatch");
        }

        double[] scaled = standardize(currentRawFeatures);
        int n = scaled.length;   // число исходных признаков

        // Вектор beta: [β0, β1..βn (linear), β_{1,1}..β_{n,n} (quadratic)]
        double[] b = beta.toArray();

        for (int j = 0; j < n; j++) {
            if (!adjustable[j]) {
                continue;   // динамические или неконтролируемые параметры не трогаем
            }

            double beta_lin  = b[1 + j];        // линейный коэффициент
            double beta_sq   = b[1 + n + j];    // квадратичный коэффициент

            if (Math.abs(beta_sq) > 1e-9) {
                // Парабола ветвями вверх: ищем вершину
                double x_opt_std = -beta_lin / (2.0 * beta_sq);
                scaled[j] = x_opt_std;
            } else if (beta_lin < 0) {
                // Линейная отрицательная зависимость: чем больше, тем лучше
                // (можно установить верхнюю границу, если она известна; здесь оставим текущее)
                // Заглушка: не меняем, т.к. нет информации о границах.
            } else if (beta_lin > 0) {
                // Линейная положительная зависимость: чем меньше, тем лучше
                // Аналогично, оставляем текущее.
            }
            // Если beta_lin ≈ 0 — ничего не делаем
        }

        return destandardize(scaled);
    }

    // =========================
    // ACCESS
    // =========================
    public boolean isTrained() {
        return beta != null;
    }

    public double[] getCoefficients() {
        return beta == null ? null : beta.toArray();
    }

    public double[] getMean() {
        return mean != null ? mean.clone() : null;
    }

    public double[] getStd() {
        return std != null ? std.clone() : null;
    }
}