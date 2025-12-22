# Thesis Microservices - Fault Tolerance Analysis

Pyton recreation of Google's Online Boutique microservice demo for analyzing circuit breaker patterns.

## Architecture

```
                    ┌─────────────────┐
                    │  loadgenerator  │
                    │    (Locust)     │
                    └────────┬────────┘
                             │ HTTP
                             ▼
                    ┌─────────────────┐
                    │    frontend     │◄── Circuit breakers here
                    │   (port 8080)   │
                    └────────┬────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│ productcatalog│   │    checkout   │   │      ad       │
│  (port 8081)  │   │  (port 8087)  │   │  (port 8089)  │
└───────────────┘   └───────┬───────┘   └───────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │         │         │         │         │
        ▼         ▼         ▼         ▼         ▼
┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐
│  cart   │ │ payment │ │shipping │ │  email  │ │currency │
│ (8082)  │ │ (8085)  │ │ (8084)  │ │ (8086)  │ │ (8083)  │
└────┬────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘
     │
     ▼
┌─────────┐
│  Redis  │
│ (6379)  │
└─────────┘
```

## Services
| Service        | Port   | Language         | Description               |
|----------------|--------|------------------|---------------------------|
| frontend       | 8080   | Python / FastAPI | Http endpoints for Locust |
| productcatalog | 8081   | Python / FastAPI | Product listing & details |
