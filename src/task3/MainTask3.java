package task3;

import edu.hws.jcm.data.Expression;
import edu.hws.jcm.data.Variable;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class MainTask3 extends JFrame {

    interface Function {
        double value(double x);
    }

    interface DerivativeCalculator {
        double derivative(Function function, double x, double eps);
    }

    static class AnalyticalFunction implements Function {
        private final String expressionText;
        private final Double aValue;
        private final edu.hws.jcm.data.Parser parser;
        private final Variable xVar;
        private Variable aVar;
        private final Expression expression;
        private final Expression symbolicDerivative;

        public AnalyticalFunction(String expressionText, Double aValue) {
            this.expressionText = expressionText;
            this.aValue = aValue;

            parser = new edu.hws.jcm.data.Parser();
            xVar = new Variable("x");
            parser.add(xVar);

            if (aValue != null) {
                aVar = new Variable("a");
                parser.add(aVar);
                aVar.setVal(aValue);
            }

            expression = parser.parse(expressionText);
            symbolicDerivative = expression.derivative(xVar);
        }

        public double value(double x) {
            xVar.setVal(x);
            if (aVar != null && aValue != null) {
                aVar.setVal(aValue);
            }
            return expression.getVal();
        }

        public double symbolicDerivative(double x) {
            xVar.setVal(x);
            if (aVar != null && aValue != null) {
                aVar.setVal(aValue);
            }
            return symbolicDerivative.getVal();
        }

        public String getExpressionText() {
            return expressionText;
        }
    }

    static class DataPoint implements Comparable<DataPoint> {
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

        public int compareTo(DataPoint other) {
            return Double.compare(this.x, other.x);
        }
    }

    static class TreeSetTabulatedFunction implements Function {
        private final TreeSet<DataPoint> data;

        public TreeSetTabulatedFunction(TreeSet<DataPoint> data) {
            this.data = data;
        }

        public double value(double x) {
            if (data.isEmpty()) {
                return Double.NaN;
            }

            List<DataPoint> points = new ArrayList<>(data);

            if (x < points.get(0).getX() || x > points.get(points.size() - 1).getX()) {
                return Double.NaN;
            }

            for (int i = 0; i < points.size() - 1; i++) {
                double x1 = points.get(i).getX();
                double x2 = points.get(i + 1).getX();

                if (x >= x1 && x <= x2) {
                    double y1 = points.get(i).getY();
                    double y2 = points.get(i + 1).getY();

                    if (x2 == x1) {
                        return y1;
                    }

                    return y1 + (y2 - y1) * (x - x1) / (x2 - x1);
                }
            }

            return Double.NaN;
        }
    }

    static class TreeMapTabulatedFunction implements Function {
        private final TreeMap<Double, Double> data;

        public TreeMapTabulatedFunction(TreeMap<Double, Double> data) {
            this.data = data;
        }

        public double value(double x) {
            if (data.isEmpty()) {
                return Double.NaN;
            }

            if (data.containsKey(x)) {
                return data.get(x);
            }

            Map.Entry<Double, Double> lower = data.floorEntry(x);
            Map.Entry<Double, Double> higher = data.ceilingEntry(x);

            if (lower == null || higher == null) {
                return Double.NaN;
            }

            double x1 = lower.getKey();
            double y1 = lower.getValue();
            double x2 = higher.getKey();
            double y2 = higher.getValue();

            if (x2 == x1) {
                return y1;
            }

            return y1 + (y2 - y1) * (x - x1) / (x2 - x1);
        }
    }

    static class NumericDerivativeCalculator implements DerivativeCalculator {

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

    static class SymbolicDerivativeCalculator implements DerivativeCalculator {
        public double derivative(Function function, double x, double eps) {
            if (function instanceof AnalyticalFunction analyticalFunction) {
                return analyticalFunction.symbolicDerivative(x);
            }
            return Double.NaN;
        }
    }

    static class CsvService {

        public void generateSinCsv(String filename, double xStart, double xEnd, double step) throws IOException {
            try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
                writer.println("x,y");
                for (double x = xStart; x <= xEnd + 1e-9; x += step) {
                    writer.println(format(x, 5) + "," + format(Math.sin(x), 10));
                }
            }
        }

        public TreeMapTabulatedFunction readTreeMapFunction(String filename) throws IOException {
            TreeMap<Double, Double> data = new TreeMap<>();

            try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
                String line;
                boolean first = true;

                while ((line = br.readLine()) != null) {
                    if (first) {
                        first = false;
                        if (line.toLowerCase().contains("x") && line.toLowerCase().contains("y")) {
                            continue;
                        }
                    }

                    String[] parts = line.split(",");
                    if (parts.length >= 2) {
                        double x = Double.parseDouble(parts[0].trim().replace(',', '.'));
                        double y = Double.parseDouble(parts[1].trim().replace(',', '.'));
                        data.put(x, y);
                    }
                }
            }

            return new TreeMapTabulatedFunction(data);
        }

        public TreeSetTabulatedFunction readTreeSetFunction(String filename) throws IOException {
            TreeSet<DataPoint> data = new TreeSet<>();

            try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
                String line;
                boolean first = true;

                while ((line = br.readLine()) != null) {
                    if (first) {
                        first = false;
                        if (line.toLowerCase().contains("x") && line.toLowerCase().contains("y")) {
                            continue;
                        }
                    }

                    String[] parts = line.split(",");
                    if (parts.length >= 2) {
                        double x = Double.parseDouble(parts[0].trim().replace(',', '.'));
                        double y = Double.parseDouble(parts[1].trim().replace(',', '.'));
                        data.add(new DataPoint(x, y));
                    }
                }
            }

            return new TreeSetTabulatedFunction(data);
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

    private final JRadioButton manualInputRadio = new JRadioButton("Ручне введення", true);
    private final JRadioButton csvInputRadio = new JRadioButton("Зчитування CSV");
    private final JTextField functionField = new JTextField("exp(-x)*sin(x)");
    private final JTextField aField = new JTextField("");
    private final JTextField xStartField = new JTextField("1,5");
    private final JTextField xEndField = new JTextField("6,5");
    private final JTextField stepField = new JTextField("0,05");
    private final JCheckBox symbolicDerivativeCheck = new JCheckBox("Символьна похідна", true);
    private final JCheckBox treeSetCheck = new JCheckBox("Використати TreeSet");
    private final JButton generateCsvButton = new JButton("Генерувати CSV");
    private final JButton buildButton = new JButton("Сформувати");
    private final JButton exitButton = new JButton("Вихід");
    private final ChartPanel chartPanel;
    private final CsvService csvService = new CsvService();
    private final ResultWriter resultWriter = new ResultWriter();
    private static final String CSV_FILE = "task3_table.csv";

    public MainTask3() {
        setTitle("Візуалізація функцій");
        setSize(950, 650);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new GridLayout(3, 1));

        ButtonGroup group = new ButtonGroup();
        group.add(manualInputRadio);
        group.add(csvInputRadio);

        JPanel radioPanel = new JPanel();
        radioPanel.add(manualInputRadio);
        radioPanel.add(csvInputRadio);
        radioPanel.add(symbolicDerivativeCheck);
        radioPanel.add(treeSetCheck);
        topPanel.add(radioPanel);

        JPanel functionPanel = new JPanel(new GridLayout(1, 4, 8, 8));
        functionPanel.add(new JLabel("Функція:"));
        functionPanel.add(functionField);
        functionPanel.add(new JLabel("a:"));
        functionPanel.add(aField);
        topPanel.add(functionPanel);

        JPanel rangePanel = new JPanel(new GridLayout(1, 6, 8, 8));
        rangePanel.add(new JLabel("Початок"));
        rangePanel.add(xStartField);
        rangePanel.add(new JLabel("Кінець"));
        rangePanel.add(xEndField);
        rangePanel.add(new JLabel("Крок"));
        rangePanel.add(stepField);
        topPanel.add(rangePanel);

        add(topPanel, BorderLayout.NORTH);

        XYSeriesCollection dataset = new XYSeriesCollection();
        JFreeChart chart = ChartFactory.createXYLineChart(
                "f(x) і f'(x)",
                "x",
                "y",
                dataset
        );

        chartPanel = new ChartPanel(chart);
        add(chartPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(generateCsvButton);
        bottomPanel.add(buildButton);
        bottomPanel.add(exitButton);
        add(bottomPanel, BorderLayout.SOUTH);

        manualInputRadio.addActionListener(e -> switchMode());
        csvInputRadio.addActionListener(e -> switchMode());

        generateCsvButton.addActionListener(e -> generateCsv());
        buildButton.addActionListener(e -> buildPlot());
        exitButton.addActionListener(e -> System.exit(0));

        switchMode();
    }

    private void switchMode() {
        boolean manual = manualInputRadio.isSelected();
        functionField.setEnabled(manual);
        aField.setEnabled(manual);
        symbolicDerivativeCheck.setEnabled(manual);
    }

    private void generateCsv() {
        try {
            double xStart = parseNumber(xStartField.getText());
            double xEnd = parseNumber(xEndField.getText());
            double step = parseNumber(stepField.getText());

            csvService.generateSinCsv(CSV_FILE, xStart, xEnd, step);
            JOptionPane.showMessageDialog(this, "CSV-файл створено: " + CSV_FILE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Помилка генерації CSV: " + ex.getMessage());
        }
    }

    private void buildPlot() {
        try {
            double xStart = parseNumber(xStartField.getText());
            double xEnd = parseNumber(xEndField.getText());
            double step = parseNumber(stepField.getText());
            double eps = 1e-5;

            Function function;
            DerivativeCalculator derivativeCalculator;

            if (manualInputRadio.isSelected()) {
                String expr = functionField.getText().trim();
                Double aValue = null;

                if (expr.contains("a")) {
                    String text = aField.getText().trim();
                    if (text.isEmpty()) {
                        throw new IllegalArgumentException("Для функції з параметром a введіть значення a.");
                    }
                    aValue = parseNumber(text);
                }

                AnalyticalFunction analyticalFunction = new AnalyticalFunction(expr, aValue);
                function = analyticalFunction;

                if (symbolicDerivativeCheck.isSelected()) {
                    derivativeCalculator = new SymbolicDerivativeCalculator();
                } else {
                    derivativeCalculator = new NumericDerivativeCalculator();
                }
            } else {
                File file = new File(CSV_FILE);
                if (!file.exists()) {
                    generateCsv();
                }

                if (treeSetCheck.isSelected()) {
                    function = csvService.readTreeSetFunction(CSV_FILE);
                } else {
                    function = csvService.readTreeMapFunction(CSV_FILE);
                }

                derivativeCalculator = new NumericDerivativeCalculator();
            }

            XYSeries functionSeries = new XYSeries("f(x)");
            XYSeries derivativeSeries = new XYSeries("f'(x)");

            List<Double> xValues = new ArrayList<>();
            List<Double> yValues = new ArrayList<>();
            List<Double> dValues = new ArrayList<>();

            for (double x = xStart; x <= xEnd + 1e-9; x += step) {
                double y = function.value(x);
                double dy = derivativeCalculator.derivative(function, x, eps);

                if (Double.isNaN(y) || Double.isNaN(dy) ||
                        Double.isInfinite(y) || Double.isInfinite(dy)) {
                    continue;
                }

                functionSeries.add(x, y);
                derivativeSeries.add(x, dy);

                xValues.add(x);
                yValues.add(y);
                dValues.add(dy);
            }

            XYSeriesCollection dataset = new XYSeriesCollection();
            dataset.addSeries(functionSeries);
            dataset.addSeries(derivativeSeries);
            chartPanel.getChart().getXYPlot().setDataset(dataset);

            String outputName = manualInputRadio.isSelected() ? "task3_manual_output.txt" : "task3_csv_output.txt";
            resultWriter.writeResults(outputName, xValues, yValues, dValues);

            JOptionPane.showMessageDialog(this, "Результати побудовано і збережено у " + outputName);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Помилка: " + ex.getMessage());
        }
    }

    static double parseNumber(String text) {
        return Double.parseDouble(text.trim().replace(',', '.'));
    }

    static String format(double value, int precision) {
        String pattern = "%." + precision + "f";
        return String.format(Locale.US, pattern, value).replace('.', ',');
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainTask3().setVisible(true));
    }
}