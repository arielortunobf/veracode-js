package com.mobichord.ftps.data;

import org.apache.commons.lang3.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class DecryptValidator implements
        ConstraintValidator<DecryptValidationAnnotation, DownloadRequest> {

    @Override
    public boolean isValid(DownloadRequest value, ConstraintValidatorContext context) {
        if (value.isDecrypt()) {
           return StringUtils.isNotBlank(value.getCryptoCfgId());
        }
        return true;
    }
}
