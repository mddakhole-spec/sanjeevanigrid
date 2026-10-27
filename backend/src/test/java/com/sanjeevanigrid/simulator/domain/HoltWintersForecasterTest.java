package com.sanjeevanigrid.simulator.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HoltWintersForecaster.
 * Tests forecast accuracy, MAPE computation, backtest, and depletion estimation.
 */
class HoltWintersForecasterTest {

    private HoltWintersForecaster forecaster;
    private double[] syntheticData;

    @BeforeEach
    void setUp() {
        forecaster = new HoltWintersForecaster(0.3, 0.05, 0.15);

        // Generate sufficient synthetic data (at least 2 weeks = 336 hours)
        SyntheticDataEngine engine = new SyntheticDataEngine(42L);
        List<HourlyDataPoint> wardData = engine.generate(
                "TestWard", 4.0, 50, 8, 4, 10, 5000, List.of());
        syntheticData = wardData.stream().mapToDouble(HourlyDataPoint::admissions).toArray();
    }

    @Test
    @DisplayName("Forecast produces 72 data points by default")
    void forecastProduces72Points() {
        HoltWintersForecaster.ForecastResult result = forecaster.forecast(syntheticData);

        assertEquals(72, result.forecasts().length);
        assertEquals(72, result.upperBand().length);
        assertEquals(72, result.lowerBand().length);
    }

    @Test
    @DisplayName("All forecast values are non-negative")
    void forecastValuesAreNonNegative() {
        HoltWintersForecaster.ForecastResult result = forecaster.forecast(syntheticData);

        for (int i = 0; i < result.forecasts().length; i++) {
            assertTrue(result.forecasts()[i] >= 0,
                    "Forecast at hour " + i + " should be non-negative");
            assertTrue(result.lowerBand()[i] >= 0,
                    "Lower band at hour " + i + " should be non-negative");
        }
    }

    @Test
    @DisplayName("Upper band is always >= point forecast >= lower band")
    void confidenceBandsContainForecast() {
        HoltWintersForecaster.ForecastResult result = forecaster.forecast(syntheticData);

        for (int i = 0; i < result.forecasts().length; i++) {
            assertTrue(result.upperBand()[i] >= result.forecasts()[i],
                    "Upper band should be >= forecast at hour " + i);
            assertTrue(result.forecasts()[i] >= result.lowerBand()[i],
                    "Forecast should be >= lower band at hour " + i);
        }
    }

    @Test
    @DisplayName("Confidence band widens over forecast horizon")
    void confidenceBandWidensOverTime() {
        HoltWintersForecaster.ForecastResult result = forecaster.forecast(syntheticData);

        double firstWidth = result.upperBand()[0] - result.lowerBand()[0];
        double lastWidth = result.upperBand()[71] - result.lowerBand()[71];

        assertTrue(lastWidth > firstWidth,
                "Confidence band should widen over time");
    }

    @Test
    @DisplayName("RMSE is positive and finite")
    void rmseIsPositiveAndFinite() {
        HoltWintersForecaster.ForecastResult result = forecaster.forecast(syntheticData);

        assertTrue(result.rmse() > 0, "RMSE should be positive");
        assertTrue(Double.isFinite(result.rmse()), "RMSE should be finite");
    }

    @Test
    @DisplayName("MAPE computation is correct for known values")
    void mapeComputationCorrect() {
        double[] actuals = {10, 20, 30, 40, 50};
        double[] predicted = {12, 18, 33, 38, 55};

        double mape = forecaster.computeMAPE(actuals, predicted);

        // Expected: (20%+10%+10%+5%+10%)/5 = 11%
        assertEquals(11.0, mape, 0.01);
    }

    @Test
    @DisplayName("MAPE returns NaN for empty arrays")
    void mapeEmptyArrayReturnsNaN() {
        assertTrue(Double.isNaN(forecaster.computeMAPE(new double[]{}, new double[]{})));
    }

    @Test
    @DisplayName("MAPE skips zero actuals to avoid division by zero")
    void mapeSkipsZeroActuals() {
        double[] actuals = {0, 10, 0, 20};
        double[] predicted = {5, 12, 3, 18};

        double mape = forecaster.computeMAPE(actuals, predicted);
        // Only non-zero actuals: |10-12|/10=20%, |20-18|/20=10% -> 15%
        assertEquals(15.0, mape, 0.01);
    }

    @Test
    @DisplayName("Backtest MAPE is finite and positive on synthetic data")
    void backtestMAPEIsReasonable() {
        HoltWintersForecaster.BacktestReport report = forecaster.backtest(syntheticData, 0.8);

        assertTrue(report.trainSize() > 0);
        assertTrue(report.testSize() > 0);
        assertTrue(Double.isFinite(report.mape()),
                "Backtest MAPE should be finite, got: " + report.mape());
        assertTrue(report.mape() > 0,
                "Backtest MAPE should be positive, got: " + report.mape());
        assertTrue(report.rmse() > 0);
    }

    @Test
    @DisplayName("Backtest train/test split is correct")
    void backtestSplitIsCorrect() {
        HoltWintersForecaster.BacktestReport report = forecaster.backtest(syntheticData, 0.8);

        int expectedTrain = (int)(syntheticData.length * 0.8);
        assertEquals(expectedTrain, report.trainSize());
        assertEquals(syntheticData.length - expectedTrain, report.testSize());
    }

    @Test
    @DisplayName("Hour of depletion returns correct hour")
    void hourOfDepletionCorrect() {
        // 10 available, consuming 3 per hour -> depletes at hour 3 (0+3=3, 3+3=6, 6+3=9, 9+3=12 > 10)
        double[] consumption = {3, 3, 3, 3, 3};
        int hour = forecaster.hourOfDepletion(10, consumption);

        assertTrue(hour >= 2 && hour <= 4,
                "Expected depletion around hour 3, got: " + hour);
    }

    @Test
    @DisplayName("Hour of depletion returns -1 when resource doesn't deplete")
    void hourOfDepletionNoDepletion() {
        double[] consumption = {0.1, 0.1, 0.1};
        int hour = forecaster.hourOfDepletion(100, consumption);
        assertEquals(-1, hour);
    }

    @Test
    @DisplayName("Forecast handles short data gracefully with simple fallback")
    void forecastHandlesShortData() {
        double[] shortData = {5, 3, 7, 4, 6};
        HoltWintersForecaster.ForecastResult result = forecaster.forecast(shortData);

        assertEquals(72, result.forecasts().length);
        // Simple average fallback should produce reasonable values
        for (double f : result.forecasts()) {
            assertTrue(f >= 0 && f < 100,
                    "Simple fallback forecast should be reasonable");
        }
    }

    @Test
    @DisplayName("Custom forecast horizon works correctly")
    void customHorizonWorks() {
        HoltWintersForecaster.ForecastResult result = forecaster.forecast(syntheticData, 24);
        assertEquals(24, result.forecasts().length);
    }

    @Test
    @DisplayName("Forecaster with different alpha/beta/gamma produces different results")
    void differentParametersProduceDifferentResults() {
        HoltWintersForecaster fast = new HoltWintersForecaster(0.8, 0.2, 0.3);
        HoltWintersForecaster slow = new HoltWintersForecaster(0.1, 0.01, 0.05);

        HoltWintersForecaster.ForecastResult fastResult = fast.forecast(syntheticData);
        HoltWintersForecaster.ForecastResult slowResult = slow.forecast(syntheticData);

        // They shouldn't produce identical forecasts
        boolean allSame = true;
        for (int i = 0; i < fastResult.forecasts().length; i++) {
            if (Math.abs(fastResult.forecasts()[i] - slowResult.forecasts()[i]) > 0.001) {
                allSame = false;
                break;
            }
        }
        assertFalse(allSame, "Different parameters should produce different forecasts");
    }
}
