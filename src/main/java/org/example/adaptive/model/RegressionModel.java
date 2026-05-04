package org.example.adaptive.model;

import org.apache.commons.math3.linear.*;

import java.util.Arrays;

public class RegressionModel {

    public RealVector beta;
    private final double lambda;

    private double[] mean;
    private double[] std;

    public RegressionModel(double lambda) {
        this.lambda = lambda;
    }




    public double[] standardize(double[] x) {
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




    public double predict(double[] rawFeatures) {
        if (beta == null) {
            throw new IllegalStateException("Model is not trained yet");
        }
        double[] scaled = standardize(rawFeatures);
        double[] phi = buildExtendedVector(scaled);
        return beta.dotProduct(new ArrayRealVector(phi));
    }




    public void train(double[][] X_raw, double[] T_data) {
        int m = X_raw.length;
        int n = X_raw[0].length;

        mean = new double[n];
        std = new double[n];


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





    public double[] optimizeParameters(double[] currentRawFeatures, boolean[] adjustable) {

        if (beta == null) {
            throw new IllegalStateException("Model is not trained yet");
        }

        double[] scaled = standardize(currentRawFeatures);
        int n = scaled.length;

        double[] b = beta.toArray();


        double stepSize = 0.15;


        double[] updated = Arrays.copyOf(scaled, n);

        for (int j = 0; j < n; j++) {

            if (!adjustable[j]) continue;

            double beta_lin = b[1 + j];
            double beta_sq  = b[1 + n + j];

            double target = scaled[j];




            if (Math.abs(beta_sq) > 1e-9) {

                double x_opt = -beta_lin / (2.0 * beta_sq);


                target = scaled[j] + stepSize * (x_opt - scaled[j]);

            }



            else {

                double direction = -Math.signum(beta_lin);


                target = scaled[j] + stepSize * direction * 0.5;
            }




            target = Math.max(-3.0, Math.min(3.0, target));

            updated[j] = target;
        }

        return destandardize(updated);
    }




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