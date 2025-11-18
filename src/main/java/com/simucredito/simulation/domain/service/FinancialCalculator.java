package com.simucredito.simulation.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class FinancialCalculator {

    private static final MathContext MATH_CONTEXT = MathContext.DECIMAL128;
    private static final int SCALE = 6;

    /**
     * Calcula la tasa efectiva mensual (TEM) a partir de TE o TN
     * (Asume que la tasa de entrada es ANUAL)
     */
    public BigDecimal calculateMonthlyEffectiveRate(BigDecimal annualRate, String rateType, String capitalizationPeriod) {
        double rateDouble = annualRate.divide(BigDecimal.valueOf(100), MATH_CONTEXT).doubleValue();
        double temDouble;

        if ("TE".equals(rateType)) {
            // TE Anual a TEM
            // Fórmula: TEM = (1 + TEA)^(30/360) - 1
            double base = 1.0 + rateDouble;
            double exponent = 30.0 / 360.0;
            temDouble = Math.pow(base, exponent) - 1.0;

        } else if ("TN".equals(rateType)) {
            // TN Anual a TEM
            // Fórmula: TEM = (1 + (TNA / m))^(m/12) - 1
            int m = getCapitalizationsPerYear(capitalizationPeriod);
            double n = (double) m / 12.0;
            double base = 1.0 + (rateDouble / m);
            temDouble = Math.pow(base, n) - 1.0;

        } else {
            throw new IllegalArgumentException("Invalid rate type: " + rateType);
        }

        // Devolvemos el BigDecimal con la precisión del contexto, pero el 'setScale' final
        // debe hacerlo el servicio que lo llama si necesita una escala específica.
        return BigDecimal.valueOf(temDouble);
    }

    /**
     * Convierte cualquier tasa a TEM (Tasa Efectiva Mensual)
     */
    public BigDecimal convertToTEM(BigDecimal rate, String rateType, String period, String capitalization) {
        double rateDouble = rate.divide(BigDecimal.valueOf(100), MATH_CONTEXT).doubleValue(); // Tasa en decimal
        double temDouble;

        if ("TE".equals(rateType)) {
            // Fórmula: TEM = (1 + TEP)^(n_2/n_1) - 1
            // n_2 = 30 (días mes), n_1 = días del período conocido
            double base = 1.0 + rateDouble; // (1 + TEP)
            double n1 = (double) getDaysInPeriod(period); // Días en periodo conocido (e.g., 360)
            double n2 = 30.0; // Días en periodo buscado (mes)
            double exponent = n2 / n1; // (30 / 360)

            temDouble = Math.pow(base, exponent) - 1.0;

        } else if ("TN".equals(rateType)) {
            // Fórmula: TEM = (1 + j/m)^n - 1
            // j = Tasa Nominal Anual (TNA)
            // m = frecuencia de capitalización (por año)
            // n = número de períodos de capitalización en un mes (m / 12)

            // 1. Convertir la tasa nominal del período (e.g., TN Semestral) a Tasa Nominal Anual (j)
            double j_tasa_nominal_anual = rateDouble * (360.0 / getDaysInPeriod(period));

            // 2. 'm' = frecuencia de capitalización (por año)
            String capPeriod = (capitalization != null ? capitalization : period);
            int m = getCapitalizationsPerYear(capPeriod);

            // 3. 'n' = número de períodos de capitalización en un mes (m / 12)
            double n = (double) m / 12.0;

            // Fórmula: TEM = (1 + (j / m))^n - 1
            double base = 1.0 + (j_tasa_nominal_anual / m);
            temDouble = Math.pow(base, n) - 1.0;

        } else {
            throw new IllegalArgumentException("Invalid rate type: " + rateType);
        }

        return BigDecimal.valueOf(temDouble);
    }

    /**
     * Calcula el COK (Costo de Oportunidad del Capital)
     */
    public BigDecimal calculateCOK(BigDecimal opportunityCostRate, String opportunityCostType,
                                   String opportunityCostPeriod, String opportunityCostCapitalization) {
        return convertToTEM(opportunityCostRate, opportunityCostType, opportunityCostPeriod, opportunityCostCapitalization);
    }

    private int getDaysInPeriod(String period) {
        return switch (period.toLowerCase()) {
            case "daily" -> 1;
            case "seminal", "bi-weekly" -> 15;
            case "monthly" -> 30;
            case "bi-monthly" -> 60;
            case "quarterly" -> 90;
            case "semi-annually" -> 180;
            case "annual" -> 360;
            default -> 30; // default to monthly
        };
    }

    /**
     * Devuelve el número de períodos de capitalización por año (360 días)
     */
    private int getCapitalizationsPerYear(String period) {
        if (period == null) return 12; // Default a mensual si es nulo
        return switch (period.toLowerCase()) {
            case "daily" -> 360;
            case "seminal", "bi-weekly" -> 24; // 360 / 15
            case "monthly" -> 12;
            case "bi-monthly" -> 6;
            case "quarterly" -> 4;
            case "semi-annually" -> 2;
            case "annual" -> 1;
            default -> 12; // default a mensual
        };
    }

    /**
     * Calcula la cuota mensual usando el método francés (amortización constante)
     */
    public BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal monthlyRate, int termMonths) {
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            // Si no hay interés, es una división simple
            return principal.divide(BigDecimal.valueOf(termMonths), MATH_CONTEXT);
        }

        // Fórmula del método francés: P * (r * (1 + r)^n) / ((1 + r)^n - 1)
        BigDecimal rateFactor = monthlyRate.add(BigDecimal.ONE).pow(termMonths, MATH_CONTEXT);
        BigDecimal numerator = monthlyRate.multiply(rateFactor, MATH_CONTEXT);
        BigDecimal denominator = rateFactor.subtract(BigDecimal.ONE, MATH_CONTEXT);

        return principal.multiply(numerator, MATH_CONTEXT)
                .divide(denominator, MATH_CONTEXT)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula el VAN (Valor Actual Neto) usando COK
     */
    public BigDecimal calculateVAN(BigDecimal monthlyPayment, BigDecimal cokRate, int termMonths, BigDecimal initialInvestment) {
        BigDecimal van = BigDecimal.ZERO;

        // Initial investment is already negative when passed in
        van = van.add(initialInvestment, MATH_CONTEXT);

        for (int period = 1; period <= termMonths; period++) {
            BigDecimal discountedPayment = monthlyPayment.divide(
                    cokRate.add(BigDecimal.ONE).pow(period, MATH_CONTEXT), MATH_CONTEXT);
            van = van.add(discountedPayment, MATH_CONTEXT);
        }

        return van.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula el TIR usando el método de Newton-Raphson
     */
    public BigDecimal calculateTIR(BigDecimal monthlyPayment, BigDecimal principal, int termMonths, int maxIterations, double tolerance) {
        BigDecimal guess = BigDecimal.valueOf(0.01); // 1% inicial

        for (int i = 0; i < maxIterations; i++) {
            BigDecimal f = calculateNPV(guess, monthlyPayment, principal, termMonths);
            BigDecimal fPrime = calculateNPVDerivative(guess, monthlyPayment, termMonths);

            if (fPrime.abs().compareTo(BigDecimal.valueOf(tolerance)) < 0) {
                break; // Evitar división por cero
            }

            BigDecimal newGuess = guess.subtract(f.divide(fPrime, MATH_CONTEXT), MATH_CONTEXT);

            if (newGuess.subtract(guess).abs().compareTo(BigDecimal.valueOf(tolerance)) < 0) {
                return newGuess.multiply(BigDecimal.valueOf(100), MATH_CONTEXT)
                        .setScale(4, RoundingMode.HALF_UP);
            }

            guess = newGuess;
        }

        return guess.multiply(BigDecimal.valueOf(100), MATH_CONTEXT).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateNPV(BigDecimal rate, BigDecimal monthlyPayment, BigDecimal principal, int termMonths) {
        BigDecimal npv = principal.negate(); // Inversión inicial (negativa)

        for (int period = 1; period <= termMonths; period++) {
            BigDecimal discountedPayment = monthlyPayment.divide(
                    rate.add(BigDecimal.ONE).pow(period, MATH_CONTEXT), MATH_CONTEXT);
            npv = npv.add(discountedPayment, MATH_CONTEXT);
        }

        return npv;
    }

    private BigDecimal calculateNPVDerivative(BigDecimal rate, BigDecimal monthlyPayment, int termMonths) {
        BigDecimal derivative = BigDecimal.ZERO;

        for (int period = 1; period <= termMonths; period++) {
            BigDecimal discountedPayment = monthlyPayment.multiply(BigDecimal.valueOf(-period))
                    .divide(rate.add(BigDecimal.ONE).pow(period + 1, MATH_CONTEXT), MATH_CONTEXT);
            derivative = derivative.add(discountedPayment, MATH_CONTEXT);
        }

        return derivative;
    }

    /**
     * Genera el cronograma de amortización completo con todos los costos
     */
    public List<AmortizationEntry> generateAmortizationSchedule(
            BigDecimal principal, BigDecimal monthlyRate, BigDecimal monthlyPayment,
            int termMonths, Integer gracePeriodMonths, String gracePeriodType,
            BigDecimal lifeInsuranceRate, BigDecimal propertyInsuranceRate,
            BigDecimal monthlyCommissions, BigDecimal administrationCosts,
            String statementDelivery, BigDecimal propertyInsuranceValue) {

        List<AmortizationEntry> schedule = new ArrayList<>();
        BigDecimal remainingBalance = principal;
        BigDecimal cumulativePrincipal = BigDecimal.ZERO;
        BigDecimal cumulativeInterest = BigDecimal.ZERO;

        // Costos periódicos mensuales
        BigDecimal deliveryCost = "physical".equals(statementDelivery) ? BigDecimal.valueOf(10) : BigDecimal.ZERO;

        for (int period = 1; period <= termMonths; period++) {
            boolean isGracePeriod = gracePeriodMonths != null && period <= gracePeriodMonths;
            BigDecimal interestPayment;
            BigDecimal principalPayment;
            BigDecimal scheduledPayment;
            BigDecimal tem = monthlyRate;

            if (isGracePeriod) {
                if ("total".equals(gracePeriodType)) {
                    // Gracia Total: No se paga nada. Intereses se capitalizan.
                    interestPayment = remainingBalance.multiply(monthlyRate, MATH_CONTEXT);
                    principalPayment = BigDecimal.ZERO;
                    scheduledPayment = BigDecimal.ZERO; // No hay pago
                    // Los intereses se capitalizan (se suman al saldo)
                    remainingBalance = remainingBalance.add(interestPayment, MATH_CONTEXT);

                } else if ("partial".equals(gracePeriodType)) {
                    // Gracia Parcial: Se pagan solo intereses. Saldo no cambia.
                    interestPayment = remainingBalance.multiply(monthlyRate, MATH_CONTEXT);
                    principalPayment = BigDecimal.ZERO;
                    scheduledPayment = interestPayment; // El pago es solo el interés

                } else {
                    // "none" dentro de un período de gracia (no debería pasar, pero por si acaso)
                    interestPayment = remainingBalance.multiply(monthlyRate, MATH_CONTEXT);
                    principalPayment = monthlyPayment.subtract(interestPayment, MATH_CONTEXT);
                    scheduledPayment = monthlyPayment;
                }
            } else {
                // Pago normal
                interestPayment = remainingBalance.multiply(monthlyRate, MATH_CONTEXT);
                principalPayment = monthlyPayment.subtract(interestPayment, MATH_CONTEXT);

                // Ajuste en el último pago
                if (period == termMonths) {
                    principalPayment = remainingBalance;
                }

                scheduledPayment = monthlyPayment;
            }

            // Seguros
            BigDecimal lifeInsurancePayment = remainingBalance.multiply(lifeInsuranceRate, MATH_CONTEXT);
            BigDecimal propertyInsurancePayment = propertyInsuranceValue.multiply(propertyInsuranceRate, MATH_CONTEXT);

            // Costos periódicos
            BigDecimal commissions = monthlyCommissions;
            BigDecimal adminCosts = administrationCosts;
            BigDecimal deliveryCosts = deliveryCost;

            // Cuota total
            BigDecimal totalPayment = scheduledPayment.add(lifeInsurancePayment)
                    .add(propertyInsurancePayment).add(commissions).add(adminCosts).add(deliveryCosts);

            // Actualizar balances
            BigDecimal endingBalance = remainingBalance.subtract(principalPayment, MATH_CONTEXT);
            cumulativePrincipal = cumulativePrincipal.add(principalPayment, MATH_CONTEXT);
            cumulativeInterest = cumulativeInterest.add(interestPayment, MATH_CONTEXT);

            // Cash flow (flujo de caja negativo para outflows)
            BigDecimal cashFlow = totalPayment.negate();

            AmortizationEntry entry = AmortizationEntry.builder()
                    .periodNumber(period)
                    .beginningBalance(remainingBalance)
                    .scheduledPayment(totalPayment)
                    .principalPayment(principalPayment)
                    .interestPayment(interestPayment)
                    .lifeInsurancePayment(lifeInsurancePayment)
                    .propertyInsurancePayment(propertyInsurancePayment)
                    .endingBalance(endingBalance)
                    .cumulativePrincipal(cumulativePrincipal)
                    .cumulativeInterest(cumulativeInterest)
                    .isGracePeriod(isGracePeriod)
                    .build();

            schedule.add(entry);
            remainingBalance = endingBalance;
        }

        return schedule;
    }

    /**
     * Clase interna para representar una entrada del cronograma de amortización
     */
    public static class AmortizationEntry {
        private final int periodNumber;
        private final BigDecimal beginningBalance;
        private final BigDecimal scheduledPayment;
        private final BigDecimal principalPayment;
        private final BigDecimal interestPayment;
        private final BigDecimal lifeInsurancePayment;
        private final BigDecimal propertyInsurancePayment;
        private final BigDecimal endingBalance;
        private final BigDecimal cumulativePrincipal;
        private final BigDecimal cumulativeInterest;
        private final boolean isGracePeriod;

        public AmortizationEntry(int periodNumber, BigDecimal beginningBalance, BigDecimal scheduledPayment,
                               BigDecimal principalPayment, BigDecimal interestPayment,
                               BigDecimal lifeInsurancePayment, BigDecimal propertyInsurancePayment,
                               BigDecimal endingBalance, BigDecimal cumulativePrincipal,
                               BigDecimal cumulativeInterest, boolean isGracePeriod) {
            this.periodNumber = periodNumber;
            this.beginningBalance = beginningBalance;
            this.scheduledPayment = scheduledPayment;
            this.principalPayment = principalPayment;
            this.interestPayment = interestPayment;
            this.lifeInsurancePayment = lifeInsurancePayment;
            this.propertyInsurancePayment = propertyInsurancePayment;
            this.endingBalance = endingBalance;
            this.cumulativePrincipal = cumulativePrincipal;
            this.cumulativeInterest = cumulativeInterest;
            this.isGracePeriod = isGracePeriod;
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public int getPeriodNumber() { return periodNumber; }
        public BigDecimal getBeginningBalance() { return beginningBalance; }
        public BigDecimal getScheduledPayment() { return scheduledPayment; }
        public BigDecimal getPrincipalPayment() { return principalPayment; }
        public BigDecimal getInterestPayment() { return interestPayment; }
        public BigDecimal getLifeInsurancePayment() { return lifeInsurancePayment; }
        public BigDecimal getPropertyInsurancePayment() { return propertyInsurancePayment; }
        public BigDecimal getEndingBalance() { return endingBalance; }
        public BigDecimal getCumulativePrincipal() { return cumulativePrincipal; }
        public BigDecimal getCumulativeInterest() { return cumulativeInterest; }
        public boolean isGracePeriod() { return isGracePeriod; }

        public static class Builder {
            private int periodNumber;
            private BigDecimal beginningBalance;
            private BigDecimal scheduledPayment;
            private BigDecimal principalPayment;
            private BigDecimal interestPayment;
            private BigDecimal lifeInsurancePayment;
            private BigDecimal propertyInsurancePayment;
            private BigDecimal endingBalance;
            private BigDecimal cumulativePrincipal;
            private BigDecimal cumulativeInterest;
            private boolean isGracePeriod;

            public Builder periodNumber(int periodNumber) { this.periodNumber = periodNumber; return this; }
            public Builder beginningBalance(BigDecimal beginningBalance) { this.beginningBalance = beginningBalance; return this; }
            public Builder scheduledPayment(BigDecimal scheduledPayment) { this.scheduledPayment = scheduledPayment; return this; }
            public Builder principalPayment(BigDecimal principalPayment) { this.principalPayment = principalPayment; return this; }
            public Builder interestPayment(BigDecimal interestPayment) { this.interestPayment = interestPayment; return this; }
            public Builder lifeInsurancePayment(BigDecimal lifeInsurancePayment) { this.lifeInsurancePayment = lifeInsurancePayment; return this; }
            public Builder propertyInsurancePayment(BigDecimal propertyInsurancePayment) { this.propertyInsurancePayment = propertyInsurancePayment; return this; }
            public Builder endingBalance(BigDecimal endingBalance) { this.endingBalance = endingBalance; return this; }
            public Builder cumulativePrincipal(BigDecimal cumulativePrincipal) { this.cumulativePrincipal = cumulativePrincipal; return this; }
            public Builder cumulativeInterest(BigDecimal cumulativeInterest) { this.cumulativeInterest = cumulativeInterest; return this; }
            public Builder isGracePeriod(boolean isGracePeriod) { this.isGracePeriod = isGracePeriod; return this; }

            public AmortizationEntry build() {
                return new AmortizationEntry(periodNumber, beginningBalance, scheduledPayment,
                        principalPayment, interestPayment, lifeInsurancePayment, propertyInsurancePayment,
                        endingBalance, cumulativePrincipal, cumulativeInterest, isGracePeriod);
            }
        }
    }
}