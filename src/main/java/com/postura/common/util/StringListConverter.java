package com.postura.common.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * List<String>과 DB의 String/TEXT 간의 변환을 담당하는 JPA Converter
 * 저장 시 : List -> 하나의 문자열로 결합
 * 조회 시 : 문자열 -> List로 분리
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    // 리스트를 DB에 저장할 때 사용할 구분자
    private static final String SPLIT_CHAR = ",";

    @Override
    public String convertToDatabaseColumn (List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "";
        }
        // List의 요소들을 콤마로 결합한 단일 문자열로 변환
        return String.join(SPLIT_CHAR, attribute);
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if(dbData == null || dbData.trim().isEmpty()) {
            return Collections.emptyList();
        }
        // DB 문자열을 콤마를 기준으로 분리하여 List<String>으로 변환
        return Arrays.stream(dbData.split(SPLIT_CHAR))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
