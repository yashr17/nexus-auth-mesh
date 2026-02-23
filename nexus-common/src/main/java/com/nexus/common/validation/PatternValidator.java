package com.nexus.common.validation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PatternValidator implements ConstraintValidator<ValidPattern, String> {

    @Autowired
    private Environment environment;
    private String name;

    private static final String PROPERTY_PREFIX = "nexus.validation.patterns.";

    @Override
    public void initialize(ValidPattern constraintAnnotation) {
        this.name = constraintAnnotation.name();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }

        String propertyPath = PROPERTY_PREFIX + name;
        String regex = environment.getProperty(propertyPath);

        if (regex == null || regex.isEmpty()) {
            // If the regex is not found in the properties, consider it invalid
            // log.error("Security Risk: No regex found for key {}", propertyPath);
            return false;
            
        }
        return value.matches(regex);
    }
}
