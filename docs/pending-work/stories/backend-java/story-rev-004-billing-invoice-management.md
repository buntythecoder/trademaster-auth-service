# Story REV-004: Billing & Invoice Management

## Epic
Epic 5: Revenue Systems & Gamification

## Story Overview
**As a** TradeMaster user  
**I want** automated billing with detailed invoices and payment history  
**So that** I can track my payments and have proper financial records

## Business Value
- **Financial Compliance**: GST-compliant invoicing for Indian market
- **Customer Trust**: Transparent billing with detailed breakdowns
- **Operational Efficiency**: Automated invoice generation and delivery
- **Revenue Reconciliation**: Accurate financial reporting and accounting

## Technical Requirements

### Invoice Generation System
```java
@Service
@Transactional
public class InvoiceService {
    
    @Autowired
    private InvoiceRepository invoiceRepository;
    
    @Autowired
    private PaymentTransactionRepository paymentRepository;
    
    @Autowired
    private TaxCalculationService taxCalculationService;
    
    @Autowired
    private PdfGenerationService pdfGenerationService;
    
    public Invoice generateInvoice(String subscriptionId, PaymentTransaction payment) {
        
        Subscription subscription = subscriptionService.getById(subscriptionId);
        User user = userService.getById(subscription.getUserId());
        
        // Calculate tax breakdown
        TaxCalculation taxCalculation = taxCalculationService.calculateTax(
            payment.getAmount(), 
            user.getBillingAddress().getState(),
            subscription.getPlan().getTaxCategory()
        );
        
        // Generate invoice number
        String invoiceNumber = generateInvoiceNumber();
        
        // Create invoice
        Invoice invoice = Invoice.builder()
            .invoiceNumber(invoiceNumber)
            .userId(user.getId())
            .subscriptionId(subscriptionId)
            .paymentTransactionId(payment.getId())
            
            // Amount Details
            .baseAmount(payment.getAmount().subtract(taxCalculation.getTotalTax()))
            .taxAmount(taxCalculation.getTotalTax())
            .totalAmount(payment.getAmount())
            
            // Tax Breakdown
            .cgstAmount(taxCalculation.getCgst())
            .sgstAmount(taxCalculation.getSgst())
            .igstAmount(taxCalculation.getIgst())
            .cgstRate(taxCalculation.getCgstRate())
            .sgstRate(taxCalculation.getSgstRate())
            .igstRate(taxCalculation.getIgstRate())
            
            // Invoice Details
            .invoiceDate(LocalDate.now())
            .dueDate(LocalDate.now()) // Immediate payment
            .description(generateDescription(subscription))
            .currency("INR")
            .status(InvoiceStatus.GENERATED)
            
            // Business Details
            .businessGstin(getBusinessGstin())
            .businessAddress(getBusinessAddress())
            .customerGstin(user.getGstNumber())
            .customerAddress(user.getBillingAddress())
            
            .build();
        
        invoice = invoiceRepository.save(invoice);
        
        // Generate PDF
        byte[] pdfContent = pdfGenerationService.generateInvoicePdf(invoice);
        invoice.setPdfPath(storePdfFile(invoiceNumber, pdfContent));
        
        // Send invoice email
        emailService.sendInvoiceEmail(user.getEmail(), invoice, pdfContent);
        
        return invoiceRepository.save(invoice);
    }
    
    @Scheduled(cron = "0 0 2 * * ?") // Run daily at 2 AM
    public void processOverdueInvoices() {
        List<Invoice> overdueInvoices = invoiceRepository.findOverdueInvoices(LocalDate.now());
        
        for (Invoice invoice : overdueInvoices) {
            try {
                processOverdueInvoice(invoice);
            } catch (Exception e) {
                log.error("Failed to process overdue invoice: " + invoice.getId(), e);
            }
        }
    }
    
    private void processOverdueInvoice(Invoice invoice) {
        // Send overdue notice
        User user = userService.getById(invoice.getUserId());
        emailService.sendOverdueNotice(user.getEmail(), invoice);
        
        // Update invoice status
        invoice.setStatus(InvoiceStatus.OVERDUE);
        invoice.setOverdueDays(calculateOverdueDays(invoice));
        
        // Apply late fee if configured
        if (shouldApplyLateFee(invoice)) {
            BigDecimal lateFee = calculateLateFee(invoice);
            invoice.setLateFeeAmount(lateFee);
            invoice.setTotalAmount(invoice.getTotalAmount().add(lateFee));
        }
        
        invoiceRepository.save(invoice);
        
        // Suspend subscription if overdue > 30 days
        if (invoice.getOverdueDays() > 30) {
            subscriptionService.suspendForNonPayment(invoice.getSubscriptionId());
        }
    }
    
    public List<Invoice> getUserInvoices(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("invoiceDate").descending());
        Page<Invoice> invoicePage = invoiceRepository.findByUserIdAndStatus(
            userId, InvoiceStatus.PAID, pageable
        );
        return invoicePage.getContent();
    }
    
    public byte[] downloadInvoicePdf(String invoiceId, String userId) {
        Invoice invoice = invoiceRepository.findByIdAndUserId(invoiceId, userId)
            .orElseThrow(() -> new InvoiceNotFoundException());
        
        if (invoice.getPdfPath() != null) {
            return fileStorageService.downloadFile(invoice.getPdfPath());
        } else {
            // Regenerate PDF if not available
            byte[] pdfContent = pdfGenerationService.generateInvoicePdf(invoice);
            invoice.setPdfPath(storePdfFile(invoice.getInvoiceNumber(), pdfContent));
            invoiceRepository.save(invoice);
            return pdfContent;
        }
    }
}

@Service
public class TaxCalculationService {
    
    public TaxCalculation calculateTax(BigDecimal amount, String customerState, TaxCategory category) {
        
        // GST rates based on service category
        BigDecimal gstRate = getGstRate(category); // 18% for digital services
        
        String businessState = getBusinessState(); // Company's state
        
        if (customerState.equals(businessState)) {
            // Intra-state: CGST + SGST
            BigDecimal cgstRate = gstRate.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
            BigDecimal sgstRate = cgstRate;
            
            BigDecimal cgstAmount = amount.multiply(cgstRate).divide(
                new BigDecimal("100"), 2, RoundingMode.HALF_UP
            );
            BigDecimal sgstAmount = cgstAmount;
            
            return TaxCalculation.builder()
                .cgst(cgstAmount)
                .sgst(sgstAmount)
                .igst(BigDecimal.ZERO)
                .cgstRate(cgstRate)
                .sgstRate(sgstRate)
                .igstRate(BigDecimal.ZERO)
                .totalTax(cgstAmount.add(sgstAmount))
                .build();
                
        } else {
            // Inter-state: IGST
            BigDecimal igstAmount = amount.multiply(gstRate).divide(
                new BigDecimal("100"), 2, RoundingMode.HALF_UP
            );
            
            return TaxCalculation.builder()
                .cgst(BigDecimal.ZERO)
                .sgst(BigDecimal.ZERO)
                .igst(igstAmount)
                .cgstRate(BigDecimal.ZERO)
                .sgstRate(BigDecimal.ZERO)
                .igstRate(gstRate)
                .totalTax(igstAmount)
                .build();
        }
    }
}
```

### PDF Invoice Generation
```java
@Service
public class PdfGenerationService {
    
    public byte[] generateInvoicePdf(Invoice invoice) {
        try {
            // Load invoice template
            String htmlTemplate = loadInvoiceTemplate();
            
            // Populate template with invoice data
            Context context = new Context();
            context.setVariable("invoice", invoice);
            context.setVariable("company", getCompanyDetails());
            context.setVariable("qrCode", generateQrCode(invoice));
            context.setVariable("taxBreakdown", calculateTaxBreakdown(invoice));
            
            String htmlContent = templateEngine.process(htmlTemplate, context);
            
            // Generate PDF using Flying Saucer
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            renderer.createPDF(outputStream);
            
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            log.error("Failed to generate PDF for invoice: " + invoice.getId(), e);
            throw new PdfGenerationException("Failed to generate invoice PDF", e);
        }
    }
    
    private String generateQrCode(Invoice invoice) {
        // Generate UPI QR code for payment
        String upiPaymentString = String.format(
            "upi://pay?pa=%s&pn=%s&am=%s&cu=INR&tn=Invoice%%20%s",
            getCompanyUpiId(),
            getCompanyName(),
            invoice.getTotalAmount(),
            invoice.getInvoiceNumber()
        );
        
        return qrCodeService.generateQrCode(upiPaymentString);
    }
}
```

### Database Schema
```sql
-- Invoices
CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    
    -- References
    user_id UUID NOT NULL REFERENCES users(id),
    subscription_id UUID REFERENCES subscriptions(id),
    payment_transaction_id UUID REFERENCES payment_transactions(id),
    
    -- Amount Breakdown
    base_amount DECIMAL(10,2) NOT NULL,
    tax_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    late_fee_amount DECIMAL(10,2) DEFAULT 0,
    total_amount DECIMAL(10,2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    
    -- Tax Details (GST)
    cgst_amount DECIMAL(10,2) DEFAULT 0,
    sgst_amount DECIMAL(10,2) DEFAULT 0,
    igst_amount DECIMAL(10,2) DEFAULT 0,
    cgst_rate DECIMAL(5,2) DEFAULT 0,
    sgst_rate DECIMAL(5,2) DEFAULT 0,
    igst_rate DECIMAL(5,2) DEFAULT 0,
    
    -- Invoice Details
    invoice_date DATE NOT NULL,
    due_date DATE NOT NULL,
    description TEXT NOT NULL,
    status invoice_status NOT NULL DEFAULT 'generated',
    
    -- Business Details
    business_gstin VARCHAR(15),
    business_address JSONB,
    customer_gstin VARCHAR(15),
    customer_address JSONB,
    
    -- File Storage
    pdf_path VARCHAR(500),
    
    -- Overdue Management
    overdue_days INTEGER DEFAULT 0,
    overdue_notice_sent_at TIMESTAMP,
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Invoice Line Items
CREATE TABLE invoice_line_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    
    -- Item Details
    description TEXT NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    
    -- Tax Information
    tax_rate DECIMAL(5,2) DEFAULT 0,
    tax_amount DECIMAL(10,2) DEFAULT 0,
    
    -- Metadata
    subscription_period_start DATE,
    subscription_period_end DATE,
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Payment Reconciliation
CREATE TABLE payment_reconciliation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- References
    invoice_id UUID NOT NULL REFERENCES invoices(id),
    payment_transaction_id UUID NOT NULL REFERENCES payment_transactions(id),
    
    -- Reconciliation Details
    reconciled_amount DECIMAL(10,2) NOT NULL,
    reconciliation_date TIMESTAMP NOT NULL DEFAULT NOW(),
    reconciliation_method VARCHAR(50), -- 'automatic', 'manual'
    
    -- Status
    status reconciliation_status DEFAULT 'pending',
    notes TEXT,
    
    -- Processed by
    processed_by UUID REFERENCES users(id),
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Custom Types
CREATE TYPE invoice_status AS ENUM (
    'generated', 'sent', 'paid', 'overdue', 'cancelled', 'refunded'
);

CREATE TYPE reconciliation_status AS ENUM (
    'pending', 'matched', 'discrepancy', 'resolved'
);

-- Indexes
CREATE INDEX idx_invoices_user_date ON invoices(user_id, invoice_date DESC);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_due_date ON invoices(due_date) WHERE status IN ('generated', 'sent');
CREATE INDEX idx_invoice_number ON invoices(invoice_number);
```

### REST API Implementation
```java
@RestController
@RequestMapping("/api/v1/billing")
@Validated
public class BillingController {
    
    @Autowired
    private InvoiceService invoiceService;
    
    @GetMapping("/invoices")
    public ResponseEntity<PagedResponse<InvoiceResponse>> getUserInvoices(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        String userId = jwtService.extractUserId(token);
        
        List<Invoice> invoices = invoiceService.getUserInvoices(userId, page, size);
        long totalElements = invoiceService.countUserInvoices(userId);
        
        List<InvoiceResponse> invoiceResponses = invoices.stream()
            .map(InvoiceResponse::from)
            .collect(Collectors.toList());
        
        PagedResponse<InvoiceResponse> response = PagedResponse.<InvoiceResponse>builder()
            .content(invoiceResponses)
            .page(page)
            .size(size)
            .totalElements(totalElements)
            .totalPages((int) Math.ceil((double) totalElements / size))
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/invoices/{invoiceId}")
    public ResponseEntity<InvoiceDetailResponse> getInvoiceDetails(
            @PathVariable String invoiceId,
            @RequestHeader("Authorization") String token) {
        
        String userId = jwtService.extractUserId(token);
        
        Invoice invoice = invoiceService.getInvoiceDetails(invoiceId, userId);
        
        return ResponseEntity.ok(InvoiceDetailResponse.from(invoice));
    }
    
    @GetMapping("/invoices/{invoiceId}/download")
    public ResponseEntity<byte[]> downloadInvoicePdf(
            @PathVariable String invoiceId,
            @RequestHeader("Authorization") String token) {
        
        String userId = jwtService.extractUserId(token);
        
        byte[] pdfContent = invoiceService.downloadInvoicePdf(invoiceId, userId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice-" + invoiceId + ".pdf");
        headers.setContentLength(pdfContent.length);
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(pdfContent);
    }
    
    @GetMapping("/summary")
    public ResponseEntity<BillingSummaryResponse> getBillingSummary(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "12") int months) {
        
        String userId = jwtService.extractUserId(token);
        
        BillingSummary summary = invoiceService.getBillingSummary(userId, months);
        
        return ResponseEntity.ok(BillingSummaryResponse.from(summary));
    }
    
    @PostMapping("/invoices/{invoiceId}/pay")
    public ResponseEntity<PaymentInitiationResponse> initiatePayment(
            @PathVariable String invoiceId,
            @RequestBody @Valid PaymentInitiationRequest request,
            @RequestHeader("Authorization") String token) {
        
        String userId = jwtService.extractUserId(token);
        
        PaymentResult result = invoiceService.initiateInvoicePayment(
            invoiceId, userId, request
        );
        
        return ResponseEntity.ok(PaymentInitiationResponse.from(result));
    }
}
```

## Acceptance Criteria

### Invoice Generation
- [ ] **Automated Generation**: Invoices generated automatically upon successful payment
- [ ] **GST Compliance**: Proper GST calculation with CGST/SGST for intra-state, IGST for inter-state
- [ ] **Invoice Numbering**: Sequential, unique invoice numbers following format TRADE/2024/001
- [ ] **PDF Generation**: Professional PDF invoices with company branding

### Tax Calculation
- [ ] **GST Rates**: Correct 18% GST for digital services
- [ ] **Tax Breakdown**: Clear breakdown of CGST, SGST, IGST amounts
- [ ] **Address Validation**: State-based tax calculation based on billing addresses
- [ ] **Tax Compliance**: GST registration details included in invoices

### Payment Integration
- [ ] **Payment Linking**: Invoices linked to payment transactions
- [ ] **Reconciliation**: Automatic payment reconciliation with invoice status updates
- [ ] **Overdue Management**: Automatic overdue notifications and late fee calculation
- [ ] **Multiple Payment Methods**: Support for UPI, cards, net banking payments

### User Experience
- [ ] **Invoice History**: Users can view and download historical invoices
- [ ] **Email Delivery**: Invoices automatically emailed to users
- [ ] **Mobile Responsive**: Invoice viewing optimized for mobile devices
- [ ] **Search & Filter**: Invoice search by date, amount, status

## Testing Strategy

### Unit Tests
- Tax calculation accuracy across different states
- Invoice generation logic validation
- PDF generation functionality
- Overdue calculation algorithms

### Integration Tests
- Payment gateway integration for invoice payments
- Email service integration for invoice delivery
- File storage integration for PDF storage
- Database transaction integrity

### Compliance Tests
- GST calculation validation with actual tax scenarios
- Invoice format compliance with Indian regulations
- Data retention compliance for financial records
- Audit trail completeness

### Performance Tests
- Invoice generation performance (bulk processing)
- PDF generation optimization
- Database query performance for invoice history
- File storage and retrieval performance

## Definition of Done
- [ ] Automated invoice generation system implemented
- [ ] GST-compliant tax calculation working for all states
- [ ] Professional PDF invoice generation with branding
- [ ] Email delivery system for invoices operational
- [ ] User dashboard for invoice history completed
- [ ] Payment integration for invoice payments working
- [ ] Overdue management system with notifications active
- [ ] Admin dashboard for billing analytics implemented
- [ ] Performance testing completed (1000+ invoices/hour)
- [ ] Compliance audit passed for GST requirements

## Story Points: 18

## Dependencies
- REV-001: Payment Gateway Integration (for invoice payments)
- REV-002: Subscription Management Service (for subscription details)
- Email service configuration
- File storage system setup
- GST registration and compliance setup

## Notes
- Integration with accounting software (Tally, QuickBooks) for business accounting
- Support for different invoice templates based on business requirements
- Integration with RBI and GST portal APIs for automated compliance
- Consideration for international expansion with different tax systems