package br.com.apiutilidades.controller;

import br.com.apiutilidades.dto.WeatherResponse;
import br.com.apiutilidades.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/clima")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @Operation(summary = "Consulta clima atual por cidade")
    @GetMapping("/cidade")
    public WeatherResponse findByCity(
            @RequestParam @NotBlank @Size(max = 100)
            @Pattern(regexp = "^[\\p{L}\\p{M} .,'-]+(?:,[A-Za-z]{2})?$", message = "Nome de cidade inválido.") String cidade,
            @RequestParam(defaultValue = "metric") @Pattern(regexp = "^(metric|imperial|standard)$") String unidade,
            @RequestParam(defaultValue = "pt_br") @Pattern(regexp = "^[a-z]{2}(?:_[A-Za-z]{2})?$") String idioma) {
        return weatherService.findByCity(cidade, unidade, idioma);
    }

    @Operation(summary = "Consulta clima atual por coordenadas")
    @GetMapping("/coordenadas")
    public WeatherResponse findByCoordinates(
            @RequestParam @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") BigDecimal lat,
            @RequestParam @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") BigDecimal lon,
            @RequestParam(defaultValue = "metric") @Pattern(regexp = "^(metric|imperial|standard)$") String unidade,
            @RequestParam(defaultValue = "pt_br") @Pattern(regexp = "^[a-z]{2}(?:_[A-Za-z]{2})?$") String idioma) {
        return weatherService.findByCoordinates(lat, lon, unidade, idioma);
    }
}