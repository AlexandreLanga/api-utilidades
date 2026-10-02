package br.com.apiutilidades.dto;

public record AddressResponse(
        String cep, String logradouro, String complemento, String bairro,
        String cidade, String uf, String ibge, String ddd) {
}