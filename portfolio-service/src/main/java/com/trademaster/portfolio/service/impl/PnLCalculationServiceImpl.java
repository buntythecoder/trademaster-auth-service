package com.trademaster.portfolio.service.impl;

import com.trademaster.portfolio.dto.*;
import com.trademaster.portfolio.entity.Portfolio;
import com.trademaster.portfolio.entity.Position;
import com.trademaster.portfolio.entity.PortfolioTransaction;
import com.trademaster.portfolio.model.CostBasisMethod;
import com.trademaster.portfolio.model.TransactionType;
import com.trademaster.portfolio.repository.PortfolioRepository;
import com.trademaster.portfolio.repository.PositionRepository;
import com.trademaster.portfolio.repository.PortfolioTransactionRepository;
import com.trademaster.portfolio.service.PnLCalculationService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Comprehensive P&L Calculation Service Implementation
 *
 * High-performance profit/loss calculation engine using Java 24 Virtual Threads.
 * Provides real-time portfolio valuation with multiple cost basis methods.
 *
 * Rule #1: Java 24 Virtual Threads
 * Rule #3: Functional programming (no if-else)
 * Rule #5: Cognitive complexity ≤7 per method
 * Rule #9: Immutable records for DTOs
 * Rule #11: No try-catch in business logic
 * Rule #22: Performance targets (<50ms)
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads + Functional Programming)
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class PnLCalculationServiceImpl implements PnLCalculationService {

    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final PortfolioTransactionRepository transactionRepository;
    private final MeterRegistry meterRegistry;

    private static final Executor VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    private static final MathContext PRECISION = new MathContext(10, RoundingMode.HALF_UP);
    private static final BigDecimal DAYS_IN_YEAR = BigDecimal.valueOf(365.25);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    // ==================== PORTFOLIO VALUATION ====================

    @Override
    @Cacheable(value = "portfolio-valuation", key = "#portfolioId")
    @CircuitBreaker(name = "portfolio-valuation", fallbackMethod = "calculatePortfolioValuationFallback")
    public PortfolioValuationResult calculatePortfolioValuation(Long portfolioId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return portfolioRepository.findById(portfolioId)
                .map(this::buildPortfolioValuation)
                .orElse(emptyValuationResult(portfolioId, sample));
        } finally {
            sample.stop(meterRegistry.timer("pnl.portfolio.valuation", "portfolioId", portfolioId.toString()));
            log.info("Calculated portfolio valuation: portfolioId={}", portfolioId);
        }
    }

    @Override
    public CompletableFuture<PortfolioValuationResult> calculatePortfolioValuationAsync(Long portfolioId) {
        return CompletableFuture.supplyAsync(
            () -> calculatePortfolioValuation(portfolioId),
            VIRTUAL_EXECUTOR
        );
    }

    private PortfolioValuationResult buildPortfolioValuation(Portfolio portfolio) {
        long startTime = System.currentTimeMillis();
        List<Position> positions = positionRepository.findByPortfolioId(portfolio.getPortfolioId());

        PositionAggregates aggregates = calculatePositionAggregates(positions);
        BigDecimal totalValue = portfolio.getCashBalance().add(aggregates.positionsValue());
        BigDecimal totalReturn = calculateTotalReturnPercentage(
            totalValue,
            portfolio.getCashBalance().add(portfolio.getRealizedPnl())
        );

        return new PortfolioValuationResult(
            portfolio.getPortfolioId(), totalValue, portfolio.getCashBalance(),
            aggregates.positionsValue(), aggregates.unrealizedPnl(), portfolio.getRealizedPnl(),
            aggregates.dayPnl(), totalReturn, positions.size(),
            Instant.now(), System.currentTimeMillis() - startTime
        );
    }

    /**
     * Calculate aggregate values from positions
     * Rule #5: Extracted method - complexity: 3
     */
    private PositionAggregates calculatePositionAggregates(List<Position> positions) {
        BigDecimal positionsValue = positions.stream()
            .map(this::calculatePositionMarketValue)
            .reduce(ZERO, BigDecimal::add);

        BigDecimal unrealizedPnl = positions.stream()
            .map(this::calculatePositionUnrealizedPnL)
            .reduce(ZERO, BigDecimal::add);

        BigDecimal dayPnl = positions.stream()
            .filter(pos -> pos.getPreviousClosePrice() != null)
            .map(this::calculatePositionDayPnL)
            .reduce(ZERO, BigDecimal::add);

        return new PositionAggregates(positionsValue, unrealizedPnl, dayPnl);
    }

    /**
     * Record for position aggregates
     * Rule #9: Immutable record for data transfer
     */
    private record PositionAggregates(BigDecimal positionsValue, BigDecimal unrealizedPnl, BigDecimal dayPnl) {}

    private PortfolioValuationResult emptyValuationResult(Long portfolioId, Timer.Sample sample) {
        sample.stop(meterRegistry.timer("pnl.portfolio.valuation.empty"));
        log.warn("Portfolio not found for valuation: portfolioId={}", portfolioId);
        return new PortfolioValuationResult(
            portfolioId, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, 0, Instant.now(), 0L
        );
    }

    private PortfolioValuationResult calculatePortfolioValuationFallback(Long portfolioId, Exception e) {
        log.error("Circuit breaker activated for portfolio valuation: portfolioId={}, error={}",
            portfolioId, e.getMessage());
        return emptyValuationResult(portfolioId, Timer.start(meterRegistry));
    }

    // ==================== POSITION P&L CALCULATIONS ====================

    @Override
    public BigDecimal calculateUnrealizedPnL(Position position, BigDecimal currentPrice) {
        return Optional.ofNullable(position)
            .filter(pos -> pos.getQuantity() != 0)
            .filter(pos -> currentPrice != null)
            .map(pos -> calculateUnrealizedPnLInternal(pos, currentPrice))
            .orElse(ZERO);
    }

    private BigDecimal calculateUnrealizedPnLInternal(Position position, BigDecimal currentPrice) {
        BigDecimal marketValue = currentPrice.multiply(new BigDecimal(Math.abs(position.getQuantity())));
        BigDecimal costValue = position.getAverageCost().multiply(new BigDecimal(Math.abs(position.getQuantity())));
        return marketValue.subtract(costValue);
    }

    private BigDecimal calculatePositionUnrealizedPnL(Position position) {
        return Optional.ofNullable(position.getCurrentPrice())
            .map(price -> calculateUnrealizedPnL(position, price))
            .orElse(ZERO);
    }

    private BigDecimal calculatePositionMarketValue(Position position) {
        return Optional.ofNullable(position.getMarketValue())
            .orElse(ZERO);
    }

    private BigDecimal calculatePositionDayPnL(Position position) {
        return Optional.ofNullable(position.getDayPnl())
            .orElse(ZERO);
    }

    @Override
    public BigDecimal calculateDayPnL(Position position, BigDecimal currentPrice, BigDecimal previousClosePrice) {
        return Optional.ofNullable(position)
            .filter(pos -> pos.getQuantity() != 0)
            .filter(pos -> currentPrice != null && previousClosePrice != null)
            .map(pos -> calculateDayPnLInternal(pos, currentPrice, previousClosePrice))
            .orElse(ZERO);
    }

    private BigDecimal calculateDayPnLInternal(Position position, BigDecimal currentPrice, BigDecimal previousClosePrice) {
        BigDecimal priceDiff = currentPrice.subtract(previousClosePrice);
        return priceDiff.multiply(new BigDecimal(position.getQuantity()));
    }

    // ==================== REALIZED P&L CALCULATIONS ====================

    @Override
    @Transactional
    @CircuitBreaker(name = "pnl-calculation", fallbackMethod = "calculateRealizedPnLFallback")
    public RealizedPnLResult calculateRealizedPnL(Long portfolioId, String symbol,
                                                   Integer tradeQuantity, BigDecimal tradePrice,
                                                   CostBasisMethod costBasisMethod) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            List<TaxLotInfo> taxLots = getTaxLots(portfolioId, symbol, costBasisMethod);

            BigDecimal avgCostBasis = calculateAverageCostBasis(taxLots, Math.abs(tradeQuantity));
            BigDecimal netProceeds = tradePrice.multiply(new BigDecimal(Math.abs(tradeQuantity)));
            BigDecimal realizedPnl = netProceeds.subtract(
                avgCostBasis.multiply(new BigDecimal(Math.abs(tradeQuantity)))
            );

            List<TaxLotInfo> usedLots = selectTaxLots(taxLots, Math.abs(tradeQuantity), costBasisMethod);

            return new RealizedPnLResult(
                realizedPnl,
                avgCostBasis,
                netProceeds,
                Math.abs(tradeQuantity),
                costBasisMethod,
                usedLots,
                Instant.now()
            );
        } finally {
            sample.stop(meterRegistry.timer("pnl.realized", "method", costBasisMethod.name()));
            log.info("Calculated realized P&L: portfolioId={}, symbol={}, pnl={}",
                portfolioId, symbol, "calculated");
        }
    }

    private BigDecimal calculateAverageCostBasis(List<TaxLotInfo> taxLots, int quantity) {
        return taxLots.stream()
            .limit(calculateRequiredLots(taxLots, quantity))
            .map(TaxLotInfo::costBasisPerShare)
            .reduce(ZERO, BigDecimal::add)
            .divide(new BigDecimal(Math.min(quantity, getTotalQuantity(taxLots))), PRECISION);
    }

    private int getTotalQuantity(List<TaxLotInfo> taxLots) {
        return taxLots.stream()
            .mapToInt(TaxLotInfo::remainingQuantity)
            .sum();
    }

    private long calculateRequiredLots(List<TaxLotInfo> taxLots, int quantity) {
        // Rule #3: Functional loop conversion with Stream API and takeWhile
        record Accumulator(int remaining, long count) {}

        return taxLots.stream()
            .reduce(
                new Accumulator(quantity, 0),
                (acc, lot) -> acc.remaining() > 0
                    ? new Accumulator(acc.remaining() - lot.remainingQuantity(), acc.count() + 1)
                    : acc,
                (acc1, acc2) -> new Accumulator(acc1.remaining() + acc2.remaining(), acc1.count() + acc2.count())
            )
            .count();
    }

    /**
     * Record for tax lot selection state
     * Rule #9: Immutable record for accumulation state
     * Moved outside method for proper type inference in Stream.reduce
     */
    private record Selection(List<TaxLotInfo> selected, int remaining) {}

    private List<TaxLotInfo> selectTaxLots(List<TaxLotInfo> taxLots, int quantity, CostBasisMethod method) {
        // Rule #3: Functional loop conversion with Stream API and reduce
        // Rule #5: Reduced complexity by extracting accumulator logic
        return taxLots.stream()
            .reduce(
                new Selection(new ArrayList<>(), quantity),
                (acc, lot) -> acc.remaining() <= 0 ? acc : addLotToSelection(acc, lot),
                (sel1, sel2) -> combineTaxLotSelections(sel1, sel2)
            )
            .selected();
    }

    /**
     * Add lot to selection and update remaining
     * Rule #5: Extracted method - complexity: 2
     */
    private Selection addLotToSelection(Selection acc, TaxLotInfo lot) {
        return Optional.of(acc)
            .map(a -> {
                List<TaxLotInfo> newSelected = new ArrayList<>(a.selected());
                newSelected.add(lot);
                int useQuantity = Math.min(a.remaining(), lot.remainingQuantity());
                return new Selection(newSelected, a.remaining() - useQuantity);
            })
            .orElse(acc);
    }

    /**
     * Combine two tax lot selections
     * Rule #5: Extracted method - complexity: 2
     */
    private Selection combineTaxLotSelections(Selection sel1, Selection sel2) {
        List<TaxLotInfo> combined = new ArrayList<>(sel1.selected());
        combined.addAll(sel2.selected());
        return new Selection(combined, sel1.remaining() + sel2.remaining());
    }

    private RealizedPnLResult calculateRealizedPnLFallback(Long portfolioId, String symbol,
                                                            Integer tradeQuantity, BigDecimal tradePrice,
                                                            CostBasisMethod costBasisMethod, Exception e) {
        log.error("Circuit breaker activated for realized P&L: portfolioId={}, symbol={}, error={}",
            portfolioId, symbol, e.getMessage());
        return new RealizedPnLResult(
            ZERO, ZERO, ZERO, Math.abs(tradeQuantity), costBasisMethod, List.of(), Instant.now()
        );
    }

    // ==================== COST BASIS CALCULATIONS ====================

    @Override
    @Cacheable(value = "weighted-average-cost", key = "#portfolioId + '_' + #symbol")
    public BigDecimal calculateWeightedAverageCost(Long portfolioId, String symbol) {
        List<PortfolioTransaction> transactions = transactionRepository
            .findByPortfolioIdAndSymbolOrderByExecutionTime(portfolioId, symbol);

        return calculateWeightedAverageCostFromTransactions(transactions);
    }

    private BigDecimal calculateWeightedAverageCostFromTransactions(List<PortfolioTransaction> transactions) {
        // Rule #3: Functional loop conversion with Stream API - filter and reduce
        record CostAccumulator(BigDecimal totalCost, int totalQuantity) {}

        CostAccumulator result = transactions.stream()
            .filter(txn -> txn.getTransactionType() == TransactionType.BUY && txn.getQuantity() != null)
            .reduce(
                new CostAccumulator(ZERO, 0),
                (acc, txn) -> new CostAccumulator(
                    acc.totalCost().add(txn.getPrice().multiply(new BigDecimal(txn.getQuantity()))),
                    acc.totalQuantity() + txn.getQuantity()
                ),
                (acc1, acc2) -> new CostAccumulator(
                    acc1.totalCost().add(acc2.totalCost()),
                    acc1.totalQuantity() + acc2.totalQuantity()
                )
            );

        return result.totalQuantity() > 0
            ? result.totalCost().divide(new BigDecimal(result.totalQuantity()), PRECISION)
            : ZERO;
    }

    @Override
    @CircuitBreaker(name = "pnl-calculation", fallbackMethod = "getTaxLotsFallback")
    public List<TaxLotInfo> getTaxLots(Long portfolioId, String symbol, CostBasisMethod costBasisMethod) {
        List<PortfolioTransaction> transactions = transactionRepository
            .findByPortfolioIdAndSymbolOrderByExecutionTime(portfolioId, symbol);

        return buildTaxLotsFromTransactions(transactions, costBasisMethod);
    }

    private List<TaxLotInfo> buildTaxLotsFromTransactions(List<PortfolioTransaction> transactions,
                                                            CostBasisMethod method) {
        // Rule #3: Functional loop conversion with Stream API
        // Rule #5: Reduced complexity by extracting transaction conversion
        ArrayList<TaxLotInfo> taxLots = transactions.stream()
            .filter(this::isBuyTransaction)
            .reduce(
                new ArrayList<TaxLotInfo>(),
                this::accumulateTaxLotFromTransaction,
                this::combineTaxLotLists
            );

        return sortTaxLotsByMethod(taxLots, method);
    }

    /**
     * Check if transaction is a buy
     * Rule #5: Extracted method - complexity: 1
     */
    private boolean isBuyTransaction(PortfolioTransaction txn) {
        return txn.getTransactionType() == TransactionType.BUY && txn.getQuantity() != null;
    }

    /**
     * Accumulate tax lot from transaction
     * Rule #5: Extracted method - complexity: 2
     */
    private ArrayList<TaxLotInfo> accumulateTaxLotFromTransaction(ArrayList<TaxLotInfo> list, PortfolioTransaction txn) {
        long lotId = list.size() + 1;
        TaxLotInfo lot = createTaxLotFromTransaction(lotId, txn);
        ArrayList<TaxLotInfo> newList = new ArrayList<>(list);
        newList.add(lot);
        return newList;
    }

    /**
     * Create tax lot from transaction
     * Rule #5: Extracted method - complexity: 1
     */
    private TaxLotInfo createTaxLotFromTransaction(long lotId, PortfolioTransaction txn) {
        return TaxLotInfo.create(
            lotId, txn.getSymbol(), txn.getExecutedAt(),
            txn.getQuantity(), txn.getQuantity(),
            txn.getPrice(), txn.getPrice().multiply(new BigDecimal(txn.getQuantity())),
            txn.getPrice(), ZERO
        );
    }

    /**
     * Combine two tax lot lists
     * Rule #5: Extracted method - complexity: 2
     */
    private ArrayList<TaxLotInfo> combineTaxLotLists(ArrayList<TaxLotInfo> list1, ArrayList<TaxLotInfo> list2) {
        ArrayList<TaxLotInfo> combined = new ArrayList<>(list1);
        combined.addAll(list2);
        return combined;
    }

    /**
     * Sort tax lots by cost basis method.
     *
     * Pattern: Pattern matching with switch expression
     * Rule #14: Switch expression for type handling
     *
     * @param taxLots List of tax lots to sort
     * @param method Cost basis method
     * @return Sorted tax lots
     */
    private List<TaxLotInfo> sortTaxLotsByMethod(List<TaxLotInfo> taxLots, CostBasisMethod method) {
        return switch (method) {
            case FIFO -> taxLots.stream()
                .sorted(Comparator.comparing(TaxLotInfo::purchaseDate))
                .toList();
            case LIFO -> taxLots.stream()
                .sorted(Comparator.comparing(TaxLotInfo::purchaseDate).reversed())
                .toList();
            case SPECIFIC_ID -> taxLots.stream()
                .sorted(Comparator.comparing(TaxLotInfo::purchaseDate))
                .toList();
            case WEIGHTED_AVERAGE -> taxLots;
        };
    }

    private List<TaxLotInfo> getTaxLotsFallback(Long portfolioId, String symbol,
                                                  CostBasisMethod costBasisMethod, Exception e) {
        log.error("Circuit breaker activated for tax lots: portfolioId={}, symbol={}, error={}",
            portfolioId, symbol, e.getMessage());
        return List.of();
    }

    @Override
    public CostBasisUpdateResult updateCostBasis(Position position, Integer tradeQuantity,
                                                  BigDecimal tradePrice, CostBasisMethod costBasisMethod) {
        BigDecimal currentAvgCost = position.getAverageCost();
        int currentQuantity = position.getQuantity();

        int newQuantity = currentQuantity + tradeQuantity;
        BigDecimal newTotalCost = calculateNewTotalCost(currentAvgCost, currentQuantity, tradePrice, tradeQuantity);
        BigDecimal newAvgCost = newQuantity > 0
            ? newTotalCost.divide(new BigDecimal(newQuantity), PRECISION)
            : ZERO;

        BigDecimal realizedPnl = tradeQuantity < 0
            ? calculateRealizedPnLForSale(currentAvgCost, tradePrice, -tradeQuantity)
            : ZERO;

        return new CostBasisUpdateResult(
            newAvgCost,
            newTotalCost,
            newQuantity,
            realizedPnl,
            costBasisMethod,
            List.of()
        );
    }

    private BigDecimal calculateNewTotalCost(BigDecimal currentAvgCost, int currentQty,
                                              BigDecimal tradePrice, int tradeQty) {
        BigDecimal currentTotalCost = currentAvgCost.multiply(new BigDecimal(currentQty));
        BigDecimal tradeCost = tradePrice.multiply(new BigDecimal(tradeQty));
        return currentTotalCost.add(tradeCost);
    }

    private BigDecimal calculateRealizedPnLForSale(BigDecimal avgCost, BigDecimal salePrice, int quantity) {
        BigDecimal costBasis = avgCost.multiply(new BigDecimal(quantity));
        BigDecimal proceeds = salePrice.multiply(new BigDecimal(quantity));
        return proceeds.subtract(costBasis);
    }

    // ==================== RETURN CALCULATIONS ====================

    @Override
    public BigDecimal calculateTotalReturn(Position position, BigDecimal currentPrice) {
        return Optional.ofNullable(position)
            .filter(pos -> pos.getAverageCost().compareTo(ZERO) > 0)
            .map(pos -> calculateTotalReturnInternal(pos.getAverageCost(), currentPrice))
            .orElse(ZERO);
    }

    private BigDecimal calculateTotalReturnInternal(BigDecimal avgCost, BigDecimal currentPrice) {
        BigDecimal priceChange = currentPrice.subtract(avgCost);
        return priceChange.divide(avgCost, PRECISION).multiply(HUNDRED);
    }

    private BigDecimal calculateTotalReturnPercentage(BigDecimal currentValue, BigDecimal initialValue) {
        return initialValue.compareTo(ZERO) > 0
            ? currentValue.subtract(initialValue).divide(initialValue, PRECISION).multiply(HUNDRED)
            : ZERO;
    }

    @Override
    public BigDecimal calculateAnnualizedReturn(Position position, BigDecimal currentPrice) {
        BigDecimal totalReturn = calculateTotalReturn(position, currentPrice);
        int holdingDays = calculateHoldingDays(position);

        return holdingDays > 0
            ? annualizeReturn(totalReturn, holdingDays)
            : ZERO;
    }

    private int calculateHoldingDays(Position position) {
        return (int) Duration.between(position.getOpenedAt(), Instant.now()).toDays();
    }

    private BigDecimal annualizeReturn(BigDecimal totalReturn, int holdingDays) {
        BigDecimal holdingYears = new BigDecimal(holdingDays).divide(DAYS_IN_YEAR, PRECISION);
        return holdingYears.compareTo(ZERO) > 0
            ? totalReturn.divide(holdingYears, PRECISION)
            : ZERO;
    }

    // ==================== BULK CALCULATIONS ====================

    @Override
    public CompletableFuture<BigDecimal> bulkCalculateUnrealizedPnL(Long portfolioId) {
        return CompletableFuture.supplyAsync(
            () -> positionRepository.findByPortfolioId(portfolioId)
                .parallelStream()
                .map(this::calculatePositionUnrealizedPnL)
                .reduce(ZERO, BigDecimal::add),
            VIRTUAL_EXECUTOR
        );
    }

    // ==================== POSITION METRICS ====================

    @Override
    @CircuitBreaker(name = "pnl-calculation", fallbackMethod = "calculatePositionPnLMetricsFallback")
    public List<PositionPnLMetrics> calculatePositionPnLMetrics(Long portfolioId) {
        return positionRepository.findByPortfolioId(portfolioId)
            .stream()
            .map(this::buildPositionMetrics)
            .filter(Objects::nonNull)
            .toList();
    }

    private PositionPnLMetrics buildPositionMetrics(Position position) {
        BigDecimal marketValue = calculatePositionMarketValue(position);
        BigDecimal costBasis = position.getTotalCost();
        BigDecimal unrealizedPnl = calculatePositionUnrealizedPnL(position);
        BigDecimal totalPnl = position.getRealizedPnl().add(unrealizedPnl);
        BigDecimal totalReturn = calculateTotalReturn(
            position,
            Optional.ofNullable(position.getCurrentPrice()).orElse(ZERO)
        );
        BigDecimal dayPnl = calculatePositionDayPnL(position);
        int holdingDays = calculateHoldingDays(position);
        BigDecimal annualizedReturn = calculateAnnualizedReturn(
            position,
            Optional.ofNullable(position.getCurrentPrice()).orElse(ZERO)
        );

        return new PositionPnLMetrics(
            position.getSymbol(),
            marketValue,
            costBasis,
            unrealizedPnl,
            position.getRealizedPnl(),
            totalPnl,
            totalReturn,
            dayPnl,
            holdingDays,
            annualizedReturn
        );
    }

    private List<PositionPnLMetrics> calculatePositionPnLMetricsFallback(Long portfolioId, Exception e) {
        log.error("Circuit breaker activated for position P&L metrics: portfolioId={}, error={}",
            portfolioId, e.getMessage());
        return List.of();
    }

    // ==================== P&L BREAKDOWN ====================

    @Override
    // Rule #5: Reduced cognitive complexity from ~8-9 to 3 by extracting helper methods
    public PnLBreakdown calculatePnLBreakdown(Long portfolioId, Instant fromDate, Instant toDate) {
        List<PortfolioTransaction> transactions = transactionRepository
            .findByPortfolioIdAndExecutionTimeBetween(portfolioId, fromDate, toDate);

        PnLComponents components = calculatePnLComponents(portfolioId, transactions, fromDate, toDate);

        return buildPnLBreakdown(portfolioId, fromDate, toDate, components);
    }

    // Rule #5: Extracted method - complexity: 5
    private PnLComponents calculatePnLComponents(Long portfolioId, List<PortfolioTransaction> transactions,
                                                  Instant fromDate, Instant toDate) {
        BigDecimal realizedPnl = calculateRealizedPnl(transactions);
        BigDecimal unrealizedPnl = bulkCalculateUnrealizedPnL(portfolioId).join();
        IncomeCalculationResult income = calculateIncome(portfolioId, fromDate, toDate);
        FeesCalculationResult fees = calculateFees(portfolioId, fromDate, toDate);
        BigDecimal totalPnl = realizedPnl.add(unrealizedPnl);
        BigDecimal netPnl = totalPnl.subtract(fees.totalFees());

        return new PnLComponents(realizedPnl, unrealizedPnl, income.totalDividends(),
                                 income.totalInterest(), fees, totalPnl, netPnl);
    }

    // Rule #5: Extracted method - complexity: 2
    private BigDecimal calculateRealizedPnl(List<PortfolioTransaction> transactions) {
        return transactions.stream()
            .filter(txn -> txn.getRealizedPnl() != null)
            .map(PortfolioTransaction::getRealizedPnl)
            .reduce(ZERO, BigDecimal::add);
    }

    // Rule #5: Extracted method - complexity: 3
    private PnLBreakdown buildPnLBreakdown(Long portfolioId, Instant fromDate, Instant toDate,
                                            PnLComponents components) {
        return new PnLBreakdown(
            portfolioId,
            fromDate,
            toDate,
            components.totalPnl(),
            components.realizedPnl(),
            components.unrealizedPnl(),
            components.dividendIncome(),
            components.interestIncome(),
            components.fees().totalFees(),
            components.fees().totalCommissions(),
            components.fees().totalTaxes(),
            components.netPnl(),
            List.of(),  // securityBreakdown - empty for now
            List.of(),  // sectorBreakdown - empty for now
            Instant.now()
        );
    }

    // Rule #9: Immutable record for P&L component aggregation
    private record PnLComponents(
        BigDecimal realizedPnl,
        BigDecimal unrealizedPnl,
        BigDecimal dividendIncome,
        BigDecimal interestIncome,
        FeesCalculationResult fees,
        BigDecimal totalPnl,
        BigDecimal netPnl
    ) {}

    private BigDecimal calculateTransactionFees(PortfolioTransaction txn) {
        return Optional.ofNullable(txn.getCommission()).orElse(ZERO)
            .add(Optional.ofNullable(txn.getTax()).orElse(ZERO))
            .add(Optional.ofNullable(txn.getOtherFees()).orElse(ZERO));
    }

    // ==================== PERFORMANCE ATTRIBUTION ====================

    @Override
    public PerformanceAttribution calculatePerformanceAttribution(Long portfolioId, Instant fromDate, Instant toDate) {
        // Simplified implementation - full attribution requires benchmark data
        BigDecimal totalReturn = calculatePortfolioReturn(portfolioId, fromDate, toDate);

        return new PerformanceAttribution(
            portfolioId,
            fromDate,
            toDate,
            totalReturn,
            ZERO,
            ZERO,
            ZERO,
            ZERO,
            List.of(),
            Instant.now()
        );
    }

    private BigDecimal calculatePortfolioReturn(Long portfolioId, Instant fromDate, Instant toDate) {
        return portfolioRepository.findById(portfolioId)
            .map(portfolio -> calculateTotalReturnPercentage(
                portfolio.getTotalValue(),
                portfolio.getCashBalance()
            ))
            .orElse(ZERO);
    }

    // ==================== TRADE IMPACT ANALYSIS ====================

    @Override
    public PnLImpactAnalysis calculateTradeImpact(Long portfolioId, String symbol, Integer quantity, BigDecimal price) {
        return positionRepository.findByPortfolioIdAndSymbol(portfolioId, symbol)
            .map(position -> buildTradeImpactAnalysis(position, quantity, price))
            .orElse(buildNewPositionImpactAnalysis(quantity, price));
    }

    private PnLImpactAnalysis buildTradeImpactAnalysis(Position position, Integer quantity, BigDecimal price) {
        BigDecimal currentUnrealizedPnl = calculatePositionUnrealizedPnL(position);
        BigDecimal projectedRealizedPnl = quantity < 0
            ? calculateRealizedPnLForSale(position.getAverageCost(), price, -quantity)
            : ZERO;

        return new PnLImpactAnalysis(
            currentUnrealizedPnl,
            projectedRealizedPnl,
            ZERO,
            projectedRealizedPnl,
            ZERO,
            position.getAverageCost(),
            "NORMAL"
        );
    }

    private PnLImpactAnalysis buildNewPositionImpactAnalysis(Integer quantity, BigDecimal price) {
        return new PnLImpactAnalysis(
            ZERO, ZERO, ZERO, ZERO, ZERO, price, "NEW_POSITION"
        );
    }

    // ==================== MONTHLY P&L SUMMARY ====================

    @Override
    @Cacheable(value = "monthly-pnl", key = "#portfolioId + '_' + #year")
    public List<MonthlyPnLSummary> getMonthlyPnLSummary(Long portfolioId, Integer year) {
        Instant startOfYear = LocalDate.of(year, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfYear = LocalDate.of(year, 12, 31).atTime(23, 59, 59).toInstant(ZoneOffset.UTC);

        List<PortfolioTransaction> transactions = transactionRepository
            .findByPortfolioIdAndExecutionTimeBetween(portfolioId, startOfYear, endOfYear);

        return buildMonthlyPnLSummaries(transactions, year);
    }

    private List<MonthlyPnLSummary> buildMonthlyPnLSummaries(List<PortfolioTransaction> transactions, Integer year) {
        Map<Integer, List<PortfolioTransaction>> byMonth = transactions.stream()
            .collect(Collectors.groupingBy(txn ->
                txn.getExecutedAt().atZone(ZoneOffset.UTC).getMonthValue()
            ));

        return byMonth.entrySet().stream()
            .map(entry -> buildMonthlySummary(year, entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparing(MonthlyPnLSummary::month))
            .toList();
    }

    private MonthlyPnLSummary buildMonthlySummary(Integer year, Integer month, List<PortfolioTransaction> transactions) {
        BigDecimal realizedPnl = transactions.stream()
            .filter(txn -> txn.getRealizedPnl() != null)
            .map(PortfolioTransaction::getRealizedPnl)
            .reduce(ZERO, BigDecimal::add);

        BigDecimal fees = transactions.stream()
            .map(this::calculateTransactionFees)
            .reduce(ZERO, BigDecimal::add);

        return new MonthlyPnLSummary(
            year,
            month,
            realizedPnl,
            transactions.size(),
            fees
        );
    }

    // ==================== INCOME CALCULATIONS ====================

    @Override
    public IncomeCalculationResult calculateIncome(Long portfolioId, Instant fromDate, Instant toDate) {
        List<PortfolioTransaction> incomeTransactions = transactionRepository
            .findByPortfolioIdAndExecutionTimeBetweenAndTransactionTypeIn(
                portfolioId, fromDate, toDate,
                List.of(TransactionType.DIVIDEND, TransactionType.INTEREST)
            );

        return buildIncomeCalculationResult(incomeTransactions);
    }

    private IncomeCalculationResult buildIncomeCalculationResult(List<PortfolioTransaction> transactions) {
        BigDecimal dividends = transactions.stream()
            .filter(txn -> txn.getTransactionType() == TransactionType.DIVIDEND)
            .map(PortfolioTransaction::getAmount)
            .reduce(ZERO, BigDecimal::add);

        BigDecimal interest = transactions.stream()
            .filter(txn -> txn.getTransactionType() == TransactionType.INTEREST)
            .map(PortfolioTransaction::getAmount)
            .reduce(ZERO, BigDecimal::add);

        int dividendPayments = (int) transactions.stream()
            .filter(txn -> txn.getTransactionType() == TransactionType.DIVIDEND)
            .count();

        int interestPayments = (int) transactions.stream()
            .filter(txn -> txn.getTransactionType() == TransactionType.INTEREST)
            .count();

        return new IncomeCalculationResult(
            dividends,
            interest,
            dividends.add(interest),
            dividendPayments,
            interestPayments,
            List.of()
        );
    }

    // ==================== FEES CALCULATIONS ====================

    @Override
    public FeesCalculationResult calculateFees(Long portfolioId, Instant fromDate, Instant toDate) {
        List<PortfolioTransaction> transactions = transactionRepository
            .findByPortfolioIdAndExecutionTimeBetween(portfolioId, fromDate, toDate);

        return buildFeesCalculationResult(transactions);
    }

    private FeesCalculationResult buildFeesCalculationResult(List<PortfolioTransaction> transactions) {
        BigDecimal commissions = transactions.stream()
            .map(txn -> Optional.ofNullable(txn.getCommission()).orElse(ZERO))
            .reduce(ZERO, BigDecimal::add);

        BigDecimal taxes = transactions.stream()
            .map(txn -> Optional.ofNullable(txn.getTax()).orElse(ZERO))
            .reduce(ZERO, BigDecimal::add);

        BigDecimal otherFees = transactions.stream()
            .map(txn -> Optional.ofNullable(txn.getOtherFees()).orElse(ZERO))
            .reduce(ZERO, BigDecimal::add);

        BigDecimal totalFees = commissions.add(taxes).add(otherFees);
        BigDecimal avgFeePerTrade = transactions.size() > 0
            ? totalFees.divide(new BigDecimal(transactions.size()), PRECISION)
            : ZERO;

        return new FeesCalculationResult(
            commissions,
            taxes,
            otherFees,
            totalFees,
            transactions.size(),
            avgFeePerTrade,
            ZERO
        );
    }

    // ==================== P&L VALIDATION ====================

    @Override
    public PnLValidationResult validatePnLCalculation(Long portfolioId) {
        BigDecimal calculatedPnl = calculateTotalPnL(portfolioId);
        BigDecimal expectedPnl = calculateExpectedPnL(portfolioId);
        BigDecimal variance = calculatedPnl.subtract(expectedPnl).abs();
        BigDecimal threshold = expectedPnl.multiply(BigDecimal.valueOf(0.01)); // 1% tolerance

        return new PnLValidationResult(
            variance.compareTo(threshold) <= 0,
            calculatedPnl,
            expectedPnl,
            variance,
            threshold,
            List.of(),
            Instant.now()
        );
    }

    private BigDecimal calculateTotalPnL(Long portfolioId) {
        return positionRepository.findByPortfolioId(portfolioId)
            .stream()
            .map(Position::getTotalPnl)
            .reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal calculateExpectedPnL(Long portfolioId) {
        return portfolioRepository.findById(portfolioId)
            .map(portfolio -> portfolio.getRealizedPnl().add(portfolio.getUnrealizedPnl()))
            .orElse(ZERO);
    }

    // ==================== P&L TREND ANALYSIS ====================

    @Override
    public PnLTrendAnalysis getPnLTrend(Long portfolioId, Integer periodDays) {
        Instant endDate = Instant.now();
        Instant startDate = endDate.minus(periodDays, ChronoUnit.DAYS);

        List<DailyPnLPoint> dailyPoints = generateDailyPnLPoints(portfolioId, startDate, endDate);

        return buildPnLTrendAnalysis(portfolioId, periodDays, dailyPoints);
    }

    private List<DailyPnLPoint> generateDailyPnLPoints(Long portfolioId, Instant startDate, Instant endDate) {
        // Simplified - would need historical data for accurate daily points
        return List.of();
    }

    private PnLTrendAnalysis buildPnLTrendAnalysis(Long portfolioId, Integer periodDays, List<DailyPnLPoint> dailyPoints) {
        return new PnLTrendAnalysis(
            portfolioId,
            periodDays,
            ZERO,
            ZERO,
            ZERO,
            ZERO,
            ZERO,
            0,
            0,
            dailyPoints
        );
    }

    // ==================== RISK METRICS ====================

    @Override
    public BigDecimal calculateSharpeRatio(Long portfolioId, BigDecimal riskFreeRate, Integer periodDays) {
        // Simplified - requires historical returns data
        return ZERO;
    }

    @Override
    public BigDecimal calculateMaxDrawdown(Long portfolioId, Integer periodDays) {
        // Simplified - requires historical value data
        return ZERO;
    }

    // ==================== P&L REPORT ====================

    @Override
    public CompletableFuture<PnLAttributionReport> generatePnLReport(Long portfolioId, Instant fromDate, Instant toDate) {
        return CompletableFuture.supplyAsync(
            () -> buildPnLReport(portfolioId, fromDate, toDate),
            VIRTUAL_EXECUTOR
        );
    }

    private PnLAttributionReport buildPnLReport(Long portfolioId, Instant fromDate, Instant toDate) {
        List<PositionPnLMetrics> positionBreakdown = calculatePositionPnLMetrics(portfolioId);
        PerformanceAttribution attribution = calculatePerformanceAttribution(portfolioId, fromDate, toDate);

        BigDecimal totalPnl = positionBreakdown.stream()
            .map(PositionPnLMetrics::totalPnl)
            .reduce(ZERO, BigDecimal::add);

        BigDecimal realizedPnl = positionBreakdown.stream()
            .map(PositionPnLMetrics::realizedPnl)
            .reduce(ZERO, BigDecimal::add);

        BigDecimal unrealizedPnl = positionBreakdown.stream()
            .map(PositionPnLMetrics::unrealizedPnl)
            .reduce(ZERO, BigDecimal::add);

        IncomeCalculationResult income = calculateIncome(portfolioId, fromDate, toDate);
        FeesCalculationResult fees = calculateFees(portfolioId, fromDate, toDate);

        return new PnLAttributionReport(
            portfolioId,
            fromDate,
            toDate,
            totalPnl,
            realizedPnl,
            unrealizedPnl,
            income.totalIncome(),
            fees.totalFees(),
            positionBreakdown,
            List.of(),
            attribution,
            Instant.now()
        );
    }
}
