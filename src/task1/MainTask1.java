package task1;

import java.io.*;
import java.util.*;

public class MainTask1 {

    interface Evaluatable {
        double evalf(double x);
    }

    static class AnalyticalFunction implements Evaluatable {
        private double a;

        public AnalyticalFunction(double a) {
            this.a = a;
        }

        public double evalf(double x) {
            return Math.exp(-a * x * x) * Math.sin(x);
        }
    }

    static class Point2D {
        double x;
        double y;

        public Point2D(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    static class TabulatedFunction implements Evaluatable {
        private final List<Point2D> data = new ArrayList<>();

        public void addPoint(double x, double y) {
            data.add(new Point2D(x, y));
        }

        public void readFromFile(String filename) throws IOException {
            data.clear();
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line;

            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 2) {
                    double x = Double.parseDouble(parts[0].replace(',', '.'));
                    double y = Double.parseDouble(parts[1].replace(',', '.'));
                    addPoint(x, y);
                }
            }

            br.close();
            data.sort(Comparator.comparingDouble(p -> p.x));
        }

        public double evalf(double x) {
            if (data.isEmpty()) {
                return Double.NaN;
            }

            if (data.size() == 1) {
                return data.get(0).y;
            }

            if (x < data.get(0).x || x > data.get(data.size() - 1).x) {
                return Double.NaN;
            }

            for (int i = 0; i < data.size() - 1; i++) {
                double x1 = data.get(i).x;
                double x2 = data.get(i + 1).x;

                if (x >= x1 && x <= x2) {
                    double y1 = data.get(i).y;
                    double y2 = data.get(i + 1).y;

                    if (x2 == x1) {
                        return y1;
                    }

                    return y1 + (y2 - y1) * (x - x1) / (x2 - x1);
                }
            }

            return Double.NaN;
        }
    }

    static class NumMethods {
        private static double derivativeStep(Evaluatable f, double x, double h) {
            double right = f.evalf(x + h);
            double left = f.evalf(x - h);

            if (Double.isNaN(right) || Double.isNaN(left) ||
                    Double.isInfinite(right) || Double.isInfinite(left)) {
                return Double.NaN;
            }

            return (right - left) / (2.0 * h);
        }

        public static double derivative(Evaluatable f, double x, double eps) {
            double h = 0.1;

            double d1 = derivativeStep(f, x, h);
            h *= 0.1;
            double d2 = derivativeStep(f, x, h);

            if (Double.isNaN(d1) || Double.isNaN(d2)) {
                return Double.NaN;
            }

            int maxIterations = 20;
            int count = 0;

            while (count < maxIterations) {
                h *= 0.1;
                double d3 = derivativeStep(f, x, h);

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

    static String format(double value, int precision) {
        String pattern = "%." + precision + "f";
        return String.format(Locale.US, pattern, value).replace('.', ',');
    }

    static void generateTable(String filename, double start, double end, double step) throws IOException {
        PrintWriter writer = new PrintWriter(new FileWriter(filename));

        for (double x = start; x <= end + 1e-9; x += step) {
            writer.println(format(x, 5) + " " + format(Math.sin(x), 10));
        }

        writer.close();
    }

    static void computeAndSave(Evaluatable f,
                               String filename,
                               double start,
                               double end,
                               double step,
                               double eps) throws IOException {

        PrintWriter writer = new PrintWriter(new FileWriter(filename));
        writer.println("x\tf(x)\tf'(x)");

        for (double x = start; x <= end + 1e-9; x += step) {
            double fx = f.evalf(x);
            double dfx = NumMethods.derivative(f, x, eps);

            if (Double.isNaN(fx) || Double.isNaN(dfx) ||
                    Double.isInfinite(fx) || Double.isInfinite(dfx)) {
                continue;
            }

            writer.println(
                    format(x, 5) + "\t" +
                            format(fx, 10) + "\t" +
                            format(dfx, 10)
            );
        }

        writer.close();
    }

    public static void main(String[] args) {
        double xStart = 1.5;
        double xEnd = 6.5;
        double step = 0.05;
        double eps = 1e-5;

        try {
            Evaluatable f1 = new AnalyticalFunction(1.0);
            computeAndSave(f1, "function1.txt", xStart, xEnd, step, eps);

            double[] aValues = {0.5, 1.0, 1.5};
            for (double a : aValues) {
                Evaluatable f2 = new AnalyticalFunction(a);
                computeAndSave(f2, "function2_a_" + a + ".txt", xStart, xEnd, step, eps);
            }

            generateTable("table.txt", xStart, xEnd, step);

            TabulatedFunction f3 = new TabulatedFunction();
            f3.readFromFile("table.txt");
            computeAndSave(f3, "function3_table.txt", xStart, xEnd, step, eps);

            System.out.println("Готово");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}