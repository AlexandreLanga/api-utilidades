# API Utilidades

API REST em Java 21 e Spring Boot para consulta de endereços via ViaCEP e clima atual via OpenWeatherMap.

## Requisitos

- JDK 21
- Maven 3.9+
- Chave OpenWeatherMap configurada em `OPENWEATHER_API_KEY`

A aplicação falha no startup com uma mensagem explícita quando a chave não está configurada. Não coloque chaves reais em arquivos versionados. Em produção, injete o segredo pelo gerenciador de segredos da plataforma.

## Executar

PowerShell:

```powershell
$env:OPENWEATHER_API_KEY = "sua-chave"
mvn spring-boot:run
```

Linux/macOS:

```sh
export OPENWEATHER_API_KEY="sua-chave"
mvn spring-boot:run
```

Swagger UI: <http://localhost:8080/swagger-ui.html>

## Endpoints

| Método | Endpoint | Observação |
| --- | --- | --- |
| GET | `/api/v1/enderecos/{cep}` | Aceita `01001000` ou `01001-000` |
| GET | `/api/v1/clima/cidade?cidade=Chapecó` | Unidade `metric`, `imperial` ou `standard`; idioma padrão `pt_br` |
| GET | `/api/v1/clima/coordenadas?lat=-27.1&lon=-52.6` | Latitude entre -90 e 90; longitude entre -180 e 180 |
| GET | `/actuator/health` | Health check sem detalhes internos |

Erros seguem RFC 7807 (`application/problem+json`). CEP inexistente retorna 404, parâmetros inválidos 400 e indisponibilidade de provedor 503.

## Segurança e operação

- Chave externa obrigatória via variável de ambiente; não é incluída na saída nem nos logs da aplicação.
- Conexões externas têm timeout de conexão de 3 s e leitura de 5 s, sem seguir redirecionamentos.
- Circuit breaker independente para cada provedor; cache em memória de CEPs por 24 h, limitado a 10.000 itens.
- O Actuator expõe somente `health` e `info`; detalhes de health e stack traces não são enviados aos clientes.
- A documentação OpenAPI fica habilitada por padrão para desenvolvimento e pode ser desativada na implantação com `OPENAPI_ENABLED=false`.
- A imagem Docker executa como usuário sem privilégios. Para produção, configure TLS no proxy/ingress, limites de tráfego e armazenamento de segredos da plataforma.

## Testes e imagem

```sh
mvn test
docker build -t api-utilidades .
docker run --rm -p 8080:8080 -e OPENWEATHER_API_KEY="$OPENWEATHER_API_KEY" api-utilidades
```

## SonarQube no GitHub Actions

O workflow `.github/workflows/quality.yml` executa `mvn clean verify`, exige no mínimo 90% de cobertura de linhas via JaCoCo e envia a análise ao SonarCloud, aguardando o Quality Gate. A chave configurada no `pom.xml` é `AlexandreLanga_api-utilidades`.

Configure no repositório GitHub:

- Secret `SONAR_TOKEN` com um token de análise do projeto.
- `SONAR_HOST_URL` usa `https://sonarcloud.io` por padrão; configure a variável apenas se usar outro servidor.
- Variable `SONAR_ORGANIZATION` com a chave da organização no SonarCloud.

No SonarCloud, atribua ao projeto um Quality Gate com a condição `Coverage is less than 90%` como falha. No GitHub, configure a regra de proteção da branch principal para exigir o check `Build and SonarQube / quality-gate`. Assim, o merge requer tanto a cobertura JaCoCo mínima quanto a aprovação do Quality Gate remoto. O build e os testes rodam em todos os eventos; em pull requests de forks, a análise SonarCloud é ignorada porque o GitHub não disponibiliza secrets para esses eventos, mas o limite de cobertura local continua valendo.
