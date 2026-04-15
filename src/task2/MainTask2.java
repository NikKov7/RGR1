package task2;

import java.io.*;
import java.util.*;
import java.util.function.DoubleUnaryOperator;

public class MainTask2 {

    interface Function {
        double value(double x);
    }

    interface DerivativeCalculator {
        double derivative(Function function, double x, double eps);
    }

    static class AnalyticalFunction implements Function {
        private final DoubleUnaryOperator function;

        public AnalyticalFunction(DoubleUnaryOperator function) {
            this.function = function;
        }

        public double value(double x) {
            return function.applyAsDouble(x);
        }
    }

    static class DataPoint {
        private final double x;
        private final double y;

        public DataPoint(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }
    }

    static class TabulatedFunction implements Function {
        private final List<DataPoint> data;

        public TabulatedFunction(List<DataPoint> data) {
            this.data = new ArrayList<>(data);
            this.data.sort(Comparator.comparingDouble(DataPoint::getX));
        }

        public double value(double x) {
            if (data.isEmpty()) {
                return Double.NaN;
            }

            if (data.size() == 1) {
                return data.get(0).getY();
            }

            if (x < data.get(0).getX() || x > data.get(data.size() - 1).getX()) {
                return Double.NaN;
            }

            for (int i = 0; i < data.size() - 1; i++) {
                double x1 = data.get(i).getX();
                double x2 = data.get(i + 1).getX();

                if (x >= x1 && x <= x2) {
                    double y1 = data.get(i).getY();
                    double y2 = data.get(i + 1).getY();

                    if (x2 == x1) {
                        return y1;
                    }

                    return y1 + (y2 - y1) * (x - x1) / (x2 - x1);
                }
            }

            return Double.NaN;
        }
    }

    static class CentralDifferenceDerivative implements DerivativeCalculator {

        private double derivativeStep(Function function, double x, double h) {
            double left = function.value(x - h);
            double right = function.value(x + h);

            if (Double.isNaN(left) || Double.isNaN(right) ||
                    Double.isInfinite(left) || Double.isInfinite(right)) {
                return Double.NaN;
            }

            return (right - left) / (2.0 * h);
        }

        public double derivative(Function function, double x, double eps) {
            double h = 0.1;

            double d1 = derivativeStep(function, x, h);
            h *= 0.1;
            double d2 = derivativeStep(function, x, h);

            if (Double.isNaN(d1) || Double.isNaN(d2)) {
                return Double.NaN;
            }

            int maxIterations = 20;
            int count = 0;

            while (count < maxIterations) {
                h *= 0.1;
                double d3 = derivativeStep(function, x, h);

                if (Double.isNaN(d3) || Double.isInfinite(d3)) {
                    return d2;
                }

                if (Math.abs(d3 - d2) >= Math.abs(d2 - d1)) {
                    return d2;
                }

                if (Math.abs(d2 - d1) < eps) {
                    return d2;
                }

                d1 = d2;
                d2 = d3;
                count++;
            }

            return d2;
        }
    }

    static class TableGenerator {
        public void generateSinTable(String filename, double xStart, double xEnd, double step) throws IOException {
            try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
                for (double x = xStart; x <= xEnd + 1e-9; x += step) {
                    writer.println(format(x, 5) + " " + format(Math.sin(x), 10));
                }
            }
        }
    }

    static class TableFileReader {
        public TabulatedFunction read(String filename) throws IOException {
            List<DataPoint> points = new ArrayList<>();

            try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
                String line;

                while ((line = br.readLine()) != null) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 2) {
                        double x = Double.parseDouble(parts[0].replace(',', '.'));
                        double y = Double.parseDouble(parts[1].replace(',', '.'));
                        points.add(new DataPoint(x, y));
                    }
                }
            }

            return new TabulatedFunction(points);
        }
    }

    static class ResultWriter {
        public void writeResults(String filename,
                                 List<Double> xValues,
                                 List<Double> yValues,
                                 List<Double> derivatives) throws IOException {
            try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
                writer.println("x\tf(x)\tf'(x)");

                for (int i = 0; i < xValues.size(); i++) {
                    writer.println(
                            format(xValues.get(i), 5) + "\t" +
                                    format(yValues.get(i), 10) + "\t" +
                                    format(derivatives.get(i), 10)
                    );
                }
            }
        }
    }

    static class FunctionEvaluator {
        private final DerivativeCalculator derivativeCalculator;
        private final ResultWriter resultWriter;

        public FunctionEvaluator(DerivativeCalculator derivativeCalculator, ResultWriter resultWriter) {
            this.derivativeCalculator = derivativeCalculator;
            this.resultWriter = resultWriter;
        }

        public void evaluate(Function function,
                             String filename,
                             double xStart,
                             double xEnd,
                             double step,
                             double eps) throws IOException {

            List<Double> xValues = new ArrayList<>();
            List<Double> yValues = new ArrayList<>();
            List<Double> derivatives = new ArrayList<>();

            for (double x = xStart; x <= xEnd + 1e-9; x += step) {
                double fx = function.value(x);
                double dfx = derivativeCalculator.derivative(function, x, eps);

                if (Double.isNaN(fx) || Double.isNaN(dfx) ||
                        Double.isInfinite(fx) || Double.isInfinite(dfx)) {
                    continue;
                }

                xValues.add(x);
                yValues.add(fx);
                derivatives.add(dfx);
            }

            resultWriter.writeResults(filename, xValues, yValues, derivatives);
        }
    }

    static String format(double value, int precision) {
        String pattern = "%." + precision + "f";
        return String.format(Locale.US, pattern, value).replace('.', ',');
    }

    public static void main(String[] args) {
        double xStart = 1.5;
        double xEnd = 6.5;
        double step = 0.05;
        double eps = 1e-5;

        try {
            DerivativeCalculator derivativeCalculator = new CentralDifferenceDerivative();
            ResultWriter resultWriter = new ResultWriter();
            FunctionEvaluator evaluator = new FunctionEvaluator(derivativeCalculator, resultWriter);
            TableGenerator tableGenerator = new TableGenerator();
            TableFileReader tableFileReader = new TableFileReader();

            Function f1 = new AnalyticalFunction(
                    x -> Math.exp(-x * x) * Math.sin(x)
            );
            evaluator.evaluate(f1, "task2_function1.txt", xStart, xEnd, step, eps);

            double[] aValues = {0.5, 1.0, 1.5};
            for (double a : aValues) {
                Function f2 = new AnalyticalFunction(
                        x -> Math.exp(-a * x * x) * Math.sin(x)
                );
                evaluator.evaluate(f2, "task2_function2_a_" + a + ".txt", xStart, xEnd, step, eps);
            }

            tableGenerator.generateSinTable("task2_table.txt", xStart, xEnd, step);

            Function f3 = tableFileReader.read("task2_table.txt");
            evaluator.evaluate(f3, "task2_function3_table.txt", xStart, xEnd, step, eps);

            System.out.println("Готово");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}