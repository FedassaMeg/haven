package org.haven.shared.validation;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Generic business rule validator
 */
@Service
public class BusinessRuleValidator {
    
    public ValidationResult validate(Object target, BusinessRule... rules) {
        List<String> violations = new ArrayList<>();
        
        for (BusinessRule rule : rules) {
            if (!rule.test(target)) {
                violations.add(rule.getViolationMessage());
            }
        }
        
        return new ValidationResult(violations.isEmpty(), violations);
    }
    
    public static BusinessRule rule(Predicate<Object> predicate, String violationMessage) {
        return new BusinessRule(predicate, violationMessage);
    }
    
    public static BusinessRule rule(Predicate<Object> predicate, Supplier<String> messageSupplier) {
        return new BusinessRule(predicate, messageSupplier.get());
    }
    
    public static class BusinessRule {
        private final Predicate<Object> predicate;
        private final String violationMessage;
        
        public BusinessRule(Predicate<Object> predicate, String violationMessage) {
            this.predicate = predicate;
            this.violationMessage = violationMessage;
        }
        
        public boolean test(Object target) {
            return predicate.test(target);
        }
        
        public String getViolationMessage() {
            return violationMessage;
        }
    }
    
    public record ValidationResult(
        boolean isValid,
        List<String> violations
    ) {
        public void throwIfInvalid() {
            if (!isValid) {
                throw new BusinessRuleViolationException(violations);
            }
        }
    }
    
    public static class BusinessRuleViolationException extends RuntimeException {
        private final List<String> violations;
        
        public BusinessRuleViolationException(List<String> violations) {
            super("Business rule violations: " + String.join(", ", violations));
            this.violations = violations;
        }
        
        public List<String> getViolations() {
            return violations;
        }
    }
}