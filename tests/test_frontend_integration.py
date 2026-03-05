import os
import sys
import pytest
from unittest.mock import AsyncMock, patch, MagicMock

os.environ["CIRCUIT_BREAKER_ENABLED"] = "true"
os.environ["CB_FAIL_MAX"] = "3"
os.environ["CB_RESET_TIMEOUT"] = "60"

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'frontend'))

from fastapi.testclient import TestClient
import main as frontend

@pytest.fixture(autouse=True)
def reset_breakers(monkeypatch):
    import main as m
    monkeypatch.setattr(m, "CIRCUIT_BREAKER_ENABLED", True)
    for name in list(m.breakers.keys()):
        m.breakers[name] = m.CircuitBreaker(name, m.CB_FAIL_MAX, m.CB_RESET_TIMEOUT)
    yield





@pytest.fixture()
def client():
    return TestClient(frontend.app, raise_server_exceptions=False)



class TestHealth:
    def test_health_returns_200(self, client):
        r = client.get("/health")
        assert r.status_code == 200

    def test_health_shows_cb_enabled(self, client):
        data = client.get("/health").json()
        assert data["circuit_breaker_enabled"] is True


    def test_health_has_status_okay(self, client):
        data = client.get("/health").json()
        assert data["status"] == "ok"







class TestMetrics:
    def test_metrics_returns_200(self, client):
        assert client.get("/metrics").status_code == 200


    def test_metrics_has_all_breakers(self, client):
        data = client.get("/metrics").json()
        expected = {"productcatalog", "cart", "currency", "shipping", "checkout", "recommendation", "ad"}
        assert set(data["breakers"].keys()) == expected


    def test_metrics_breakers_start_closed(self, client):
        data = client.get("/metrics").json()
        for name, breaker in data["breakers"].items():
            assert breaker["state"] == "closed", f"{name} should start closed"



    def test_metrics_reflect_state_after_trips(self, client):
        import main as m
        for _ in range(m.CB_FAIL_MAX):
            m.breakers["productcatalog"].record_failure()

        data = client.get("/metrics").json()
        assert data["breakers"]["productcatalog"]["state"] == "open"






class TestCircuitBreakerFalllbacks:
    def _open_breaker(self, name):
        pass

    def test_open_productcatalog_returns_empty_list(self):
        ...

    def test_open_cart_returns_empty_cart(self):
        ...

    def test_open_recommendation_returns_empty_list(self):
        ...

    def test_open_ad_retuns_empty_lsit(self):
        ...

    def test_open_shipping_returns_zero_cost(self):
        ...






class TestIndexPage:
    ...





class TestProductPage:
    ...


class TestCart:
    ...



class TestCheckout:
    FORM = {
        "email": "test@example.com",
        "street_address": "via Carducci 1234",
        "city": "Bolzano",
        "state": "BZ",
        "zip_code": "39100",
        "country": "IT",
        "credit_card_number": "4111111111111111",
        "credit_card_expiration_month": "12",
        "credit_card_expiration_year": "2026",
        "credit_card_cvv": "123",
    }
