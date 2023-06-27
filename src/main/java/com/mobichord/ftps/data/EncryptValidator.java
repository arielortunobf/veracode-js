package com.mobichord.ftps.data;

import org.apache.commons.lang3.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class EncryptValidator implements
        ConstraintValidator<EncryptValidationAnnotation, UploadRequest> {

    @Override
    public boolean isValid(UploadRequest value, ConstraintValidatorContext context) {
        if (value.isEncrypt()) {
           return StringUtils.isNotBlank(value.getCryptoCfgId());
        }
        return true;
    }
}
