package com.sanjeevanigrid.simulator.domain;

import java.util.Arrays;
import java.util.List;

/**
 * Holt-Winters style triple exponential smoothing forecaster with
 * hour-of-week seasonality (168 seasonal periods).
 *
 * Produces a 72-hour inflow forecast with confidence bands and
 * hour-of-depletion estimates per resource.
 */
public class HoltWintersForecaster {

    private static final int SEASON_LENGTH = 168; // hours in a week

    private final double alpha; // level smoothing
    private final double beta;  // trend smoothing
    private final double gamma; // seasonal smoothing

    public HoltWintersForecaster(double alpha, double beta, double gamma) {
        this.alpha = alpha;
        this.beta = beta;
        this.gamma = gamma;
    }

    public HoltWintersForecaster() {
        this(0.3, 0.05, 0.15);
    }

    /**
     * Fit the model on historical hourly admission data and forecast 72 hours.
     *
     * @param historicalAdmissions hourly admission counts (at least 2 * SEASON_LENGTH)
     * @return ForecastResult with point forecasts and confidence bands
     */
    public ForecastResult forecast(double[] historicalAdmissions) {
        return forecast(historicalAdmissions, 72);
    }

    public ForecastResult forecast(double[] historicalAdmissions, int horizonHours) {
        int n = historicalAdmissions.length;

        if (n < SEASON_LENGTH * 2) {
            // Fallback: not enough data for seasonal decomposition – use simple moving average
            return simpleForecast(historicalAdmissions, horizonHours);
        }

        // Initialize level and trend from first two seasons
        double[] seasonal = new double[SEASON_LENGTH];
        double level;
        double trend;

        // Average of first season
        double firstSeasonAvg = 0;
        for (int i = 0; i < SEASON_LENGTH; i++) {
            firstSeasonAvg += historicalAdmissions[i];
        }
        firstSeasonAvg /= SEASON_LENGTH;

        double secondSeasonAvg = 0;
        for (int i = SEASON_LENGTH; i < 2 * SEASON_LENGTH; i++) {
            secondSeasonAvg += historicalAdmissions[i];
        }
        secondSeasonAvg /= SEASON_LENGTH;

        level = firstSeasonAvg;
        trend = (secondSeasonAvg - firstSeasonAvg) / SEASON_LENGTH;

        // Initialize seasonal indices (multiplicative)
        for (int i = 0; i < SEASON_LENGTH; i++) {
            double avg = 0;
            int count = 0;
            for (int j = i; j < n; j += SEASON_LENGTH) {
                avg += historicalAdmissions[j];
                count++;
            }
            seasonal[i] = (count > 0 && firstSeasonAvg > 0) ? (avg / count) / firstSeasonAvg : 1.0;
        }

        // Fit model over training data, collecting one-step-ahead errors
        double[] fitted = new double[n];
        double sumSquaredError = 0;
        int errorCount = 0;

        for (int t = 0; t < n; t++) {
            int seasonIdx = t % SEASON_LENGTH;
            double y = historicalAdmissions[t];
            double s = seasonal[seasonIdx];

            if (s == 0) s = 0.001;

            double newLevel = alpha * (y / s) + (1 - alpha) * (level + trend);
            double newTrend = beta * (newLevel - level) + (1 - beta) * trend;
            double newSeasonal = gamma * (y / newLevel) + (1 - gamma) * s;

            fitted[t] = (level + trend) * s;

            if (t >= SEASON_LENGTH) {
                double error = y - fitted[t];
                sumSquaredError += error * error;
                errorCount++;
            }

            level = newLevel;
            trend = newTrend;
            seasonal[seasonIdx] = newSeasonal;
        }

        double rmse = errorCount > 0 ? Math.sqrt(sumSquaredError / errorCount) : 1.0;

        // Generate forecasts
        double[] forecasts = new double[horizonHours];
        double[] upper = new double[horizonHours];
        double[] lower = new double[horizonHours];

        for (int h = 1; h <= horizonHours; h++) {
            int seasonIdx = (n + h) % SEASON_LENGTH;
            double forecast = (level + h * trend) * seasonal[seasonIdx];
            forecast = Math.max(0, forecast);

            // Confidence band widens with horizon
            double halfWidth = 1.96 * rmse * Math.sqrt(h);
            forecasts[h - 1] = forecast;
            upper[h - 1] = Math.max(0, forecast + halfWidth);
            lower[h - 1] = Math.max(0, forecast - halfWidth);
        }

        return new ForecastResult(forecasts, upper, lower, rmse);
    }

    /**
     * Estimate when a resource will deplete given current level, consumption rate, and forecast.
     *
     * @param currentAvailable current available count
     * @param hourlyConsumption forecasted hourly consumption array
     * @return hour offset when resource depletes, or -1 if it doesn't within the forecast window
     */
    public int hourOfDepletion(int currentAvailable, double[] hourlyConsumption) {
        double remaining = currentAvailable;
        for (int h = 0; h < hourlyConsumption.length; h++) {
            remaining -= hourlyConsumption[h];
            if (remaining <= 0) return h;
        }
        return -1; // does not deplete within forecast window
    }

    private ForecastResult simpleForecast(double[] data, int horizon) {
        int windowSize = Math.min(24, data.length);
        double avg = 0;
        for (int i = data.length - windowSize; i < data.length; i++) {
            avg += data[i];
        }
        avg /= windowSize;

        double[] forecasts = new double[horizon];
        double[] upper = new double[horizon];
        double[] lower = new double[horizon];
        Arrays.fill(forecasts, Math.max(0, avg));
        for (int h = 0; h < horizon; h++) {
            upper[h] = Math.max(0, avg * 1.5);
            lower[h] = Math.max(0, avg * 0.5);
        }
        return new ForecastResult(forecasts, upper, lower, avg * 0.2);
    }

    /**
     * Compute MAPE (Mean Absolute Percentage Error) on a held-out test set.
     */
    public double computeMAPE(double[] actuals, double[] predicted) {
        if (actuals.length != predicted.length || actuals.length == 0) return Double.NaN;
        double sum = 0;
        int count = 0;
        for (int i = 0; i < actuals.length; i++) {
            if (actuals[i] > 0) {
                sum += Math.abs(actuals[i] - predicted[i]) / actuals[i];
                count++;
            }
        }
        return count > 0 ? (sum / count) * 100.0 : Double.NaN;
    }

    /**
     * Run a backtest: train on first trainPct of data, forecast the rest, report MAPE.
     */
    public BacktestReport backtest(double[] allData, double trainPct) {
        int trainSize = (int)(allData.length * trainPct);
        int testSize = allData.length - trainSize;

        double[] train = Arrays.copyOfRange(allData, 0, trainSize);
        double[] testActual = Arrays.copyOfRange(allData, trainSize, allData.length);

        ForecastResult result = forecast(train, testSize);
        double mape = computeMAPE(testActual, result.forecasts());

        return new BacktestReport(trainSize, testSize, mape, result.rmse());
    }

    public record ForecastResult(
            double[] forecasts,
            double[] upperBand,
            double[] lowerBand,
            double rmse
    ) {}

    public record BacktestReport(
            int trainSize,
            int testSize,
            double mape,
            double rmse
    ) {}
}
