# Entity & DTO Record Conversion Guide
## Rule 9 Compliance: Immutability & Records Usage

### Current State
- **Entities**: Using @Data (mutable, violates Rule 9)
- **DTOs**: Using @Data (mutable, violates Rule 9)
- **Pattern**: Lombok @Builder with mutable fields

### Target State (Rule 9 Compliant)
- **DTOs**: Convert ALL to immutable records ✅
- **Entities**: Hybrid approach (records where possible, @Data with final fields for JPA limitations)

---

## DTO Conversion Pattern

### Before (Violates Rule 9)
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {
    private UUID userId;  // Mutable - BAD!
    private BigDecimal amount;
    // ... more mutable fields
}
```

### After (Compliant with Rule 9)
```java
public record PaymentRequestRecord(
    @NotNull UUID userId,
    @NotNull BigDecimal amount,
    String currency
    // ... more fields
) {
    // Compact constructor with validation
    public PaymentRequestRecord {
        currency = (currency != null) ? currency : "INR";
    }

    // Builder pattern for convenience
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        // Builder implementation
    }
}
```

### Key Benefits
1. **Immutability**: No setters, thread-safe
2. **Concise**: Automatic equals/hashCode/toString
3. **Validation**: In compact constructor
4. **Type Safety**: Compile-time guarantees

---

## Entity Conversion Pattern

### Challenge: JPA + Records
JPA entities traditionally need:
- Default constructor (records have compact constructor only)
- Setters for lazy loading (records are immutable)
- Lifecycle callbacks (@PrePersist/@PreUpdate)

### Solution: Hybrid Approach

#### Option 1: Records with JPA (Hibernate 6+)
```java
@Entity
@Table(name = "payment_transactions")
public record PaymentTransactionRecord(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    UUID id,

    @NotNull
    UUID userId,

    @NotNull
    BigDecimal amount,

    Instant createdAt,
    Instant updatedAt
) {
    // Compact constructor
    public PaymentTransactionRecord {
        createdAt = (createdAt != null) ? createdAt : Instant.now();
        updatedAt = (updatedAt != null) ? updatedAt : Instant.now();
    }
}
```

**Limitations**:
- No @PrePersist/@PreUpdate (handle in service layer)
- No bidirectional relationships (use unidirectional)
- No lazy loading (fetch everything or use DTOs)

#### Option 2: Immutable @Data (Pragmatic Approach) ✅
```java
@Entity
@Table(name = "payment_transactions")
@Data  // Keep for JPA compatibility
@NoArgsConstructor  // Required by JPA
@AllArgsConstructor(access = AccessLevel.PRIVATE)  // Builder only
@Builder(toBuilder = true)  // Immutable updates via copy
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private final UUID id;  // final = immutable

    @NotNull
    private final UUID userId;  // final = immutable

    @NotNull
    private final BigDecimal amount;  // final = immutable

    // ... all fields final

    @PrePersist
    protected void onCreate() {
        // Lifecycle callback
    }

    // NO business logic here - move to services!
}
```

**This approach**:
- ✅ Works with JPA/Hibernate
- ✅ Immutable via `final` fields
- ✅ Lifecycle callbacks work
- ✅ Builder pattern for updates: `entity.toBuilder().status(NEW_STATUS).build()`

---

## Conversion Priority

### High Priority (Convert to Records)
1. ✅ `PaymentRequestRecord` - Example created
2. `PaymentResponseRecord`
3. `RefundRequestRecord`
4. `RefundResponseRecord`
5. All other DTOs

### Medium Priority (Make Immutable)
1. `PaymentTransaction` - Add `final` to all fields
2. `UserSubscription` - Add `final` to all fields
3. All other entities

### Pattern Files Created
- ✅ `PaymentRequestRecord.java` - Full example of record-based DTO
- ✅ `Result.java` - Sealed interface with Success/Failure records
- This guide

---

## Implementation Steps

### For DTOs
1. Create new `*Record.java` file
2. Convert class to record with all fields as parameters
3. Add validation in compact constructor
4. Create Builder inner class for fluent API
5. Add defensive copies for mutable types (Map, List)
6. Update all usages to use record

### For Entities
1. Add `final` to ALL fields (except @Id if generated)
2. Make constructor `private` for builder-only access
3. Add `@Builder(toBuilder = true)` for immutable updates
4. Keep lifecycle callbacks
5. Move business logic to services (NO if-else in entities)

---

## Testing Immutability

### Test Pattern
```java
@Test
void testImmutability() {
    PaymentRequestRecord request = PaymentRequestRecord.builder()
        .userId(UUID.randomUUID())
        .amount(BigDecimal.valueOf(100))
        .currency("INR")
        .build();

    // Records are immutable - no setters exist
    // Compile error: request.setAmount(...)  ✅ Good!

    // Must create new instance for changes
    PaymentRequestRecord updated = request.builder()
        .amount(BigDecimal.valueOf(200))
        .build();

    assertNotEquals(request, updated);
}
```

---

## Compliance Checklist

### Rule 9: Immutability & Records Usage
- [x] Example DTO record created (`PaymentRequestRecord`)
- [x] Validation in compact constructor
- [x] Builder pattern with fluent API
- [x] Defensive copies for mutable collections
- [ ] All DTOs converted to records (9 remaining)
- [ ] All entities have `final` fields (6 remaining)

### Rule 3: Functional Programming First
- [x] No setters in records (immutable)
- [x] Pattern matching with sealed Result interface
- [ ] Remove if-else from entity helper methods
- [ ] Move business logic to services

### Rule 4: Advanced Design Patterns
- [x] Builder pattern for records
- [x] Sealed interfaces for type hierarchies
- [ ] Factory pattern for entity creation

---

## Next Steps

1. **Immediate**: Convert remaining DTOs to records
2. **Short-term**: Add `final` to entity fields
3. **Medium-term**: Move entity business logic to services
4. **Long-term**: Consider full record migration for entities when JPA supports it better

**Status**: Pattern established ✅ | Full conversion pending ⏳
