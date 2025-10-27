package com.trademaster.portfolio.service.impl;

import com.trademaster.portfolio.dto.TaxCalculationRequest;
import com.trademaster.portfolio.dto.TaxReport;
import com.trademaster.portfolio.dto.TaxReport.TransactionTaxDetail;
import com.trademaster.portfolio.entity.Position;
import com.trademaster.portfolio.service.IndianTaxCalculationService;
import com.trademaster.portfolio.service.PositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Indian Tax Calculation Service Implementation
 *
 * Implements Indian tax calculations for equity trading:
 * - STCG: 15% for equity held <1 year
 * - LTCG: 10% for gains >₹1 lakh for equity held ≥1 year
 * - STT: 0.025% for equity delivery, 0.1% for options
 *
 * Rule #1: Java 24 + Virtual Threads for async operations
 * Rule #3: Functional programming - no if-else, no loops
 * Rule #5: Max 15 lines per method, cognitive complexity ≤7
 * Rule #10: Lombok for boilerplate reduction
 * Rule #11: No try-catch, functional error handling
 * Rule #12: Virtual Thread executor for all async operations
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndianTaxCalculationServiceImpl implements IndianTaxCalculationService {

    // Indian Tax Rates (as of FY 2024-25)
    private static final BigDecimal STCG_RATE = BigDecimal.valueOf(0.15); // 15%
    private static final BigDecimal LTCG_RATE = BigDecimal.valueOf(0.10); // 10%
    private static final BigDecimal LTCG_EXEMPTION = BigDecimal.valueOf(100000); // ₹1 lakh
    private static final BigDecimal STT_EQUITY_DELIVERY = BigDecimal.valueOf(0.00025); // 0.025%
    private static final BigDecimal STT_EQUITY_INTRADAY = BigDecimal.valueOf(0.00025); // 0.025%
    private static final BigDecimal STT_OPTIONS = BigDecimal.valueOf(0.001); // 0.1%
    private static final BigDecimal STT_FUTURES = BigDecimal.valueOf(0.0001); // 0.01%
    private static final int LONG_TERM_HOLDING_DAYS = 365;

    private final PositionService positionService;
    private final Executor virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public CompletableFuture<TaxImpact> calculateTaxImpact(TaxCalculationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Calculating tax impact for symbol: {} quantity: {}", request.symbol(), request.quantity());

            BigDecimal grossPnL = request.calculateGrossPnL();
            return buildTaxImpact(request, grossPnL);
        }, virtualExecutor);
    }

    /**
     * Build tax impact from request
     * Rule #5: Extracted method - complexity: 6 (orchestration)
     */
    private TaxImpact buildTaxImpact(TaxCalculationRequest request, BigDecimal grossPnL) {
        boolean isLongTerm = request.isLongTerm();
        String taxCategory = isLongTerm ? "LTCG" : "STCG";

        BigDecimal capitalGain = calculateCapitalGain(grossPnL, isLongTerm);
        BigDecimal taxRate = isLongTerm ? LTCG_RATE : STCG_RATE;
        BigDecimal taxAmount = calculateTaxAmount(capitalGain, taxRate);
        BigDecimal sttAmount = calculateSTT(request);
        BigDecimal totalTax = taxAmount.add(sttAmount);
        BigDecimal netPnL = grossPnL.subtract(totalTax);

        return new TaxImpact(
            request.symbol(),
            grossPnL,
            capitalGain,
            taxCategory,
            taxRate,
            taxAmount,
            sttAmount,
            totalTax,
            netPnL,
            buildTaxExplanation(taxCategory, capitalGain, taxAmount, sttAmount, request.getHoldingDays())
        );
    }

    /**
     * Calculate capital gain (after LTCG exemption if applicable)
     * Rule #3: Pattern matching for LTCG exemption
     * Rule #5: Extracted method - complexity: 3
     */
    private BigDecimal calculateCapitalGain(BigDecimal grossPnL, boolean isLongTerm) {
        return switch (grossPnL.compareTo(BigDecimal.ZERO)) {
            case -1 -> BigDecimal.ZERO; // Loss - no capital gain
            case 0 -> BigDecimal.ZERO; // Break-even
            default -> isLongTerm
                ? applyLtcgExemption(grossPnL)
                : grossPnL; // STCG - no exemption
        };
    }

    /**
     * Apply LTCG exemption of ₹1 lakh
     * Rule #5: Extracted method - complexity: 2
     */
    private BigDecimal applyLtcgExemption(BigDecimal grossPnL) {
        return grossPnL.subtract(LTCG_EXEMPTION).max(BigDecimal.ZERO);
    }

    /**
     * Calculate tax amount on capital gain
     * Rule #5: Extracted method - complexity: 2
     */
    private BigDecimal calculateTaxAmount(BigDecimal capitalGain, BigDecimal taxRate) {
        return capitalGain.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate STT based on transaction type
     * Rule #3: Pattern matching for transaction types
     * Rule #5: Extracted method - complexity: 4
     */
    private BigDecimal calculateSTT(TaxCalculationRequest request) {
        BigDecimal transactionValue = request.sellPrice().multiply(BigDecimal.valueOf(request.quantity()));

        return switch (request.transactionType()) {
            case "DELIVERY" -> transactionValue.multiply(STT_EQUITY_DELIVERY);
            case "INTRADAY" -> transactionValue.multiply(STT_EQUITY_INTRADAY);
            case "OPTIONS" -> transactionValue.multiply(STT_OPTIONS);
            case "FUTURES" -> transactionValue.multiply(STT_FUTURES);
            default -> BigDecimal.ZERO;
        };
    }

    /**
     * Build tax explanation message
     * Rule #5: Extracted method - complexity: 4
     */
    private String buildTaxExplanation(
            String taxCategory,
            BigDecimal capitalGain,
            BigDecimal taxAmount,
            BigDecimal sttAmount,
            int holdingDays) {

        String categoryExplanation = switch (taxCategory) {
            case "LTCG" -> String.format(
                "Long-term holding (%d days). First ₹1L exempt, 10%% tax on remaining gains.",
                holdingDays
            );
            case "STCG" -> String.format(
                "Short-term holding (%d days). 15%% tax on total gains.",
                holdingDays
            );
            default -> "Unknown tax category";
        };

        return String.format(
            "%s Capital Gain: ₹%.2f, Tax: ₹%.2f, STT: ₹%.2f",
            categoryExplanation,
            capitalGain,
            taxAmount,
            sttAmount
        );
    }

    @Override
    public CompletableFuture<TaxReport> generateTaxReport(Long portfolioId, String financialYear) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Generating tax report for portfolio: {} FY: {}", portfolioId, financialYear);

            // TODO: Fetch realized transactions for the financial year from database
            // For now, return empty report
            return TaxReport.empty(portfolioId, financialYear);
        }, virtualExecutor);
    }

    @Override
    public CompletableFuture<BigDecimal> calculateRealizedGainsTax(
            Long portfolioId,
            String symbol,
            Integer quantity,
            BigDecimal sellPrice) {

        return CompletableFuture.supplyAsync(() -> {
            log.info("Calculating realized gains tax for portfolio: {} symbol: {}", portfolioId, symbol);

            Position position = positionService.getPosition(portfolioId, symbol);
            return calculatePositionTax(position, quantity, sellPrice);
        }, virtualExecutor);
    }

    /**
     * Calculate tax for position sale
     * Rule #5: Extracted method - complexity: 5
     */
    private BigDecimal calculatePositionTax(Position position, Integer quantity, BigDecimal sellPrice) {
        BigDecimal purchasePrice = position.getAverageCost();
        int holdingDays = calculateHoldingDays(position);
        boolean isLongTerm = holdingDays >= LONG_TERM_HOLDING_DAYS;

        TaxCalculationRequest request = buildTaxRequest(
            position.getSymbol(),
            quantity,
            sellPrice,
            purchasePrice,
            position.getOpenedAt(),
            "EQUITY",
            "DELIVERY"
        );

        BigDecimal grossPnL = request.calculateGrossPnL();
        BigDecimal capitalGain = calculateCapitalGain(grossPnL, isLongTerm);
        BigDecimal taxRate = isLongTerm ? LTCG_RATE : STCG_RATE;

        return calculateTaxAmount(capitalGain, taxRate);
    }

    /**
     * Calculate holding days for position
     * Rule #5: Extracted method - complexity: 2
     */
    private int calculateHoldingDays(Position position) {
        return (int) java.time.Duration.between(position.getOpenedAt(), Instant.now()).toDays();
    }

    /**
     * Build tax calculation request from position
     * Rule #5: Extracted method - complexity: 2
     */
    private TaxCalculationRequest buildTaxRequest(
            String symbol,
            Integer quantity,
            BigDecimal sellPrice,
            BigDecimal purchasePrice,
            Instant purchaseDate,
            String assetType,
            String transactionType) {

        return new TaxCalculationRequest(
            symbol,
            quantity,
            sellPrice,
            purchasePrice,
            purchaseDate,
            assetType,
            transactionType
        );
    }
}
