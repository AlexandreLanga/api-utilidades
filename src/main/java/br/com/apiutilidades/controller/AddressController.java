package br.com.apiutilidades.controller;

import br.com.apiutilidades.dto.AddressResponse;
import br.com.apiutilidades.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/enderecos")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @Operation(summary = "Consulta endereço por CEP")
    @GetMapping("/{cep}")
    public AddressResponse findByCep(
            @PathVariable @Pattern(regexp = "^(?:[0-9]{8}|[0-9]{5}-[0-9]{3})$", message = "CEP deve conter 8 dígitos, com hífen opcional.") String cep) {
        return addressService.findByCep(cep.replace("-", ""));
    }

    @Operation(summary = "Pesquisa endereços por UF, cidade e logradouro")
    @GetMapping("/busca")
    public List<AddressResponse> findByAddress(
            @RequestParam @Pattern(regexp = "^[A-Za-z]{2}$", message = "UF deve conter duas letras.") String uf,
            @RequestParam @NotBlank @Size(min = 3, message = "Cidade deve conter ao menos 3 caracteres.") String cidade,
            @RequestParam @NotBlank @Size(min = 3, message = "Logradouro deve conter ao menos 3 caracteres.") String logradouro) {
        return addressService.findByAddress(uf, cidade, logradouro);
    }
}