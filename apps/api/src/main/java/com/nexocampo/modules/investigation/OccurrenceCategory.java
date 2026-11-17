package com.nexocampo.modules.investigation;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum OccurrenceCategory {
    PRAGA,
    DOENCA,
    PLANTA_DANINHA,
    DEFICIENCIA_NUTRICIONAL,
    CLIMA,
    @JsonEnumDefaultValue
    OUTRO
}
