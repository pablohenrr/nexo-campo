package com.nexocampo.modules.ingestion;

import java.time.LocalDate;
import java.util.List;

/**
 * Entrada da IA.
 *
 * @param text       texto da mensagem somado à transcrição do áudio; pode ser vazio quando só veio foto
 * @param image      foto enviada, ou {@code null}
 * @param plotNames  talhões do produtor, para a IA escolher um deles
 * @param today      data de hoje no fuso da fazenda, para resolver "ontem", "segunda"...
 */
public record InterpretationRequest(String text, IncomingMedia image, List<String> plotNames, LocalDate today) {}
