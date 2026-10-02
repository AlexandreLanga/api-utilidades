package br.com.apiutilidades.dto;

public record WeatherResponse(
        String cidade, String pais, double temperaturaAtual, double sensacaoTermica,
        double temperaturaMinima, double temperaturaMaxima, int umidadePercentual,
        double velocidadeVento, String condicaoClimatica, String descricao) {
}