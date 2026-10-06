package com.xpv.backend.dto;

/** Horas jogadas somadas numa plataforma vinculada - usado por TEMPO_POR_PLATAFORMA. */
public record TempoPlataformaResponse(String plataforma, double horasTotais) {
}
