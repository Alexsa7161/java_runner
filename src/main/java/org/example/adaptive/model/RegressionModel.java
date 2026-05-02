package org.example.adaptive.model;

import org.apache.commons.math3.linear.*;

public class RegressionModel {

    private RealVector beta; // коэффициенты модели
    private final double lambda;

    public RegressionModel(double lambda) {
        this.lambda = lambda;
    }

    public static double[] buildExtendedVector(double[] x) {
        int n = x.length;
        double[] phi = new double[1 + 2 * n];

        phi[0] = 1.0; // bias

        // линейные
        System.arraycopy(x, 0, phi, 1, n);

        // квадратичные
        for (int i = 0; i < n; i++) {
            phi[1 + n + i] = x[i] * x[i];
        }

        return phi;
    }

    public double predict(double[] features) {
        if (beta == null) {
            throw new IllegalStateException("Model is not trained yet");
        }

        RealVector x = new ArrayRealVector(features);
        return beta.dotProduct(x);
    }

    public void train(double[][] X_data, double[] T_data) {

        RealMatrix X = new Array2DRowRealMatrix(X_data);
        RealVector T = new ArrayRealVector(T_data);

        int n = X.getColumnDimension();

        // X^T
        RealMatrix Xt = X.transpose();

        // X^T X
        RealMatrix XtX = Xt.multiply(X);

        // λI
        RealMatrix identity = MatrixUtils.createRealIdentityMatrix(n);
        RealMatrix regularized = XtX.add(identity.scalarMultiply(lambda));

        // (X^T X + λI)^-1
        DecompositionSolver solver = new SingularValueDecomposition(regularized).getSolver();
        RealMatrix inverse = solver.getInverse();

        // X^T T
        RealVector XtT = Xt.operate(T);

        // β = inverse * XtT
        this.beta = inverse.operate(XtT);
    }

    public boolean isTrained() {
        return beta != null;
    }

    public double[] getCoefficients() {
        return beta != null ? beta.toArray() : null;
    }
}