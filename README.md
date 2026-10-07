# API Utilidades

API REST em Java 21 e Spring Boot para consulta de endereços via ViaCEP e clima atual via OpenWeatherMap.

## Requisitos

- JDK 21
- Maven 3.9+
- Chaves `OPENWEATHER_API_KEY` e `JWT_SECRET` configuradas

A aplicação falha no startup com uma mensagem explícita quando `OPENWEATHER_API_KEY` ou `JWT_SECRET` não está configurada. Gere um valor aleatório forte para `JWT_SECRET`. Não coloque chaves reais em arquivos versionados. Em produção, injete os segredos pelo gerenciador de segredos da plataforma.

## Executar

PowerShell:

```powershell
$env:OPENWEATHER_API_KEY = "sua-chave"
$env:JWT_SECRET = "seu-segredo-forte"
mvn spring-boot:run
```

Linux/macOS:

```sh
export OPENWEATHER_API_KEY="sua-chave"
export JWT_SECRET="seu-segredo-forte"
mvn spring-boot:run
```

A página inicial da API fica em <http://localhost:8080/>. Swagger UI: <http://localhost:8080/swagger-ui.html>

## Endpoints

| Método | Endpoint | Observação |
| --- | --- | --- |
| GET | `/api/v1/enderecos/{cep}` | Aceita `01001000` ou `01001-000` |
| GET | `/api/v1/enderecos/busca?uf=RS&cidade=Porto Alegre&logradouro=Domingos` | Pesquisa endereços; cidade e logradouro devem ter ao menos 3 caracteres |
| GET | `/api/v1/clima/cidade?cidade=Chapecó` | Unidade `metric`, `imperial` ou `standard`; idioma padrão `pt_br` |
| GET | `/api/v1/clima/coordenadas?lat=-27.1&lon=-52.6` | Latitude entre -90 e 90; longitude entre -180 e 180 |
| GET | `/actuator/health` | Health check sem detalhes internos |

Erros seguem RFC 7807 (`application/problem+json`). CEP inexistente retorna 404, parâmetros inválidos 400 e indisponibilidade de provedor 503. A busca por endereço segue o [formato oficial do ViaCEP](https://viacep.com.br/), que requer UF, cidade e logradouro; cidade e logradouro precisam ter no mínimo 3 caracteres. A busca pode retornar até 50 correspondências.

Todas as rotas `/api/**` exigem que o valor configurado em `JWT_SECRET` seja enviado no header `X-API-KEY`. Chave ausente ou inválida retorna 401. A página inicial, documentação OpenAPI e health check não exigem esse header.

## Segurança e operação

- Chaves externas e de acesso à API são configuradas por variáveis de ambiente e não são incluídas na saída nem nos logs da aplicação.
- Conexões externas têm timeout de conexão de 3 s e leitura de 5 s, sem seguir redirecionamentos.
- Circuit breaker independente para cada provedor; cache em memória de CEPs por 24 h, limitado a 10.000 itens.
- O Actuator expõe somente `health` e `info`; detalhes de health e stack traces não são enviados aos clientes.
- A documentação OpenAPI fica habilitada por padrão para desenvolvimento e pode ser desativada na implantação com `OPENAPI_ENABLED=false`.
- A imagem Docker executa como usuário sem privilégios. Para produção, configure TLS no proxy/ingress, limites de tráfego e armazenamento de segredos da plataforma.

## Testes e imagem

```sh
mvn test
docker build -t api-utilidades .
docker run --rm -p 8080:8080 -e OPENWEATHER_API_KEY="$OPENWEATHER_API_KEY" -e JWT_SECRET="$JWT_SECRET" api-utilidades
```

## SonarQube no GitHub Actions

Os workflows separam as validações: `.github/workflows/quality.yml` executa build, testes e verificação JaCoCo (mínimo de 90% de cobertura de linhas) somente em pull requests; `.github/workflows/sonarqube.yml` roda análise após push/merge na `main`, gera a cobertura sem reaplicar o limite local e aguarda o Quality Gate remoto. A chave configurada no `pom.xml` é `AlexandreLanga_api-utilidades`.

Configure no Environment `main` do GitHub:

- Secret `SONAR_TOKEN` com um token de análise do projeto.
- Variable `SONAR_HOST_URL` com `https://sonarcloud.io`.
- Variable `SONAR_ORGANIZATION` com a chave da organização no SonarCloud.

No SonarCloud, atribua ao projeto um Quality Gate com a condição `Coverage is less than 90%` como falha. No GitHub, exija o check `Build and SonarQube / quality-gate` nas regras da branch principal para bloquear merges abaixo da cobertura JaCoCo. Como o plano atual analisa apenas a `main`, o Quality Gate remoto é verificado depois do merge e não pode bloqueá-lo retroativamente; o check pré-merge é o gate local de cobertura.
