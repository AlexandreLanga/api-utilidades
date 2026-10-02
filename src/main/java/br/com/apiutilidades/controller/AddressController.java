package br.com.apiutilidades.controller;

import br.com.apiutilidades.dto.AddressResponse;
import br.com.apiutilidades.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
}